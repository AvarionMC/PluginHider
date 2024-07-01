package org.avarion.plugin_hider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.LRUCache;
import org.avarion.plugin_hider.util.ReceivedPackets;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PluginCommandListener implements Listener {
    final private PluginHider plugin;
    final private Set<String> possible_plugin_commands = Set.of("/pl", "/plugins", "/bukkit:pl", "/bukkit:plugins");

    public PluginCommandListener(PluginHider plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");

        String firstArg = args[0].toLowerCase();
        if (!possible_plugin_commands.contains(firstArg)) {
            return;
        }

        plugin.cachedUsers.put(
            event.getPlayer().getUniqueId(),
            new ReceivedPackets()
        );
    }
}
