package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.LRUCache;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class CommonVersionHelpClass extends PacketListenerAbstract implements Listener {
    private final LRUCache<UUID, String> usersSeen = new LRUCache<>(1_000);

    abstract boolean isCorrectCommand(@Nullable String text);

    abstract boolean shouldShow(@Nullable String text);

    abstract void handleCommand(@NotNull PlayerCommandPreprocessEvent event);

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");

        if (isCorrectCommand(args[0])) {
            handleCommand(event);
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)
            || PluginHider.config.isOpLike(player)
            || event.getPacketType() != PacketType.Play.Client.TAB_COMPLETE) {
            return;
        }

        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);

        final String txt = packet.getText();
        if (isCorrectCommand(txt)) {
            usersSeen.put(player.getUniqueId(), txt);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.TAB_COMPLETE) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player) || PluginHider.config.isOpLike(player)) {
            return;
        }

        String prevCmd = usersSeen.remove(player.getUniqueId());
        if (prevCmd == null) {
            return;
        }

        WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(event);
        List<WrapperPlayServerTabComplete.CommandMatch> suggestions = packet.getCommandMatches();
        List<WrapperPlayServerTabComplete.CommandMatch> newSuggestions = new ArrayList<>();

        for (WrapperPlayServerTabComplete.CommandMatch suggestion : suggestions) {
            if (shouldShow(suggestion.getText())) {
                newSuggestions.add(suggestion);
            }
        }

        if (suggestions.size() != newSuggestions.size()) {
            packet.setCommandMatches(newSuggestions);
        }
    }
}
