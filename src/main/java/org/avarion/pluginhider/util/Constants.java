package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

public class Constants {
    public static final int bstatsPluginId = 22462;
    public static final int spigotPluginId = 117705;

    private static final Set<String> possiblePluginCommands = Set.of("/pl", "/plugins", "/minecraft:pl", "/minecraft:plugins", "/bukkit:pl", "/bukkit:plugins");
    private static final Set<String> possibleVersionCommands = Set.of(
            "/ver",
            "/version",
            "/about",
            "/minecraft:ver",
            "/minecraft:version",
            "/minecraft:about",
            "/bukkit:ver",
            "/bukkit:version",
            "/bukkit:about"
    );

    private static @NotNull String cleanup(@NotNull final String cmd) {
        String[] args = cmd.trim().split("\\s+");
        return args[0].toLowerCase(Locale.ENGLISH);
    }

    public static boolean isPluginCmd(final @Nullable String txt) {
        return txt != null && possiblePluginCommands.contains(cleanup(txt));
    }

    public static boolean isVersionCmd(final @Nullable String txt) {
        return txt != null && possibleVersionCommands.contains(cleanup(txt));
    }
}
