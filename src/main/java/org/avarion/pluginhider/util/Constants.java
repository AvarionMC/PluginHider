package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

public class Constants {
    public static final int bstatsPluginId = 22462;
    public static final int spigotPluginId = 117705;

    private static final Set<String> possibleCommands = Set.of(
            "/pl",
            "/plugins",
            "/ver",
            "/version",
            "/about",
            "/?",
            "/help",
            "/minecraft:pl",
            "/minecraft:plugins",
            "/minecraft:ver",
            "/minecraft:version",
            "/minecraft:about",
            "/minecraft:?",
            "/minecraft:help",
            "/bukkit:pl",
            "/bukkit:plugins",
            "/bukkit:ver",
            "/bukkit:version",
            "/bukkit:about",
            "/bukkit:?",
            "/bukkit:help"
    );

    private static @NotNull String cleanup(@NotNull final String cmd) {
        String[] args = cmd.trim().split("\\s+");
        return args[0].toLowerCase(Locale.ENGLISH);
    }

    public static boolean shouldHideThisCommand(final @Nullable String txt) {
        return txt != null && possibleCommands.contains(cleanup(txt));
    }
}
