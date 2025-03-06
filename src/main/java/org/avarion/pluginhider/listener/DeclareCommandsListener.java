package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.Node;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeclareCommands;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Caches;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class DeclareCommandsListener extends PacketListenerAbstract {
    private static final byte FLAG_HAS_REDIRECT = 0x8;

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

        if (PluginHider.config.isOpLike(player)) {
            return;
        }

        Internal internal = new Internal(event);
        filter(internal, internal.rootNode, false);
        internal.packet.setNodes(internal.newList);
        internal.packet.write();
    }

    private boolean hasRedirect(@NotNull Node node) {
        return (node.getFlags() & FLAG_HAS_REDIRECT) == FLAG_HAS_REDIRECT && node.getRedirectNodeIndex() != 0;
    }

    private void handleRedirection(@Nullable Node node, Internal data) {
        if (node == null || !hasRedirect(node)) {
            return;
        }

        final int oldRedirectIndex = node.getRedirectNodeIndex();
        final Node redirectedNode = data.nodes.get(oldRedirectIndex);
        assert redirectedNode != null;

        Integer newRedirectIndex = data.indexTranslations.get(oldRedirectIndex);
        if (newRedirectIndex != null) {
            node.setRedirectNodeIndex(newRedirectIndex);
        }
        else { // unknown yet
            newRedirectIndex = data.newList.size();
            node.setRedirectNodeIndex(newRedirectIndex);
            data.newList.add(redirectedNode);

            data.indexTranslations.put(oldRedirectIndex, newRedirectIndex);

            // Only dig deeper when I don't know it yet
            handleRedirection(data.nodes.get(oldRedirectIndex), data);
        }
    }

    private void filter(@NotNull Internal data, @Nullable Node node, boolean alwaysAdd) {
        if (node == null || node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }

        List<Integer> newChildren = new ArrayList<>();

        int childIndex = -1;
        for (Integer idx : node.getChildren()) {
            childIndex += 1;
            if (idx == null) {
                continue;
            }

            Node child = data.nodes.get(idx);
            if (child == null) {
                continue;
            }

            final String name = child.getName().orElse("");
            if (alwaysAdd || Caches.shouldShow(name)) {
                if (data.indexTranslations.containsKey(idx)) {
                    // Already in the list!
                    newChildren.add(data.indexTranslations.get(idx));
                }
                else {
                    final int newIndex = data.newList.size();

                    newChildren.add(newIndex);
                    node.getChildren().set(childIndex, newIndex);
                    data.newList.add(child);
                    data.indexTranslations.put(idx, newIndex);

                    filter(data, child, true);
                    handleRedirection(child, data);
                }
            }
        }
        node.setChildren(newChildren);
    }

    private static class Internal {
        private final WrapperPlayServerDeclareCommands packet;
        private final List<Node> nodes;
        private final Node rootNode;
        private final List<Node> newList = new ArrayList<>();
        private final Map<Integer, Integer> indexTranslations = new HashMap<>();
        // Mapping from original index -> new index

        Internal(PacketSendEvent event) {
            packet = new WrapperPlayServerDeclareCommands(event);
            nodes = packet.getNodes();
            rootNode = nodes.get(packet.getRootIndex());
            newList.add(rootNode);

            loadPluginCommands();
        }

        private void loadPluginCommands() {
            for (Integer idx : rootNode.getChildren()) {
                if (idx == null) {
                    continue;
                }

                final String name = nodes.get(idx).getName().orElse("");
                var res = Caches.splitPluginName(name); // This handles the storing if it's not registered yet
                PluginHider.logger.info("res: " + Arrays.toString(res));
            }
        }
    }
}
