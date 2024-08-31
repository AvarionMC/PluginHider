/*
package org.avarion.pluginhider.listener;

import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class PluginCommandListener implements Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");

        String firstArg = args[0].toLowerCase();
        if (Constants.isVersionCmd(firstArg)) {
            handleVersionCommand(event);
        }
    }

    private void handleVersionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ", 2);
        if (args.length != 2) {
            return;
        }

        String pluginName = args[1].trim();
        if (!PluginHider.config.shouldShow(pluginName)) {
            Player player = event.getPlayer();
            // default message from spigot when a plugin isn't found.
            player.sendMessage("This server is not running any plugin by that name.");
            player.sendMessage("Use /plugins to get a list of plugins.");

            event.setCancelled(true);
        }
    }
}
*/