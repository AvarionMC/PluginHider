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
//
//    public void onPacketReceiving(PacketEvent var1) {
//        if (var1.getPacketType() == Client.TAB_COMPLETE) {
//            if (this.plhide.isIsUsingProxy()) {
//                return;
//            }
//
//            if (var1.getPlayer() == null) {
//                this.plhide.getLogger().debug("Failed to read tabcomplete receive: Player is null");
//                var1.setCancelled(true);
//                return;
//            }
//
//            if (var1.getPlayer() instanceof TemporaryPlayer) {
//                this.plhide.getLogger().debug("Failed to read tabcomplete receive: Player is temporary");
//                var1.setCancelled(true);
//                return;
//            }
//
//            if (this.plhide.isLegacy()) {
//                this.incomingCommand.put(var1.getPlayer().getUniqueId(), ((String)var1.getPacket().getSpecificModifier(String.class).read(0)).toLowerCase());
//            } else {
//                String var2 = var1.getPacket().getModifier().read(1).toString().toLowerCase();
//                IConfig var3 = this.plhide.getConfig().getSection("completion_exploit_fixer");
//                if (var3.getBoolean("enabled") && CompletionExploitFixer.checkForInvalidArg(var2)) {
//                    var3 = var3.getSection("should_kick");
//                    var1.setCancelled(true);
//                    if (var3.getBoolean("enabled")) {
//                        var1.getPlayer().kickPlayer(PlHidePro.translateColor(var3.getString("message")));
//                    }
//
//                    return;
//                }
//
//                this.incomingCommand.put(var1.getPlayer().getUniqueId(), var2);
//            }
//        }
//
//    }
}
