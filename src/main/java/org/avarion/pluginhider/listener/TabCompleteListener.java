package org.avarion.pluginhider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.LRUCache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TabCompleteListener extends PacketAdapter {
    final private PluginHider plugin;
    final private Map<UUID, String> latestTabCompletion = new LRUCache<>(1000);

    public TabCompleteListener(PluginHider plugin) {
        super(plugin, ListenerPriority.HIGHEST, Arrays.asList(PacketType.Play.Server.TAB_COMPLETE, PacketType.Play.Client.TAB_COMPLETE), ListenerOptions.ASYNC);

        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.TAB_COMPLETE) {
            return;
        }

        final String cmd = event.getPacket().getModifier().read(1).toString().toLowerCase();
        latestTabCompletion.put(event.getPlayer().getUniqueId(), cmd);

        plugin.logger.debug("Incoming TabCompletion request: {0}", cmd);
    }

    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.TAB_COMPLETE) {
            return;
        }

        UUID player = event.getPlayer().getUniqueId();
        if (!latestTabCompletion.containsKey(player)) {
            return;
        }

        List<Suggestion> suggestions = ((Suggestions) event.getPacket().getModifier().read(1)).getList();
        List<String> plugins = suggestions.stream().map(Suggestion::getText).filter(plugin.getMyConfig()::shouldShow)
                                          .toList();

        plugin.logger.debug("Filtered TabCompletion request: {0}", plugins);

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
    }
}
