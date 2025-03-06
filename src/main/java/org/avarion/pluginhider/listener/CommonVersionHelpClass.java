package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.LRUCache;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;


public class CommonVersionHelpClass {
    private final LRUCache<UUID, String> usersSeen = new LRUCache<>(1_000);
    private final LRUCache<String, String> cache = new LRUCache<>(1_000);

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

    @NotNull String cleanup(@NotNull final String text) {
        return cache.computeIfAbsent(
                text, t -> {
                    return t.trim().toLowerCase(Locale.ENGLISH);
                }
        );
    }

    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");

        if (args[0] != null && isCorrectCommand.apply(cleanup(args[0]))) {
            handleCommand.accept(event);
        }
    }

    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)
            || PluginHider.config.isOpLike(player)
            || event.getPacketType() != PacketType.Play.Client.TAB_COMPLETE) {
            return;
        }

        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);

        final String txt = packet.getText();
        if (txt != null && isCorrectCommand.apply(cleanup(txt))) {
            usersSeen.put(player.getUniqueId(), txt);
        }
    }

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
            String sug = suggestion.getText();
            if (sug != null && shouldShow.apply(cleanup(sug))) {
                newSuggestions.add(suggestion);
            }
        }

        if (suggestions.size() != newSuggestions.size()) {
            packet.setCommandMatches(newSuggestions);
        }
    }
}
