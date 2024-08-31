/*
package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.LRUCache;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TabCompleteListener extends PacketListenerAbstract {
    final private Map<UUID, String> latestTabCompletion = new LRUCache<UUID, String>(1000);
    final private Set<PacketTypeCommon> types = Set.of(
            Client.CHAT_COMMAND,
            Client.CHAT_COMMAND_UNSIGNED,
            Server.CHAT_MESSAGE,
            Server.CHAT_PREVIEW_PACKET,
            Server.CUSTOM_CHAT_COMPLETIONS,
            Server.DISPLAY_CHAT_PREVIEW,
            Server.PLAYER_CHAT_HEADER,
            Server.SYSTEM_CHAT_MESSAGE,
            Server.DELETE_CHAT,
            Server.DISGUISED_CHAT,
            Client.TAB_COMPLETE,
            Server.TAB_COMPLETE
    );

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (types.contains(event.getPacketType())) {
            doTabCompleteReceive(player, event);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        else if (types.contains(event.getPacketType())) {
            doTabCompleteSend(player, event);
        }
    }

    private void doTabCompleteReceive(@NotNull Player player, PacketReceiveEvent event) {
        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);
        final String cmd = packet.getText();
        String[] args = cmd.split("\\s+");

        String firstArg = args[0].toLowerCase();
        if (Constants.isVersionCmd(firstArg)) {
            latestTabCompletion.put(player.getUniqueId(), cmd);
        }
    }

    private void doTabCompleteSend(@NotNull Player player, PacketSendEvent event) {
        UUID playerUuid = player.getUniqueId();
        if (!latestTabCompletion.containsKey(playerUuid)) {
            return;
        }

        WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(event);
        List<WrapperPlayServerTabComplete.CommandMatch> suggestions = packet.getCommandMatches();
        List<String> plugins = suggestions.stream().map(WrapperPlayServerTabComplete.CommandMatch::getText).filter(PluginHider.config::shouldShow)
                                          .toList();

        PluginHider.logger.debug("Filtered TabCompletion request: {0}", plugins);

        if (suggestions.size() == plugins.size()) {
            return; // Nothing removed
        }

        if (plugins.isEmpty()) {
            // Everything removed
            event.setCancelled(true);
            return;
        }

//        StringRange sr = suggestions.get(0).getRange();
//        Suggestions newSuggestions = new Suggestions(sr, plugins.stream()
//                                                                .map(name -> new Suggestion(sr, name))
//                                                                .toList());
//        packet.setCommandMatches(newSuggestions);
    }
}
*/