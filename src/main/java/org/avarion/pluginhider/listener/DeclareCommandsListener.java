package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.Node;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeclareCommands;
import org.avarion.pluginhider.PluginHider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DeclareCommandsListener extends PacketListenerAbstract {
    private static final byte FLAG_HAS_REDIRECT = 0x8;

    private static class Internal {
        public final WrapperPlayServerDeclareCommands packet;
        public final List<Node> nodes;
        public final Node rootNode;
        public final List<Node> newList = new ArrayList<>();
        public final Map<Integer, Integer> indexTranslations = new HashMap<>(); // Mapping from original index -> new index
        public final Map<String, Set<String>> pluginToCmd = new HashMap<>(); // Mapping from plugin name -> list of commands

        Internal(PacketSendEvent event) {
            packet = new WrapperPlayServerDeclareCommands(event);
            nodes = packet.getNodes();
            rootNode = nodes.get(packet.getRootIndex());
            newList.add(rootNode);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.DECLARE_COMMANDS) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            PluginHider.logger.warning("not a player found!");
            event.setCancelled(true);
            return;
        }

        if (PluginHider.config.getOperatorCanSeeEverything() && player.isOp()) {
            return;
        }

        Internal internal = new Internal(event);
        loadPluginCommands(internal);
        filter(internal, internal.rootNode, false);
        internal.packet.setNodes(internal.newList);
    }

    private void loadPluginCommands(@NotNull Internal data) {
        for (Integer idx : data.rootNode.getChildren()) {
            if (idx == null) {
                continue;
            }
            final String name = data.nodes.get(idx).getName().orElse("").toLowerCase(Locale.ENGLISH);
            if (!name.contains(":")) {
                continue;
            }

            String[] parts = name.split(":", 2);
            data.pluginToCmd.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
        }
    }

    private boolean shouldShow(@NotNull String name, Internal data) {
        if (name.isEmpty()) {
            return false;
        }

        name = name.toLowerCase(Locale.ENGLISH);
        String[] parts = name.split(":", 2);

        String pluginName = null;
        if (parts.length == 2) { // it has ':'
            if (!PluginHider.config.shouldAllowConolOnTabComplete) {
                return false;
            }
            pluginName = parts[0];
        }
        else { // No ':' in it
            for (var entry : data.pluginToCmd.entrySet()) {
                if (entry.getValue().contains(name)) {
                    pluginName = entry.getKey();
                    break;
                }
            }
        }

        if (pluginName == null) {
            return true; // ???!!!
        }

        return PluginHider.config.shouldShow(pluginName);
    }

    private boolean hasRedirect(@NotNull Node node) {
        return (node.getFlags() & FLAG_HAS_REDIRECT) == FLAG_HAS_REDIRECT && node.getRedirectNodeIndex() != 0;
    }

    private void handleRedirection(@Nullable Node node, Internal data) {
        if (node == null || !hasRedirect(node)) {
            return;
        }

        final int redirectNodeIndex = node.getRedirectNodeIndex();
        final Node redirectedNode = data.nodes.get(redirectNodeIndex);
        assert redirectedNode != null;

        Integer newRedirectNodeIndex = data.indexTranslations.get(redirectNodeIndex);
        if (newRedirectNodeIndex == null) { // unknown yet
            newRedirectNodeIndex = data.newList.size();
            node.setRedirectNodeIndex(newRedirectNodeIndex);
            data.newList.add(redirectedNode);

            data.indexTranslations.put(redirectNodeIndex, newRedirectNodeIndex);

            // Only dig deeper when I don't know it yet
            handleRedirection(data.nodes.get(redirectNodeIndex), data);
        }
    }

    private void filter(@NotNull Internal data, @Nullable Node node, boolean alwaysAdd) {
        if (node == null || node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }

        List<Integer> newChildren = new ArrayList<>();

        for (Integer idx : node.getChildren()) {
            if (idx == null) {
                continue;
            }

            Node child = data.nodes.get(idx);
            if (child == null) {
                continue;
            }

            if (alwaysAdd || shouldShow(child.getName().orElse(""), data)) {
                if (data.indexTranslations.containsKey(idx)) {
                    // Already in the list!
                    newChildren.add(data.indexTranslations.get(idx));
                }
                else {
                    final int newIndex = data.newList.size();

                    newChildren.add(newIndex);
                    data.newList.add(child);
                    data.indexTranslations.put(idx, newIndex);

                    filter(data, child, true);
                    handleRedirection(child, data);
                }
            }
        }
        node.setChildren(newChildren);
    }
}
