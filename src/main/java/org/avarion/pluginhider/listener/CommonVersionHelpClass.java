package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.LRUCache;
import org.avarion.pluginhider.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;


public class CommonVersionHelpClass {
    private final LRUCache<UUID, String> usersSeen = new LRUCache<>(1_000);

    private final Function<@NotNull String, Boolean> isCorrectCommand;
    private final Function<@NotNull String, Boolean> shouldShow;
    private final Consumer<@NotNull PlayerCommandPreprocessEvent> handleCommand;

    CommonVersionHelpClass(
            Function<@NotNull String, Boolean> isCorrectCommand,
            Function<@NotNull String, Boolean> shouldShow,
            Consumer<@NotNull PlayerCommandPreprocessEvent> handleCommand
    ) {
        this.isCorrectCommand = isCorrectCommand;
        this.shouldShow = shouldShow;
        this.handleCommand = handleCommand;
    }

    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String cmd = Util.cleanupWord(event.getMessage()); // We want to preserver the '/'!!
        if (isCorrectCommand.apply(cmd)) {
            handleCommand.accept(event);
        }
    }

    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.TAB_COMPLETE || !(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = event.getPlayer();
        if (PluginHider.settings.isOpLike(player)) {
            return;
        }

        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);

        final String txt = Util.cleanupWord(packet.getText()); // Need to keep the '/' !
        if (isCorrectCommand.apply(txt)) {
            usersSeen.put(player.getUniqueId(), txt);
        }
    }

    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.TAB_COMPLETE || !(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = event.getPlayer();
        if (PluginHider.settings.isOpLike(player)) {
            return;
        }

        if (usersSeen.remove(player.getUniqueId()) == null) {
            return;
        }

        WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(event);
        List<WrapperPlayServerTabComplete.CommandMatch> suggestions = packet.getCommandMatches();
        List<WrapperPlayServerTabComplete.CommandMatch> newSuggestions = new ArrayList<>();

        for (WrapperPlayServerTabComplete.CommandMatch suggestion : suggestions) {
            String sug = Util.cleanupCommand(suggestion.getText());
            if (shouldShow.apply(sug)) {
                newSuggestions.add(suggestion);
            }
        }

        if (suggestions.size() != newSuggestions.size()) {
            packet.setCommandMatches(newSuggestions);
        }
    }
}
