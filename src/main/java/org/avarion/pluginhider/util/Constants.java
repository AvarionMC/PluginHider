package org.avarion.pluginhider.util;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.Set;


public class Constants {
    public static final int bstatsPluginId = 22462;
    public static final int spigotPluginId = 117705;

    public static final @Unmodifiable Set<String> servers = Set.of("minecraft", "paper", "bukkit", "spigot");

    public static final @Unmodifiable Set<String> possiblePluginCommands;
    public static final @Unmodifiable Set<String> possibleVersionCommands;
    public static final @Unmodifiable Set<String> possibleHelpCommands;

    private static final @Unmodifiable Set<String> pluginCommands = Set.of("/pl", "/plugins");
    private static final @Unmodifiable Set<String> versionCommands = Set.of(
            "/ver",
            "/version",
            "/icanhasbukkit",
            "/about"
    );
    private static final @Unmodifiable Set<String> helpCommands = Set.of("/?", "/help");

    private static @Unmodifiable Set<String> loadCommands(Set<String> source) {
        Set<String> into = new HashSet<>(source);
        for (String command : source) {
            for (String server : servers) {
                into.add(server + ":" + command);
            }
        }
        return ImmutableSet.copyOf(into);
    }

    static {
        possiblePluginCommands = loadCommands(pluginCommands);
        possibleVersionCommands = loadCommands(versionCommands);
        possibleHelpCommands = loadCommands(helpCommands);
    }
}
