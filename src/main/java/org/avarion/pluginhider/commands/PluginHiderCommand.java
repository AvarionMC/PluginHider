package org.avarion.pluginhider.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Caches;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@FunctionalInterface
interface CommandAction {
    void execute(CommandSender sender);
}


public class PluginHiderCommand implements TabExecutor {
    private final Map<String, Action> functions = new LinkedHashMap<>();

    public PluginHiderCommand() {
        functions.put("reload", new Action("reload the configuration", this::reloadConfiguration));
        functions.put("help", new Action("shows this help", this::showHelp));
        functions.put("dump", new Action("dump info", this::dump));
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender commandSender,
            @NotNull Command command,
            @NotNull String s,
            String[] args
    ) {
        if (!commandSender.isOp()) {
            commandSender.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }

        if (args.length == 0) {
            showHelp(commandSender);
            return true;
        }

        if (!functions.containsKey(args[0])) {
            showHelp(commandSender, false, "Unknown subcommand: " + args[0]);
            return true;
        }

        functions.get(args[0]).action.execute(commandSender);
        return true;
    }

    private void reloadConfiguration(CommandSender player) {
        Bukkit.getScheduler().runTaskAsynchronously(
                PluginHider.inst, () -> {
                    PluginHider.inst.reloadSettings();
                    player.sendMessage("Reloaded the configuration.");
                    PluginHider.logger.info("Reloaded the configuration.");
                }
        );
    }

    private void showHelp(CommandSender commandSender) {
        showHelp(commandSender, true, "");
    }

    private void showHelp(@NotNull CommandSender commandSender, boolean showVersion, String showError) {
        var send_to = commandSender.spigot();
        if (showVersion) {
            send_to.sendMessage(new TextComponent("Version: " + PluginHider.inst.currentVersion));
        }

        if (!showError.isEmpty()) {
            send_to.sendMessage(new TextComponent(showError));
        }

        send_to.sendMessage(new TextComponent("Available actions:"));
        for (var entry : functions.entrySet()) {
            String doThis = "/pluginhider " + entry.getKey();

            TextComponent txt = new TextComponent(doThis);
            txt.setColor(ChatColor.GREEN);
            txt.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, doThis));

            TextComponent extra = new TextComponent(": " + entry.getValue().description);
            extra.setColor(ChatColor.WHITE);
            txt.addExtra(extra);

            commandSender.spigot().sendMessage(txt);
        }
    }

    public void dump(CommandSender commandSender) {
        Caches.dump();
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender commandSender,
            @NotNull Command command,
            @NotNull String s,
            String @NotNull [] args
    ) {
        if (args.length != 1) {
            return null;
        }

        if (!commandSender.isOp()) {
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

    static class Action {
        final String description;
        final CommandAction action;

        public Action(String description, CommandAction action) {
            this.description = description;
            this.action = action;
        }
    }
}
