package org.avarion.pluginhider.util;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Constants {
    public static final int bstatsPluginId = 22462;
    public static final int spigotPluginId = 117705;

    private static final LRUCache<String, CommandType> commandsCache = new LRUCache<>(1_000);
    public static final Set<String> servers = Set.of("minecraft", "paper", "bukkit", "spigot");
    public static final Map<String, String> cacheCommand2Plugin = new HashMap<>();
    public static final Map<String, Set<String>> cachePlugin2Commands = new HashMap<>();
    private static final Set<String> possiblePluginCommands = Set.of(
            "/pl",
            "/plugins",
            "/minecraft:pl",
            "/minecraft:plugins",
            "/bukkit:pl",
            "/bukkit:plugins",
            "/paper:pl",
            "/paper:plugins"
    );
    private static final Set<String> possibleVersionCommands = Set.of(
            "/ver",
            "/version",
            "/about",
            "/minecraft:ver",
            "/minecraft:version",
            "/minecraft:about",
            "/bukkit:ver",
            "/bukkit:version",
            "/bukkit:about", "/paper:ver", "/paper:version", "/paper:about"
    );
    private static final Set<String> possibleHelpCommands = Set.of(
            "/?", "/help", "/minecraft:?", "/minecraft:help",
            "/bukkit:?", "/bukkit:help", "/paper:?", "/paper:help"
    );

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
                    if (possiblePluginCommands.contains(k)) {
                        return CommandType.PLUGINS;
                    }
                    if (possibleVersionCommands.contains(k)) {
                        return CommandType.VERSION;
                    }
                    if (possibleHelpCommands.contains(k)) {
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
}
