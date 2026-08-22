package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.avarion.pluginhider.util.CraftBukkitVersionUtil.isInstance;
import static org.avarion.pluginhider.util.Util.cleanupCommand;


public class Caches {
    private Caches() {
    }

    // Published to readers (packet/command threads) as immutable snapshots swapped in via a single
    // volatile write, so a reader never observes a partially-rebuilt map.
    private static volatile Map<String, Boolean> shouldShowCmd = Map.of();
    private static volatile Map<String, Boolean> shouldShowPlugin = Map.of();
    // Build-time state, only ever touched under this class's monitor (load/reload/update).
    private final static Map<String, String> cacheCommand2Plugin = new HashMap<>();
    private final static Map<String, Set<String>> cachePlugin2Commands = new HashMap<>();

    // Immutable snapshot of command name -> owning plugin (lowercased), for per-player grant checks.
    private static volatile Map<String, String> commandOwner = Map.of();

    // Plugins that expose no commands (so they never appear in the help map); discovered by the
    // updatePlugins() sweep and folded into the show/hide snapshot on the next update().
    private static final Set<String> extraPlugins = ConcurrentHashMap.newKeySet();

    private static volatile boolean isLoaded = false;
    private final static int TIMEOUT_FOR_CHECKING_PLUGINS = 1000 * 60 * 5;

    private static final Map<String, String> defaultPackageNames = Map.of(
            "io.papermc",
            "paper",
            "org.bukkit",
            "bukkit",
            "co.aikar",
            "bukkit",
            "org.spigotmc",
            "spigot",
            "org.avarion.pluginhider.custom_commands",
            "bukkit"
    );

    @Contract(pure = true)
    public static boolean shouldShowPlugin(@Nullable final String pluginName) {
        final String cleaned = Util.cleanupWord(pluginName);
        final Boolean known = shouldShowPlugin.get(cleaned);
        // A plugin missing from the snapshot (no commands, failed to load, or registered after the
        // startup sweep) must still follow the hide/show rules instead of defaulting to hidden —
        // otherwise a plugin that isn't hidden would vanish from a non-op's /plugins, which is itself
        // a tell that something is filtering.
        return known != null ? known : shouldShowPlugin__Update(cleaned);
    }

    @Contract(pure = true)
    public static boolean shouldShowCommand(@Nullable final String command) {
        return shouldShowCmd.getOrDefault(command, false);
    }

    /** As {@link #shouldShowPlugin(String)}, plus any plugin this specific player was granted. */
    public static boolean shouldShowPlugin(@Nullable final UUID player, @Nullable final String pluginName) {
        if (shouldShowPlugin(pluginName)) {
            return true;
        }
        final Set<String> granted = PluginHider.settings.grantsFor(player);
        return granted.contains("*") || granted.contains(Util.cleanupWord(pluginName));
    }

    /** As {@link #shouldShowCommand(String)}, plus commands owned by a plugin this player was granted. */
    public static boolean shouldShowCommand(@Nullable final UUID player, @Nullable final String command) {
        if (shouldShowCommand(command)) {
            return true;
        }
        final Set<String> granted = PluginHider.settings.grantsFor(player);
        if (granted.contains("*")) {
            return true;
        }
        if (granted.isEmpty() || command == null) {
            return false;
        }
        String owner = commandOwner.get(command);
        if (owner == null) {
            owner = commandOwner.get(cleanupCommand(command));
        }
        return owner != null && granted.contains(owner);
    }

    private static void registerCommand(Command command, @NotNull Map<String, Command> cmd2Command) {
        cmd2Command.putIfAbsent(command.getName(), command);
        for (String alias : command.getAliases()) {
            cmd2Command.putIfAbsent(alias, command);
        }

        try {
            @SuppressWarnings("unchecked") List<String> aliases = (List<String>) ReflectionUtils.getFieldValue(
                    command, "alias",
                    List.class
            );
            for (String alias : aliases) {
                cmd2Command.putIfAbsent(alias, command);
            }
        }
        catch (RuntimeException ignored) {
            // Sometimes this isn't in there
        }
    }

