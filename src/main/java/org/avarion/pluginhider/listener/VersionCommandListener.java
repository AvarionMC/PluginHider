package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete.CommandMatch;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.LRUCache;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VersionCommandListener extends PacketListenerAbstract implements Listener {
    private final LRUCache<UUID, String> usersSeen = new LRUCache<>(1000);

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");

        String firstArg = args[0].toLowerCase();
        if (Constants.isVersionCmd(firstArg)) {
            handleVersionCommand(event);
        }
    }

    private void handleVersionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");
        if (args.length != 2) {
            event.setCancelled(true);
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

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player) || player.isOp() || event.getPacketType() != Client.TAB_COMPLETE) {
            return;
        }

        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);
        final String txt = packet.getText();

        if (Constants.isVersionCmd(txt)) {
            usersSeen.put(player.getUniqueId(), txt);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != Server.TAB_COMPLETE) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player) || player.isOp()) {
            return;
        }

        String prevCmd = usersSeen.remove(player.getUniqueId());
        if (prevCmd == null) {
            return;
        }

        WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(event);
        List<CommandMatch> suggestions = packet.getCommandMatches();
        List<CommandMatch> newSuggestions = new ArrayList<>();

        for (CommandMatch suggestion : suggestions) {
            if (PluginHider.config.shouldShow(suggestion.getText())) {
                newSuggestions.add(suggestion);
            }
        }

        if (suggestions.size() != newSuggestions.size()) {
            packet.setCommandMatches(newSuggestions);
        }
    }
}
