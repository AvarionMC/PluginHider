package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.Node;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeclareCommands;
import org.avarion.pluginhider.PluginHider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FilterServerCmd {
    private static final byte FLAG_HAS_REDIRECT = 0x8;
    private final WrapperPlayServerDeclareCommands packet;
    private final List<Node> nodes;
    private final Node rootNode;
    private final List<Node> newList = new ArrayList<>();
    private final Map<Integer, Integer> indexTranslations = new HashMap<>(); // Mapping from original index -> new index
    private final Map<String, Set<String>> pluginToCmd = new HashMap<>(); // Mapping from plugin name -> list of commands

    public FilterServerCmd(@NotNull PacketSendEvent event) {
        packet = new WrapperPlayServerDeclareCommands(event);
        nodes = packet.getNodes();
        rootNode = nodes.get(packet.getRootIndex());

        loadPluginCommands();
    }

    private void loadPluginCommands() {
        for (Integer idx : rootNode.getChildren()) {
            if (idx==null) continue;
            final String name = nodes.get(idx).getName().orElse("").toLowerCase(Locale.ENGLISH);
            if (!name.contains(":")) continue;

            String[] parts = name.split(":", 2);
            pluginToCmd.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
        }
    }

    private boolean shouldShow(@NotNull String name) {
        if (name.isEmpty()) return false;

        name = name.toLowerCase(Locale.ENGLISH);
        String[] parts = name.split(":", 2);

        String pluginName = null;
        if (parts.length==2) { // it has ':'
            if (!PluginHider.config.shouldAllowConolOnTabComplete) return false;
            pluginName = parts[0];
        } else { // No ':' in it
            for (var entry : pluginToCmd.entrySet()) {
                if (entry.getValue().contains(name)) {
                    pluginName = entry.getKey();
                    break;
                }
            }
        }

        if (pluginName==null) return true; // ???!!!

        return PluginHider.config.shouldShow(pluginName);
    }

    private boolean hasRedirect(@NotNull Node node) {
        return (node.getFlags() & FLAG_HAS_REDIRECT)==FLAG_HAS_REDIRECT && node.getRedirectNodeIndex()!=0;
    }

    private void handleRedirection(@Nullable Node node) {
        if (node==null || !hasRedirect(node)) {
            return;
        }

        final int redirectNodeIndex = node.getRedirectNodeIndex();
        final Node redirectedNode = nodes.get(redirectNodeIndex);
        assert redirectedNode!=null;

        Integer newRedirectNodeIndex = indexTranslations.get(redirectNodeIndex);
        if (newRedirectNodeIndex==null) { // unknown yet
            newRedirectNodeIndex = newList.size();
            node.setRedirectNodeIndex(newRedirectNodeIndex);
            newList.add(redirectedNode);

            indexTranslations.put(redirectNodeIndex, newRedirectNodeIndex);

            // Only dig deeper when I don't know it yet
            handleRedirection(nodes.get(redirectNodeIndex));
        }
    }

    public void filter() {
        newList.clear();
        newList.add(rootNode);

        filter(rootNode, true);
    }

    private void filter(@Nullable Node node, boolean handlingRoot) {
        if (node==null || node.getChildren()==null || node.getChildren().isEmpty()) return;

        List<Integer> newChildren = new ArrayList<>();

        for (Integer idx : node.getChildren()) {
            if (idx==null) continue;

            if (idx==39) {
                int b = 1;
            }
            Node child = nodes.get(idx);
            if (child==null) continue;

            if (!handlingRoot || shouldShow(child.getName().orElse(""))) {
                if (indexTranslations.containsKey(idx)) {
                    // Already in the list!
                    newChildren.add(indexTranslations.get(idx));
                } else {
                    final int newIndex = newList.size();

                    newChildren.add(newIndex);
                    newList.add(child);
                    indexTranslations.put(idx, newIndex);

                    filter(child, false);
                    handleRedirection(child);
                }
            }
        }
        node.setChildren(newChildren);
        if (handlingRoot) packet.setNodes(newList);
    }
}
