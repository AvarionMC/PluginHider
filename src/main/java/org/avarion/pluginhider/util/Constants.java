package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Constants {
    final public static int bstatsPluginId = 22462;
    final public static int spigotPluginId = 117705;

    final public static String PREFIX = "[PluginHider] ";

    final private static Set<String> possiblePluginCommands = Set.of("/pl", "/plugins", "/minecraft:pl", "/minecraft:plugins", "/bukkit:pl", "/bukkit:plugins");
    final private static Set<String> possibleVersionCommands = Set.of("/ver", "/version", "/about", "/minecraft:ver", "/minecraft:version", "/minecraft:about", "/bukkit:ver", "/bukkit:version", "/bukkit:about");

    public static boolean isPluginCmd(@NotNull final String txt) {
        return possiblePluginCommands.contains(txt.trim().toLowerCase());
    }

    public static boolean isVersionCmd(final @NotNull String txt) {
        return possibleVersionCommands.contains(txt.trim().toLowerCase());
    }
}
