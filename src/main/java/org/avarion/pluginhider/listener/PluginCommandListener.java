package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.LRUCache;
import org.avarion.pluginhider.util.ReceivedPackets;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PluginCommandListener extends PacketListenerAbstract {
    private final LRUCache<UUID, ReceivedPackets> usersSeen = new LRUCache<>(1000);

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (player.isOp()) {
            return;
        }

        String cmd = null;
        if (event.getPacketType() == Client.CHAT_COMMAND) {
            cmd = "/" + new WrapperPlayClientChatCommand(event).getCommand();
        }
        else if (event.getPacketType() == Client.CHAT_COMMAND_UNSIGNED) {
            cmd = "/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand();
        }

        if (Constants.isPluginCmd(cmd)) {
            usersSeen.put(player.getUniqueId(), new ReceivedPackets());
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != Server.SYSTEM_CHAT_MESSAGE) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (player.isOp()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        ReceivedPackets entry = usersSeen.get(uuid);
        if (entry == null) {
            return;
        }

        Component text = new WrapperPlayServerSystemChatMessage(event).getMessage();

        assert text instanceof TextComponent;
        StringBuilder sb = new StringBuilder();
        getFullLine((TextComponent) text, sb);

        entry.addSystemChatLine(sb.toString());

        if (entry.amountOfPlugins == 0) {
            // No plugins...
            usersSeen.remove(uuid);
            return;
        }

        // Don't send out the original text.
        event.setCancelled(true);

        if (entry.isFinished()) {
            // Remove this from our cache, so we don't intercept it again
            usersSeen.remove(uuid);
            // Send messages if there is anything to send.
            entry.sendModifiedMessage(player);
        }
    }

    private void getFullLine(final @NotNull TextComponent line, final @NotNull StringBuilder sb) {
        sb.append(line.content());
        for (var component : line.children()) {
            assert component instanceof TextComponent;
            getFullLine((TextComponent) component, sb);
        }
    }
}
