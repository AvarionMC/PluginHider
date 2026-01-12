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

import static org.avarion.pluginhider.util.CraftBukkitVersionUtil.isInstance;
import static org.avarion.pluginhider.util.Util.cleanupCommand;


public class Caches {
    private Caches() {
    }

    private final static Map<String, Boolean> shouldShowCmd = new HashMap<>();
    private final static Map<String, Boolean> shouldShowPlugin = new HashMap<>();
    private final static Map<String, String> cacheCommand2Plugin = new HashMap<>();
    private final static Map<String, Set<String>> cachePlugin2Commands = new HashMap<>();

    private static boolean isLoaded = false;
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
        return shouldShowPlugin.getOrDefault(Util.cleanupWord(pluginName), false);
    }

    @Contract(pure = true)
    public static boolean shouldShowCommand(@Nullable final String command) {
        return shouldShowCmd.getOrDefault(command, false);
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

        var x = ReflectionUtils.getFields(topic.getClass());
        var name = topic.getName();
        PluginHider.logger.error("Unknown topic type (2): " + topic.getClass().getName());
    }

    public static void load() {
        if (isLoaded) {
            return;
        }
        isLoaded = true;

        HelpMap helpMap = Bukkit.getHelpMap();
        Map<String, Set<String>> aliases = new HashMap<>();
        Map<String, Command> cmd2Command = new HashMap<>();

        for (HelpTopic topic : helpMap.getHelpTopics()) {
            registerTopic(topic, aliases, cmd2Command);
        }

        addAliases(aliases, cmd2Command);
        convertMapToCache(cmd2Command);

        update(); // First time update
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

    private static <K, V> void repopulate(@NotNull Map<K, V> target, Map<K, V> source) {
        synchronized (target) { // Fine here, as it's only used in "update"
            target.clear();
            target.putAll(source);
        }
    }

    public static void update() {
        Map<String, Boolean> newShouldShowPlugin = new HashMap<>();
        Map<String, Boolean> newShouldShowCmd = new HashMap<>();

        final boolean colonsAllowed = PluginHider.settings.shouldAllowColonTabcompletion;
        for (var entry : cachePlugin2Commands.entrySet()) {
            var plugin = entry.getKey();
            var show = shouldShowPlugin__Update(plugin);

            newShouldShowPlugin.putIfAbsent(plugin, show);
            for (var cmd : cachePlugin2Commands.get(plugin)) {
                newShouldShowCmd.putIfAbsent(cmd, show);
                newShouldShowCmd.putIfAbsent(cleanupCommand(cmd), show);
                newShouldShowCmd.putIfAbsent(plugin + ":" + cmd, show && colonsAllowed);
                newShouldShowCmd.putIfAbsent(plugin + ":" + cleanupCommand(cmd), show && colonsAllowed);
            }
        }

        repopulate(shouldShowPlugin, newShouldShowPlugin);
        repopulate(shouldShowCmd, newShouldShowCmd);
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
                }

                List<String> newPlugins = new ArrayList<>();
                for (var plugin : Bukkit.getPluginManager().getPlugins()) {
                    String name = Util.cleanupWord(plugin.getName());
                    if (!shouldShowPlugin.containsKey(name)) {
                        newPlugins.add(name);
                    }
                }

                if (!newPlugins.isEmpty()) {
                    synchronized (shouldShowPlugin) {
                        for (var pluginName : newPlugins) {
                            shouldShowPlugin.computeIfAbsent(pluginName, Caches::shouldShowPlugin__Update);
                        }
                    }
                }
            }
        }.runTaskTimer(PluginHider.inst, 10, 10);
    }
}
