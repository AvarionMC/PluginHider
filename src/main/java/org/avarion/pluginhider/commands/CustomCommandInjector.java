package org.avarion.pluginhider.commands;

import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CustomCommandInjector {
    private static @Nullable CommandMap getCommandMap() {
        try {
            final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void removeCommand(
            @NotNull Map<String, Command> knownCommands,
            @NotNull String namespace,
            @NotNull String cmd,
            @NotNull List<String> removed
    ) {
        String fullNamespace = (namespace.isEmpty() ? "" : namespace + ":") + cmd;
        if (knownCommands.remove(fullNamespace) != null) {
            removed.add(namespace);
        }
    }

    private static @NotNull List<String> unregisterCommand(CommandMap commandMap, @Nullable Command cmd) {
        List<String> removed = new ArrayList<>();
        if (cmd == null) {
            return removed;
        }

        @SuppressWarnings("unchecked") Map<String, Command>
                knownCommands
                = (Map<String, Command>) ReflectionUtils.getFieldValue(commandMap, "knownCommands", Map.class);

        removeCommand(knownCommands, "", cmd.getName(), removed);
        for (String server : Constants.servers) {
            removeCommand(knownCommands, server, cmd.getName(), removed);
        }

        // Also try to remove by aliases if any
        for (String alias : cmd.getAliases()) {
            knownCommands.remove(alias);
            for (String server : Constants.servers) {
                knownCommands.remove(server + ":" + alias);
            }
        }

        return removed;
    }

    public static void replaceCommand(BukkitCommand newCommand) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            PluginHider.logger.error("Could not access CommandMap - cannot register custom '"
                                     + newCommand.getName()
                                     + "' command");
            return;
        }

        // Remove the existing command
        Command existingCommand = commandMap.getCommand(newCommand.getName());
        List<String> removed = unregisterCommand(commandMap, existingCommand);

        for (String namespace : removed) {
            commandMap.register(namespace, newCommand);
        }

        PluginHider.logger.info("Custom '" + newCommand.getName() + "' command has been registered!");
    }
}
