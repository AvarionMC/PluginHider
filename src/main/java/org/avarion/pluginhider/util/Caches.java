package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.defaults.BukkitCommand;
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
        return shouldShowPlugin.getOrDefault(Util.cleanupCommand(pluginName), false);
    }

    @Contract(pure = true)
    public static boolean shouldShowCommand(@Nullable final String command) {
        return shouldShowCmd.getOrDefault(Util.cleanupCommand(command), false);
    }

    private static void registerCommand(Command command, @NotNull Map<String, Command> cmd2Command) {
        cmd2Command.putIfAbsent(Util.cleanupCommand(command.getName()), command);

        for (String fieldName : Arrays.asList("aliases", "activeAliases")) {
            @SuppressWarnings("unchecked") List<String> aliases = (List<String>) ReflectionUtils.getFieldValue(
                    command,
                    fieldName,
                    List.class
            );
            for (String alias : aliases) {
                cmd2Command.putIfAbsent(Util.cleanupCommand(alias), command);
            }
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
        }
        else if (topic instanceof GenericCommandHelpTopic) {
            Command cmd = ReflectionUtils.getFieldValue(topic, "command", Command.class);
            registerCommand(cmd, cmd2Command);
        }
        else if (isInstance(topic, "help.CommandAliasHelpTopic")) {
            String aliasTarget = Util.cleanupCommand(ReflectionUtils.getFieldValue(topic, "aliasFor", String.class));
            String name = Util.cleanupCommand(topic.getName());
            aliases.computeIfAbsent(aliasTarget, k -> new HashSet<>()).add(name);
            aliases.get(aliasTarget).add(aliasTarget);
        }
        else if (isInstance(topic, "help.CustomHelpTopic")) {
            var name = Util.cleanupCommand(topic.getName());
            cmd2Command.putIfAbsent(name, null);
        }
        //else if (isInstance(topic, "help.CustomIndexHelpTopic"))
        //else if (isInstance(topic, "help.MultipleCommandAliasHelpTopic"))
        else {
            var x = ReflectionUtils.getFields(topic.getClass());
            var name = Util.cleanupCommand(topic.getName());
            PluginHider.logger.error("Unknown topic type: " + topic.getClass().getName());
        }
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
                if (cmd2 instanceof BukkitCommand) {
                    var pluginName = JavaPlugin.getProvidingPlugin(cmd2.getClass()).getName();
                    addElement(pluginName, cmd);
                    continue;
                }

                PluginHider.logger.warning("Unknown command: " + cmd + " -> " + pkg);
            }
        }
    }

    private static void addAliases(@NotNull Map<String, Set<String>> aliases, Map<String, Command> cmd2Command) {
        for (Set<String> aliasSet : aliases.values()) {
            String forThis = cmd2Command.keySet().stream().filter(aliasSet::contains).findFirst().orElse(null);
            if (forThis == null) {
                PluginHider.logger.warning("Cannot link these aliases back to its command: " + aliasSet);
                continue;
            }

            for (String alias : aliasSet) {
                cmd2Command.putIfAbsent(Util.cleanupCommand(alias), cmd2Command.get(forThis));
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
                newShouldShowCmd.putIfAbsent(plugin + ":" + cmd, show && colonsAllowed);
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
                    String name = Util.cleanupCommand(plugin.getName());
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