    private static void registerTopic(
            HelpTopic topic,
            Map<String, Set<String>> aliases,
            Map<String, Command> cmd2Command
    ) {
        if (topic instanceof IndexHelpTopic) {
            @SuppressWarnings("unchecked") Collection<HelpTopic> allTopics = ReflectionUtils.getFieldValue(
                    topic,
                    "allTopics",
                    Collection.class
            );
            for (var subTopic : allTopics) {
                registerTopic(subTopic, aliases, cmd2Command);
            }
            return;
        }

        if (topic instanceof GenericCommandHelpTopic) {
            Command cmd = ReflectionUtils.getFieldValue(topic, "command", Command.class);
            registerCommand(cmd, cmd2Command);
            return;
        }

        if (isInstance(topic, "help.CommandAliasHelpTopic")) {
            String aliasTarget = ReflectionUtils.getFieldValue(topic, "aliasFor", String.class);
            String name = topic.getName();
            aliases.computeIfAbsent(aliasTarget, k -> new HashSet<>()).add(name);
            aliases.get(aliasTarget).add(aliasTarget);
            return;
        }

        if (isInstance(topic, "help.CustomHelpTopic")) {
            cmd2Command.putIfAbsent(topic.getName(), null);
            return;
        }

        //else if (isInstance(topic, "help.CustomIndexHelpTopic"))
        //else if (isInstance(topic, "help.MultipleCommandAliasHelpTopic"))
        try {
            Command cmd = ReflectionUtils.getFieldValue(topic, "cmd", Command.class);
            registerCommand(cmd, cmd2Command);
            return;
        }
        catch (RuntimeException ignored) {
        }

        PluginHider.logger.error("Unknown topic type (2): " + topic.getClass().getName());
    }

    public static synchronized void load() {
        if (isLoaded) {
            return;
        }

        HelpMap helpMap = Bukkit.getHelpMap();
        Map<String, Set<String>> aliases = new HashMap<>();
        Map<String, Command> cmd2Command = new HashMap<>();

        for (HelpTopic topic : helpMap.getHelpTopics()) {
            registerTopic(topic, aliases, cmd2Command);
        }

        addAliases(aliases, cmd2Command);
        convertMapToCache(cmd2Command);
        commandOwner = Map.copyOf(cacheCommand2Plugin);

        update(); // First time update

        // Latch as loaded only after a successful build, so a transient failure retries on the next
        // call instead of permanently disabling hiding.
        isLoaded = true;
    }

    private static void addElement(final String pluginName, final String cmd) {
        var loweredName = pluginName.toLowerCase(Locale.ENGLISH);
        cacheCommand2Plugin.putIfAbsent(cmd, loweredName);
        cachePlugin2Commands.computeIfAbsent(loweredName, p -> new HashSet<>()).add(cmd);
    }

    private static void processQueue(final @NotNull List<String> queue, final List<String> leftOvers) {
        while (!queue.isEmpty()) {
            String cmd = queue.remove(0);
            if (cacheCommand2Plugin.containsKey(cmd)) {
                continue; // Already processed
            }

            int idx = cmd.indexOf(':');
            if (idx != -1) {
                String pluginName = cmd.substring(0, idx);
                cmd = cmd.substring(idx + 1);
                addElement(pluginName, cmd);
                continue; // Good!
            }

            leftOvers.add(cmd); // Put at the end
        }
    }

    private static void convertMapToCache(@NotNull Map<String, Command> cmd2Command) {
        final List<String> queue = new ArrayList<>(cmd2Command.keySet());
        final List<String> leftOvers = new ArrayList<>();

        for (int i = 0; i < 3 && !queue.isEmpty(); i++) {
            processQueue(queue, leftOvers);
            queue.addAll(leftOvers);
            leftOvers.clear();
        }

        while (!queue.isEmpty()) {
            String cmd = queue.remove(0);
            Command cmd2 = cmd2Command.get(cmd);
            if (cmd2 == null) {
                // e.g. a CustomHelpTopic registered under a name with no backing Command. Treat it
                // as a core command (shown by default) instead of NPE'ing out of the whole build.
                addElement("bukkit", cmd);
                continue;
            }
            String pkg = cmd2.getClass().getPackage().getName();

            if (cmd2 instanceof PluginCommand) {
                PluginCommand p = (PluginCommand) cmd2;
                addElement(p.getPlugin().getName(), cmd);
                continue;
            }

            boolean found = false;
            for (var p2p : defaultPackageNames.entrySet()) {
                String packageNameTest = p2p.getKey();
                if (pkg.startsWith(packageNameTest + ".") || pkg.equals(packageNameTest)) {
                    addElement(p2p.getValue(), cmd);
                    found = true;
                    break;
                }
            }

            if (!found) {
                try {
                    var pluginName = JavaPlugin.getProvidingPlugin(cmd2.getClass()).getName();
                    addElement(pluginName, cmd);
                    continue;
                }
                catch (Exception ignored) {
                }

                PluginHider.logger.warning("Unknown command: " + cmd + " -> " + pkg);
            }
        }
    }

