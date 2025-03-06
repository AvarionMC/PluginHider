package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class Caches {
    private Caches() {
    }

    public final static Map<String, Boolean> showCache = new LRUCache<>(1_000);
    public final static Map<String, Boolean> showCachePlugins = new LRUCache<>(1_000);
    public final static Map<String, String> cacheCommand2Plugin = new HashMap<>();
    public final static Map<String, Set<String>> cachePlugin2Commands = new HashMap<>();

    private static final LRUCache<String, CommandType> commandsCache = new LRUCache<>(1_000);

    public static CommandType getCommandType(@Nullable final String cmd) {
        return commandsCache.computeIfAbsent(
                cmd, k -> {
                    if (k == null) {
                        return CommandType.OTHER;
                    }
                    k = k.trim();
                    var firstSpace = k.indexOf(' ');
                    if (firstSpace != -1) {
                        k = k.substring(0, firstSpace);
                    }
                    k = k.toLowerCase(Locale.ENGLISH);
                    if (Constants.possiblePluginCommands.contains(k)) {
                        return CommandType.PLUGINS;
                    }
                    if (Constants.possibleVersionCommands.contains(k)) {
                        return CommandType.VERSION;
                    }
                    if (Constants.possibleHelpCommands.contains(k)) {
                        return CommandType.HELP;
                    }
                    return CommandType.OTHER;
                }
        );
    }

    public static boolean isPluginCmd(final @Nullable String txt) {
        return getCommandType(txt) == CommandType.PLUGINS;
    }

    public static boolean isVersionCmd(final @Nullable String txt) {
        return getCommandType(txt) == CommandType.VERSION;
    }

    public static boolean isHelpCmd(final @Nullable String txt) {
        return getCommandType(txt) == CommandType.HELP;
    }

    @Contract("null -> new")
    public static String @NotNull [] splitPluginName(@Nullable final String pluginName) {
        if (pluginName == null || pluginName.isBlank()) {
            return new String[]{
                    null, null
            };
        }

        String cleaned = pluginName.toLowerCase().trim().split("\\s+", 2)[0];

        String[] parts = cleaned.split(":", 2);
        if (parts.length == 2) {
            final String plugin = parts[0];
            final String cmd = parts[1];
            cacheCommand2Plugin.putIfAbsent(cmd, plugin);
            cachePlugin2Commands.computeIfAbsent(plugin, p -> new HashSet<>()).add(cmd);
            return parts;
        }

        return new String[]{null, parts[0]};
    }

    /**
     * Expected `pluginName` to be trimmed & lowered
     */
    public static boolean shouldShowPlugin(@Nullable final String pluginName) {
        return Caches.showCachePlugins.computeIfAbsent(pluginName, PluginHider.config::shouldShowPlugin);
    }

    public static boolean shouldShow(@Nullable final String pluginName) {
        return Caches.showCache.computeIfAbsent(pluginName, PluginHider.config::shouldShow);
    }
}
