package org.avarion.plugin_hider;

import org.avarion.plugin_hider.util.SendCmd;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
interface CommandAction {
    void execute(CommandSender sender);
}

record Action(PluginHider plugin, String description, CommandAction action) {
}

public class PluginHiderCommand implements CommandExecutor, TabCompleter {
    private final PluginHider plugin;
    private final Map<String, Action> functions = new LinkedHashMap<>();

    public PluginHiderCommand(PluginHider plugin) {
        this.plugin = plugin;

        functions.put("reload", new Action(plugin, "reload the configuration", this::reloadConfiguration));
        functions.put("help", new Action(plugin, "shows this help", this::showHelp));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if ( ! commandSender.isOp() ) {
            return false;
        }

        if (args.length == 0){
            showHelp(commandSender);
            return true;
        }

        if (!functions.containsKey(args[0])) {
            showHelp(commandSender, false, "Unknown subcommand: " + args[0]);
            return true;
        }

        functions.get(args[0]).action().execute(commandSender);
        return true;
    }

    private void reloadConfiguration(CommandSender ignored) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.config.reload();
            plugin.logger.info("Reloaded the configuration.");
        });
    }

    private void showHelp(CommandSender commandSender) {
        showHelp(commandSender, true, "");
    }

    private void showHelp(CommandSender commandSender, boolean showVersion, String showError) {
        if (showVersion) {
            SendCmd.sendMessage(commandSender, "Version: " + plugin.currentVersion);
        }

        if (showError.isEmpty()) {
            SendCmd.sendMessage(commandSender, showError);
        }

        SendCmd.sendMessage(commandSender, "Available actions:");
        for (var entry : functions.entrySet()) {
            SendCmd.sendMessage(commandSender, "/pluginhide " + entry.getKey() + ": " + entry.getValue().description());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if ( args.length != 1 ) {
            return null;
        }

        if ( ! commandSender.isOp() ) {
            return null;
        }

        String currentArg = args[0].toLowerCase();
        if (currentArg.isEmpty()) {  // No arg yet
            return new ArrayList<>(functions.keySet());
        }

        List<String> tmp = new ArrayList<>();
        for (var entry : functions.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.startsWith(currentArg)) {
                tmp.add(key);
            }
        }

        return tmp;
    }
}
