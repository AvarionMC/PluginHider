package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete.CommandMatch;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Config;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.LRUCache;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class VersionCommandListener extends PacketListenerAbstract implements Listener {
    private final LRUCache<UUID, String> usersSeen = new LRUCache<>(1000);

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");

        String firstArg = args[0].toLowerCase();
        if (Constants.isVersionCmd(firstArg)) {
            handleVersionCommand(event);
        }
    }

    private void handleVersionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split("\\s+");
        if (args.length < 2) {
            // This is when you ask '/version': ie the servers version
            event.setCancelled(true);
            return;
        }

        // This is when you ask /version <plugin>
        if (!Config.shouldShow(args[1])) {
            Player player = event.getPlayer();
            // default message from spigot when a plugin isn't found.
            player.sendMessage("This server is not running any plugin by that name.");
            player.sendMessage("Use /plugins to get a list of plugins.");

            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)
            || PluginHider.config.isOpLike(player)
            || event.getPacketType() != Client.TAB_COMPLETE) {
            return;
        }

        WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);
        final String txt = packet.getText();

        if (Constants.isVersionCmd(txt) || Constants.isHelpCmd(txt)) {
            usersSeen.put(player.getUniqueId(), txt);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != Server.TAB_COMPLETE) {
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
        List<CommandMatch> suggestions = packet.getCommandMatches();
        List<CommandMatch> newSuggestions = new ArrayList<>();

        boolean isVersionCmd = Constants.isVersionCmd(prevCmd);
        for (CommandMatch suggestion : suggestions) {
            boolean canAdd = false;
            final String sug = suggestion.getText().toLowerCase(Locale.ENGLISH);
            if (isVersionCmd) { // version receives the plugin name
                canAdd = Config.shouldShowPlugin(sug);
            }
            // Everything below is the "/help" command. That receives both <plugin>, <plugin>:<command> & <command>!
            else if (sug.indexOf(':') != -1 && Config.shouldShow(sug)) {
                canAdd = true;
            }
            else if (Config.showCachePlugins.containsKey(sug) && Config.shouldShowPlugin(sug)) {
                canAdd = true;
            }
            else if (Constants.cacheCommand2Plugin.containsKey(sug)
                     && Config.shouldShowPlugin(Constants.cacheCommand2Plugin.get(sug))) {
                canAdd = true;
            }

            if (canAdd) {
                newSuggestions.add(suggestion);
            }
        }

        if (suggestions.size() != newSuggestions.size()) {
            packet.setCommandMatches(newSuggestions);
        }
    }
}
