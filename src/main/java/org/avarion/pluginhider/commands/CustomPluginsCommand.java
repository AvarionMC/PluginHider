package org.avarion.pluginhider.commands;

import org.avarion.pluginhider.util.Caches;
import org.avarion.pluginhider.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.PluginsCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


public class CustomPluginsCommand extends PluginsCommand implements MyCustomCommand {
    public CustomPluginsCommand() {
        super("plugins");
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Caches.load();

        return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                     .map(Plugin::getName)
                     .filter(name -> isAllowedPlugin(sender, name))
                     .collect(Collectors.toList());
    }

    /**
     * Executes the plugins command with filtering based on permissions
     */
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        Caches.load();

        if (Constants.isPaperServer()) {
            return executePaperPlugins(sender, currentAlias, args);
        }
        else {
            return executeSpigotPlugins(sender, currentAlias, args);
        }
    }

    /**
     * Paper server implementation using reflection but with only Bukkit API output
     */
    private boolean executePaperPlugins(CommandSender sender, String currentAlias, String[] args) {
        try {
            // Get the necessary classes via reflection
            Class<?> entrypointClass = Class.forName("io.papermc.paper.plugin.entrypoint.Entrypoint");
            Class<?> launchEntryPointHandlerClass = Class.forName(
                    "io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler");
            Class<?> paperPluginProviderClass = Class.forName(
                    "io.papermc.paper.plugin.provider.type.paper.PaperPluginParent$PaperServerPluginProvider");
            Class<?> spigotPluginProviderClass = Class.forName(
                    "io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider");

            // Get the INSTANCE field
            Field instanceField = launchEntryPointHandlerClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object handlerInstance = instanceField.get(null);

            // Get the PLUGIN field
            Field pluginField = entrypointClass.getDeclaredField("PLUGIN");
            pluginField.setAccessible(true);
            Object pluginEntrypoint = pluginField.get(null);

            // Call get() method
            Method getMethod = launchEntryPointHandlerClass.getDeclaredMethod("get", entrypointClass);
            Object result = getMethod.invoke(handlerInstance, pluginEntrypoint);

            // Get registered providers
            Method getRegisteredProvidersMethod = result.getClass().getDeclaredMethod("getRegisteredProviders");
            List<?> providers = (List<?>) getRegisteredProvidersMethod.invoke(result);

            // Sort providers into maps
            TreeMap<String, Object> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            TreeMap<String, Object> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (Object provider : providers) {
                // Get plugin meta
                Method getMetaMethod = provider.getClass().getDeclaredMethod("getMeta");
                Object meta = getMetaMethod.invoke(provider);

                // Get plugin name
                Method getNameMethod = meta.getClass().getDeclaredMethod("getName");
                String name = (String) getNameMethod.invoke(meta);

                Method getDisplayNameMethod = meta.getClass().getDeclaredMethod("getDisplayName");
                String displayName = (String) getDisplayNameMethod.invoke(meta);

                // Check if plugin is allowed to be shown
                if (!isAllowedPlugin(sender, name)) {
                    continue;
                }

                // Categorize by provider type
                if (paperPluginProviderClass.isInstance(provider)) {
                    paperPlugins.put(displayName, provider);
                }
                else if (spigotPluginProviderClass.isInstance(provider)) {
                    spigotPlugins.put(displayName, provider);
                }
            }

            // Generate and send output using Bukkit/Spigot API
            int sizePaperPlugins = paperPlugins.size();
            int sizeSpigotPlugins = spigotPlugins.size();
            int sizePlugins = sizePaperPlugins + sizeSpigotPlugins;
            boolean hasAllPluginTypes = (sizePaperPlugins > 0 && sizeSpigotPlugins > 0);

            // Send header message
            String infoMessage = ChatColor.WHITE + "Server Plugins (" + sizePlugins + "):";
            sender.sendMessage(infoMessage);

            // Paper plugins section
            if (!paperPlugins.isEmpty()) {
                String paperHeader = ChatColor.BLUE + "Paper Plugins" + (
                        hasAllPluginTypes ? " (" + sizePaperPlugins + ")" : ""
                ) + ":";

                sender.sendMessage(paperHeader);

                // Format and send paper plugins
                List<String> formattedPaperPlugins = formatPluginsAsText(paperPlugins);
                for (String line : formattedPaperPlugins) {
                    sender.sendMessage(line);
                }
            }

            // Spigot plugins section
            if (!spigotPlugins.isEmpty()) {
                String spigotHeader = ChatColor.GOLD + "Bukkit Plugins" + (
                        hasAllPluginTypes ? " (" + sizeSpigotPlugins + ")" : ""
                ) + ":";

                sender.sendMessage(spigotHeader);

                // Format and send spigot plugins
                List<String> formattedSpigotPlugins = formatPluginsAsText(spigotPlugins);
                for (String line : formattedSpigotPlugins) {
                    sender.sendMessage(line);
                }
            }

            return true;
        }
        catch (Exception e) {
            int a = 1;
        }

        return executeSpigotPlugins(sender, currentAlias, args);
    }

    /**
     * Spigot fallback implementation
     */
    private boolean executeSpigotPlugins(CommandSender sender, String currentAlias, String[] args) {
        List<String> plugins = new ArrayList<>();

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (isAllowedPlugin(sender, plugin.getName())) {
                String displayName = plugin.isEnabled()
                                     ? ChatColor.GREEN + plugin.getName()
                                     : ChatColor.RED + plugin.getName();
                plugins.add(displayName);
            }
        }

        plugins.sort(String.CASE_INSENSITIVE_ORDER);

        sender.sendMessage("Plugins (" + plugins.size() + "): " + String.join(", ", plugins));
        return true;
    }

    /**
     * Format providers for text output
     */
    private @NotNull List<String> formatPluginsAsText(TreeMap<String, Object> plugins) {
        try {
            List<String> pluginNames = new ArrayList<>();

            for (Map.Entry<String, Object> entry : plugins.entrySet()) {
                String pluginName = entry.getKey();
                Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

                String formattedName = (plugin != null && plugin.isEnabled())
                                       ? ChatColor.GREEN + pluginName
                                       : ChatColor.RED + pluginName;

                pluginNames.add(formattedName);
            }

            pluginNames.sort(String.CASE_INSENSITIVE_ORDER);

            // Split into groups of 10 plugins
            List<String> result = new ArrayList<>();
            List<List<String>> partitions = partitionList(pluginNames, 10);

            boolean isFirst = true;
            for (List<String> partition : partitions) {
                StringBuilder line = new StringBuilder();

                if (isFirst) {
                    line.append(ChatColor.DARK_GRAY).append("- ");
                    isFirst = false;
                }
                else {
                    line.append("  ");
                }

                line.append(String.join(ChatColor.WHITE + ", ", partition));
                result.add(line.toString());
            }

            return result;
        }
        catch (Exception e) {
            // Simple fallback
            List<String> fallback = new ArrayList<>();
            fallback.add(String.join(", ", plugins.keySet()));
            return fallback;
        }
    }

    /**
     * Helper to split a list into sublists of specified size
     */
    private <T> @NotNull List<List<T>> partitionList(@NotNull List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return parts;
    }
}