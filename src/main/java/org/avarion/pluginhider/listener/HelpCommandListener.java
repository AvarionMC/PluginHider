package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.avarion.pluginhider.util.Config;
import org.avarion.pluginhider.util.Constants;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

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
        event.setCancelled(true);

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
    }

    private void handleHelpCommand(@NotNull Player player, final @NotNull String command) {
        if (!shouldShow(command.toLowerCase(Locale.ENGLISH))) {
            player.sendMessage(ChatColor.RED + "No help for " + command);
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Showing help for " + command); // TODO
    }

    private void showHelpPage(@NotNull Player player, int helpPage) {
        helpPage = Math.min(helpPage, 1);
        player.sendMessage(ChatColor.GREEN + "Showing helppage " + helpPage); // TODO
    }

    private boolean shouldShow(@NotNull String suggestion) {
        // The "/help" command receives both <plugin>, <plugin>:<command> & <command>!
        int idx = suggestion.indexOf(':');
        if (idx != -1) {
            return Config.shouldShow(suggestion.substring(0, idx));
        }

        if (Config.showCachePlugins.containsKey(suggestion)) {
            return Config.shouldShowPlugin(suggestion);
        }

        if (Constants.cacheCommand2Plugin.containsKey(suggestion)) {
            return Config.shouldShowPlugin(Constants.cacheCommand2Plugin.get(suggestion));
        }

        return true;
    }

    boolean isCorrectCommand(@NotNull String text) {
        return Constants.isHelpCmd(text);
    }
}
