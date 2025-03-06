package org.avarion.pluginhider.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.avarion.pluginhider.util.Caches;
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

    private boolean shouldShow(@NotNull String argument) {
        // The "/help" command receives both <plugin>, <plugin>:<command> & <command>!
        if ("aliases".equals(argument)) {
            return true; // Special case...
        }

        boolean isCommand = Caches.isInCommandCache(argument);
        if (isCommand && Caches.shouldShowCommand(argument)) {
            return true;
        }

        boolean isPlugin = Caches.isInPluginCache(argument);
        return isPlugin && Caches.shouldShowPlugin(argument);
    }

    boolean isCorrectCommand(@NotNull String text) {
        return Caches.isHelpCmd(text);
    }
}
