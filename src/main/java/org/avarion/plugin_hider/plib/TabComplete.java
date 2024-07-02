package org.avarion.plugin_hider.plib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import org.avarion.plugin_hider.PluginHider;

public class TabComplete extends PacketAdapter {
    private final PluginHider plugin;

    public TabComplete(PluginHider plugin, ListenerPriority priority, Iterable<PacketType> list, ListenerOptions... options) {
        super(plugin, priority, list, options);
        this.plugin = plugin;
    }
}
