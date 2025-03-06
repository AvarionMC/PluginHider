package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.avarion.pluginhider.util.Config;
import org.avarion.pluginhider.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;


public class VersionCommandListener extends PacketListenerAbstract implements Listener {
    private final CommonVersionHelpClass common;

    public VersionCommandListener() {
        common = new CommonVersionHelpClass(this::isCorrectCommand, this::shouldShow, this::handleCommand);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        common.onPacketReceive(event);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        common.onPacketSend(event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        common.onCommand(event);
    }

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

    boolean isCorrectCommand(@NotNull String text) {
        return Constants.isVersionCmd(text);
    }

    boolean shouldShow(@NotNull String text) {
        return Config.shouldShowPlugin(text);
    }
}
