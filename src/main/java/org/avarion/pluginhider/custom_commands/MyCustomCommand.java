package org.avarion.pluginhider.custom_commands;

import org.avarion.pluginhider.util.Caches;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public interface MyCustomCommand {
    /**
     * Whether a plugin is visible to this sender. The console always sees everything; a player sees
     * the globally-visible plugins plus whatever their per-UUID grant adds; any other sender (e.g.
     * RCON) gets the plain global view.
     */
    default boolean isAllowedPlugin(CommandSender sender, @NotNull String name) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }
        if (sender instanceof Player player) {
            return Caches.shouldShowPlugin(player.getUniqueId(), name);
        }
        return Caches.shouldShowPlugin(name);
    }

    /**
     * Whether a command is visible to this sender — the command analogue of
     * {@link #isAllowedPlugin(CommandSender, String)}.
     */
    default boolean isAllowedCommand(CommandSender sender, @NotNull String name) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }
        if (sender instanceof Player player) {
            return Caches.shouldShowCommand(player.getUniqueId(), name);
        }
        return Caches.shouldShowCommand(name);
    }
}
