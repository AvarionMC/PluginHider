package org.avarion.pluginhider.util;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;


public class Constants {
    public static final int bstatsPluginId = 22462;
    public static final int spigotPluginId = 117705;

    public static final @Unmodifiable Set<String> servers = Set.of("minecraft", "paper", "bukkit", "spigot");

    private static final boolean IS_PAPER;

    static {
        IS_PAPER = isPaper();
    }

    private static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.command.PaperPluginsCommand");
            return true;
        }
        catch (ClassNotFoundException ignored) {
        }

        return false;
    }

    public static boolean isPaperServer() {
        return IS_PAPER;
    }
}
