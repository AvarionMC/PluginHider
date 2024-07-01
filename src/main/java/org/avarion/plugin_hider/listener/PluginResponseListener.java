package org.avarion.plugin_hider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.LRUCache;
import org.avarion.plugin_hider.util.ReceivedPackets;

public class PluginResponseListener extends PacketAdapter {
    final private PluginHider plugin;

    public PluginResponseListener(PluginHider plugin) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT);

        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        System.out.println(">>> [onPacketSending] Packet event found: " + event.getPacketType());
        System.out.println(">>> [onPacketSending] Player for event found: " + event.getPlayer().getName());
        System.out.println(">>> [onPacketSending] Strings found: " + event.getPacket().getStrings());
    }
}