    private static void addAliases(@NotNull Map<String, Set<String>> aliases, Map<String, Command> cmd2Command) {
        for (Set<String> aliasSet : aliases.values()) {
            var forThis = cmd2Command.keySet().stream().filter(aliasSet::contains).findFirst();
            if (forThis.isEmpty()) {
                forThis = cmd2Command.keySet()
                                     .stream()
                                     .filter(k -> aliasSet.stream()
                                                          .anyMatch(a -> a.startsWith("/") && a.substring(1).equals(k)))
                                     .findFirst();
            }

            if (forThis.isEmpty()) {
                PluginHider.logger.warning("Cannot link these aliases back to its command: " + aliasSet);
                continue;
            }

            for (String alias : aliasSet) {
                cmd2Command.putIfAbsent(cleanupCommand(alias), cmd2Command.get(forThis.get()));
            }
        }

        aliases.clear();
    }

    private static boolean shouldShowPlugin__Update(@NotNull final String cleaned) {
        if (PluginHider.settings.showPlugins.contains(cleaned)) {
            return true; // explicitly shown -- remember that `servers` are automagically added in Settings::load!
        }
        if (PluginHider.settings.hidePlugins.contains(cleaned)) {
            return false; // explicitly hidden
        }
        if (Constants.servers.contains(cleaned)) {
            return true;
        }

        return !PluginHider.settings.hideAll; // if all plugins are hidden;
    }

    public static synchronized void update() {
        Map<String, Boolean> newShouldShowPlugin = new HashMap<>();
        Map<String, Boolean> newShouldShowCmd = new HashMap<>();

        final boolean colonsAllowed = PluginHider.settings.shouldAllowColonTabcompletion;
        for (var entry : cachePlugin2Commands.entrySet()) {
            var plugin = entry.getKey();
            var show = shouldShowPlugin__Update(plugin);

            newShouldShowPlugin.putIfAbsent(plugin, show);
            for (var cmd : entry.getValue()) {
                newShouldShowCmd.putIfAbsent(cmd, show);
                newShouldShowCmd.putIfAbsent(cleanupCommand(cmd), show);
                newShouldShowCmd.putIfAbsent(plugin + ":" + cmd, show && colonsAllowed);
                newShouldShowCmd.putIfAbsent(plugin + ":" + cleanupCommand(cmd), show && colonsAllowed);
            }
        }

        for (String plugin : extraPlugins) {
            newShouldShowPlugin.putIfAbsent(plugin, shouldShowPlugin__Update(plugin));
        }

        // Publish immutable snapshots via a single volatile write each, so readers on the packet
        // thread never observe a half-cleared map (the old clear()+putAll() had that window).
        shouldShowPlugin = Map.copyOf(newShouldShowPlugin);
        shouldShowCmd = Map.copyOf(newShouldShowCmd);
    }

    public static synchronized void reload() {
        // Invalidate the built command/plugin mapping so the next load() rebuilds it from scratch,
        // picking up new plugins, aliases and settings. Every reader calls load() before reading a
        // snapshot, so it will always see a freshly-built one.
        isLoaded = false;
        cacheCommand2Plugin.clear();
        cachePlugin2Commands.clear();
    }

    public static void dump() {
        PluginHider.logger.info("----------------------------------------");
        PluginHider.logger.info("Dumping plugin hider `showCachePlugins`:");
        PluginHider.logger.info("*** size: " + shouldShowPlugin.size());
        for (var entry : Caches.shouldShowPlugin.entrySet()) {
            PluginHider.logger.info("key: " + entry.getKey() + ", value: " + entry.getValue());
        }
        PluginHider.logger.info("----------------------------------------");
        PluginHider.logger.info("Dumping plugin hider `showCache`:");
        PluginHider.logger.info("*** size: " + Caches.shouldShowCmd.size());
        for (var entry : Caches.shouldShowCmd.entrySet()) {
            PluginHider.logger.info("key: " + entry.getKey() + ", value: " + entry.getValue());
        }
        PluginHider.logger.info("----------------------------------------");
    }

    public static void updatePlugins() {
        // For the first 5 minutes on your server (yeah, there are slow servers!), update the plugins
        //   to ensure we got the ones not mentioned in the helpmap (ie: the ones without commands)
        final long stopAt = System.currentTimeMillis() + TIMEOUT_FOR_CHECKING_PLUGINS;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() > stopAt) {
                    cancel();
                    return;
                }

                // Build the cache proactively on the main thread once the help map is ready
                // (cheap no-op once loaded), instead of waiting for the first player to join.
                load();

                boolean added = false;
                for (var plugin : Bukkit.getPluginManager().getPlugins()) {
                    String name = Util.cleanupWord(plugin.getName());
                    if (!shouldShowPlugin.containsKey(name) && extraPlugins.add(name)) {
                        added = true;
                    }
                }

                if (added) {
                    update();
                }
            }
        }.runTaskTimer(PluginHider.inst, 10, 10);
    }
}
