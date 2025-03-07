package org.avarion.pluginhider.custom_commands;

import org.avarion.pluginhider.util.Caches;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.PluginsCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

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
            try {
                return executePaperPlugins(sender, currentAlias, args);
            }
            catch (Exception e) {
                int a = 1;
            }
        }

        return executeSpigotPlugins(sender, currentAlias, args);
    }

    /**
     * Paper server implementation using reflection with ReflectionUtils
     */
    private boolean executePaperPlugins(CommandSender sender, String currentAlias, String[] args) throws Exception {
        // Get the necessary classes via reflection
        Class<?> entrypointClass = ReflectionUtils.getClass("io.papermc.paper.plugin.entrypoint.Entrypoint");
        Class<?> launchEntryPointHandlerClass = ReflectionUtils.getClass(
                "io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler");
        Class<?> paperPluginProviderClass = ReflectionUtils.getClass(
                "io.papermc.paper.plugin.provider.type.paper.PaperPluginParent$PaperServerPluginProvider");
        Class<?> spigotPluginProviderClass = ReflectionUtils.getClass(
                "io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider");

        // Get static INSTANCE field
        Object handlerInstance = ReflectionUtils.getStaticFieldValue(
                launchEntryPointHandlerClass,
                "INSTANCE",
                Object.class
        );

        // Get static PLUGIN field
        Object pluginEntrypoint = ReflectionUtils.getStaticFieldValue(entrypointClass, "PLUGIN", Object.class);

        // Call get() method
        Object result = ReflectionUtils.invoke(handlerInstance, "get", pluginEntrypoint);

        // Call getRegisteredProviders via reflection, using our utility that checks superclasses
        Collection<?> providers = (Collection<?>) ReflectionUtils.invoke(result, "getRegisteredProviders");

        if (providers == null || providers.isEmpty()) {
            return executeSpigotPlugins(sender, currentAlias, args);
        }

        // Sort providers into maps
        TreeMap<String, Object> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        TreeMap<String, Object> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Object provider : providers) {
            try {
                // Get plugin meta
                Object meta = ReflectionUtils.invoke(provider, "getMeta");

                // Get name and display name
                String name = ReflectionUtils.invoke(meta, "getName", String.class);

                // Check if plugin is allowed to be shown
                if (!isAllowedPlugin(sender, name)) {
                    continue;
                }

                // Categorize by provider type
                if (paperPluginProviderClass.isInstance(provider)) {
                    paperPlugins.put(name, provider);
                }
                else if (spigotPluginProviderClass.isInstance(provider)) {
                    spigotPlugins.put(name, provider);
                }
            }
            catch (Exception e) {
                // Skip this provider if we can't get its information
                int a = 1;
            }
        }

        // Generate and send output using Bukkit/Spigot API
        int sizePaperPlugins = paperPlugins.size();
        int sizeSpigotPlugins = spigotPlugins.size();
        int sizePlugins = sizePaperPlugins + sizeSpigotPlugins;
        boolean hasAllPluginTypes = (sizePaperPlugins > 0 && sizeSpigotPlugins > 0);

        // Send header message
        // ChatColor.AQUA + "ℹ " +  <-- not yet available in Paper 1.21.1
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

    private @NotNull List<String> formatPluginsAsText(TreeMap<String, Object> plugins) {
        try {
            List<String> pluginNames = new ArrayList<>();

            // Get needed classes once
            Class<?> spigotProviderClass = ReflectionUtils.getClass(
                    "io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider");
            Class<?> craftMagicNumbersClass = ReflectionUtils.getClass("org.bukkit.craftbukkit.util.CraftMagicNumbers");

            for (Map.Entry<String, Object> entry : plugins.entrySet()) {
                String pluginName = entry.getKey();
                Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
                Object provider = entry.getValue();

                boolean isLegacy = false;
                if (spigotProviderClass != null && craftMagicNumbersClass != null && spigotProviderClass.isInstance(
                        provider)) {
                    try {
                        Object meta = ReflectionUtils.invoke(provider, "getMeta");
                        isLegacy = ReflectionUtils.invokeStatic(
                                craftMagicNumbersClass,
                                "isLegacy",
                                Boolean.class,
                                meta
                        );
                    }
                    catch (Exception e) {
                        // If we can't determine legacy status, assume it's not legacy
                    }
                }

                String formattedName = (plugin != null && plugin.isEnabled())
                                       ? ChatColor.GREEN + pluginName
                                       : ChatColor.RED + pluginName;

                // Add star for legacy plugins
                if (isLegacy) {
                    formattedName = ChatColor.YELLOW + "*" + formattedName;
                }

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