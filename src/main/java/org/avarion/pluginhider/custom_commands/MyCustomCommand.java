package org.avarion.pluginhider.custom_commands;

import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Caches;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;


public interface MyCustomCommand {
    /**
     * Checks if a sender has permission to access a specific item
     */
    default <T> boolean isAllowed(CommandSender sender, @NotNull T item, Function<T, Boolean> cacheChecker) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (PluginHider.settings.isOpLike(player)) {
                return true;
            }
        }

        return cacheChecker.apply(item);
    }

    /**
     * Checks if a plugin is allowed for the sender
     */
    default boolean isAllowedPlugin(CommandSender sender, @NotNull String name) {
        return isAllowed(sender, name, Caches::shouldShowPlugin);
    }

    /**
     * Checks if a command is allowed for the sender
     */
    default boolean isAllowedCommand(CommandSender sender, @NotNull String name) {
        return isAllowed(sender, name, Caches::shouldShowCommand);
    }
}
