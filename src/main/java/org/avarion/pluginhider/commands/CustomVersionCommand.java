package org.avarion.pluginhider.commands;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.Validate;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Caches;
import org.avarion.pluginhider.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VersionCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class CustomVersionCommand extends VersionCommand implements MyCustomCommand {
    final Method describeToSender;

    public CustomVersionCommand() {
        super("version");
        describeToSender = ReflectionUtils.getMethod(this, "describeToSender", Plugin.class, CommandSender.class);
    }

    private void callToSender(Plugin plugin, CommandSender sender) {
        try {
            describeToSender.invoke(plugin, sender);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            PluginHider.logger.error("Failed to call 'describeToSender'!");
        }
    }

    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        Caches.load();

        if (this.testPermission(sender)) {
            if (args.length == 0) {
                sender.sendMessage("This server is running our own implementation. (Implementing API version "
                                   + Bukkit.getBukkitVersion()
                                   + ")");
            }
            else {
                StringBuilder name = new StringBuilder();

                for (String arg : args) {
                    if (name.length() > 0) {
                        name.append(' ');
                    }

                    name.append(arg);
                }

                String pluginName = name.toString();
                Plugin exactPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
                if (exactPlugin != null && isAllowedPlugin(sender, pluginName)) {
                    callToSender(exactPlugin, sender);
                    return true;
                }

                boolean found = false;
                pluginName = pluginName.toLowerCase(Locale.ENGLISH);

                for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                    if (plugin.getName().toLowerCase(Locale.ENGLISH).contains(pluginName) && isAllowedPlugin(
                            sender,
                            plugin.getName()
                    )) {
                        callToSender(plugin, sender);
                        found = true;
                    }
                }

                if (!found) {
                    sender.sendMessage("This server is not running any plugin by that name.");
                    sender.sendMessage("Use /plugins to get a list of plugins.");
                }
            }
        }
        return true;
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Caches.load();

        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String toComplete = args[0].toLowerCase(Locale.ENGLISH);

            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (StringUtil.startsWithIgnoreCase(plugin.getName(), toComplete) && isAllowedPlugin(
                        sender,
                        plugin.getName()
                )) {
                    completions.add(plugin.getName());
                }
            }

            return completions;
        }
        else {
            return ImmutableList.of();
        }
    }
}
