package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.LRUCache;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TabCompleteListener extends PacketListenerAbstract {
    final private Map<UUID, String> latestTabCompletion = new LRUCache<>(1000);

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
//        if (!(event.getPlayer() instanceof Player player)) {
//            return;
//        }

        Player player = (Player) event.getPlayer();
        if (event.getPacketType()==Client.TAB_COMPLETE) {
            doTabCompleteReceive(player, event);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
//        if (!(event.getPlayer() instanceof Player player)) {
//            return;
//        }
        Player player = (Player) event.getPlayer();

        if (event.getPacketType()==Server.TAB_COMPLETE) {
            doTabCompleteSend(player, event);
        } else if (event.getPacketType()==Server.DECLARE_COMMANDS) {
            new FilterServerCmd(event).filter();
        }
    }

    private void doTabCompleteReceive(@NotNull Player player, PacketReceiveEvent event) {
        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);
        final String cmd = packet.getText();
        latestTabCompletion.put(player.getUniqueId(), cmd);

        PluginHider.logger.debug("Incoming TabCompletion request: {0}", cmd);
    }

    private void doTabCompleteSend(@NotNull Player player, PacketSendEvent event) {
        UUID playerUuid = player.getUniqueId();
        if (!latestTabCompletion.containsKey(playerUuid)) {
            PluginHider.logger.error("Received Server.TAB_COMPLETE from {0}, but I don't know the player?", playerUuid);
            return;
        }

        WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(event);
        List<WrapperPlayServerTabComplete.CommandMatch> matches = packet.getCommandMatches();
        int a = 1;
        /*
        List<Suggestion> suggestions = ((Suggestions) event.getPacket().getModifier().read(1)).getList();
        List<String> plugins = suggestions.stream().map(Suggestion::getText).filter(PluginHider.config::shouldShow)
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

        StringRange sr = suggestions.get(0).getRange();
        Suggestions newSuggestions = new Suggestions(sr, plugins.stream()
                                                                .map(name -> new Suggestion(sr, name))
                                                                .toList());
        event.getPacket().getModifier().write(1, newSuggestions);
*/
    }
}
