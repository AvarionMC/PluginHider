package org.avarion.pluginhider.commands;

import org.avarion.pluginhider.util.Caches;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.PluginsCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class CustomPluginsCommand extends PluginsCommand implements MyCustomCommand {
    public CustomPluginsCommand() {
        super("plugins");
    }

    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        Caches.load();

        if (!this.testPermission(sender)) {
            return true;
        }
        else {
            sender.sendMessage("Plugins " + this.getPluginList(sender));
            return true;
        }
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Caches.load();

        return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                     .map(Plugin::getName)
                     .filter(name -> isAllowedPlugin(sender, name))
                     .collect(Collectors.toList());
    }

    @NotNull
    private String getPluginList(@NotNull CommandSender sender) {
        StringBuilder pluginList = new StringBuilder();
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();

        for (Plugin plugin : plugins) {
            if (!isAllowedPlugin(sender, plugin.getName())) {
                continue;
            }

            if (pluginList.length() > 0) {
                pluginList.append(ChatColor.WHITE);
                pluginList.append(", ");
            }

            pluginList.append(plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED);
            pluginList.append(plugin.getDescription().getName());
            if (!plugin.getDescription().getProvides().isEmpty()) {
                pluginList.append(" (").append(String.join(", ", plugin.getDescription().getProvides())).append(")");
            }
        }

        return "(" + plugins.length + "): " + pluginList;
    }
}
