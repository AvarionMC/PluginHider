package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.avarion.pluginhider.util.Config;
import org.avarion.pluginhider.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


public class HelpCommandListener extends PacketListenerAbstract implements Listener {
    private final CommonVersionHelpClass common;

    public HelpCommandListener() {
        common = new CommonVersionHelpClass(this::isCorrectCommand, this::shouldShow, this::handleCommand);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        common.onPacketReceive(event);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        common.onPacketSend(event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        common.onCommand(event);
    }

    void handleCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().trim().split("\\s+", 2);
        if (args.length > 1) {
            try {
                int number = Integer.parseInt(args[1]);
                showHelpPage(event.getPlayer(), number);
            }
            catch (NumberFormatException e) {
                handleHelpCommand(event.getPlayer(), args[1]);
            }
            return;
        }

        showHelpPage(event.getPlayer(), 1);

        // This is when you ask /version <plugin>
        if (!Config.shouldShow(args[1])) {
            Player player = event.getPlayer();
            // default message from spigot when a plugin isn't found.
            player.sendMessage("This server is not running any plugin by that name.");
            player.sendMessage("Use /plugins to get a list of plugins.");

            event.setCancelled(true);
        }
    }

    private void handleHelpCommand(@NotNull Player player, final String command) {
        Config.shouldShow(command);
    }

    private void showHelpPage(@NotNull Player player, int helpPage) {

    }

    private boolean shouldShow(@Nullable String suggestion) {
        if (suggestion == null) {
            return false;
        }

        String sug = suggestion.trim().toLowerCase(Locale.ENGLISH);

        // The "/help" command receives both <plugin>, <plugin>:<command> & <command>!
        if (sug.indexOf(':') != -1 && Config.shouldShow(sug)) {
            return true;
        }

        if (Config.showCachePlugins.containsKey(sug) && Config.shouldShowPlugin(sug)) {
            return true;
        }

        return Constants.cacheCommand2Plugin.containsKey(sug)
               && Config.shouldShowPlugin(Constants.cacheCommand2Plugin.get(sug));
    }

    boolean isCorrectCommand(@Nullable String text) {
        return Constants.isHelpCmd(text);
    }
}
