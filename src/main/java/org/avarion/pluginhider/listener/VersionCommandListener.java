package org.avarion.pluginhider.listener;

import org.avarion.pluginhider.util.Config;
import org.avarion.pluginhider.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class VersionCommandListener extends CommonVersionHelpClass implements Listener {

    void handleCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");
        if (args.length < 2) {
            // This is when you ask '/version': ie the servers version
            event.setCancelled(true);
            return;
        }

        // This is when you ask /version <plugin>
        if (!shouldShow(args[1])) {
            Player player = event.getPlayer();
            // default message from spigot when a plugin isn't found.
            player.sendMessage("This server is not running any plugin by that name.");
            player.sendMessage("Use /plugins to get a list of plugins.");

            event.setCancelled(true);
        }
    }

    boolean isCorrectCommand(@Nullable String text) {
        return Constants.isVersionCmd(text);
    }

    boolean shouldShow(@Nullable String text) {
        return Config.shouldShowPlugin(text);
    }
}
