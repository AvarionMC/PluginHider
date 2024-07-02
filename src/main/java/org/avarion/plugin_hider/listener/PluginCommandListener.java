package org.avarion.plugin_hider.listener;

import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.Constants;
import org.avarion.plugin_hider.util.ReceivedPackets;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class PluginCommandListener implements Listener {
    final private PluginHider plugin;

    public PluginCommandListener(PluginHider plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");

        String firstArg = args[0].toLowerCase();
        if (Constants.isPluginCmd(firstArg)) {
            handlePluginsCommand(event);
        }
        if (Constants.isVersionCmd(firstArg)) {
            handleVersionCommand(event);
        }
    }

    private void handlePluginsCommand(@NotNull PlayerCommandPreprocessEvent event) {
        plugin.cachedUsers.put(event.getPlayer().getUniqueId(), new ReceivedPackets(10));
    }

    private void handleVersionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ", 2);
        if (args.length != 2) {
            return;
        }

        String pluginName = args[1].trim();
        if (!plugin.getMyConfig().shouldShow(pluginName)) {
            Player player = event.getPlayer();
            // default message from spigot when a plugin isn't found.
            player.sendMessage("This server is not running any plugin by that name.");
            player.sendMessage("Use /plugins to get a list of plugins.");

            event.setCancelled(true);
        }
    }
}
