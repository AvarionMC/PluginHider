package org.avarion.pluginhider.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
    private static final Component INFO_ICON_SERVER_PLUGIN = Component.text("ℹ ", TextColor.color(52, 159, 218));
    private static final Component PLUGIN_TICK = Component.text("- ", NamedTextColor.DARK_GRAY);
    private static final Component PLUGIN_TICK_EMPTY = Component.text(" ");

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
        if (Constants.isPaperServer()) {
            return executePaperPlugins(sender, currentAlias, args);
        }
        else {
            return executeSpigotPlugins(sender, currentAlias, args);
        }
    }

    /**
     * Paper server implementation using reflection
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

            // Generate and send output components
            int sizePaperPlugins = paperPlugins.size();
            int sizeSpigotPlugins = spigotPlugins.size();
            int sizePlugins = sizePaperPlugins + sizeSpigotPlugins;
            boolean hasAllPluginTypes = (sizePaperPlugins > 0 && sizeSpigotPlugins > 0);

            Component infoMessage = Component.text()
                                             .append(INFO_ICON_SERVER_PLUGIN)
                                             .append(Component.text(
                                                     String.format("Server Plugins (%s):", sizePlugins),
                                                     NamedTextColor.WHITE
                                             ))
                                             .build();

            sender.sendMessage(infoMessage);

            if (!paperPlugins.isEmpty()) {
                sender.sendMessage(header("Paper Plugins", 0x0288D1, sizePaperPlugins, hasAllPluginTypes));

                for (Component component : formatProviders(paperPlugins)) {
                    sender.sendMessage(component);
                }
            }

            if (!spigotPlugins.isEmpty()) {
                sender.sendMessage(header("Bukkit Plugins", 0xED8106, sizeSpigotPlugins, hasAllPluginTypes));

                for (Component component : formatProviders(spigotPlugins)) {
                    sender.sendMessage(component);
                }
            }

            return true;
        }
        catch (Exception e) {
            // Fallback to regular plugin list
            return executeSpigotPlugins(sender, currentAlias, args);
        }
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
     * Format providers similar to PaperPluginsCommand
     */
    private @NotNull List<Component> formatProviders(TreeMap<String, Object> plugins) {
        try {
            List<Component> components = new ArrayList<>(plugins.size());

            for (Map.Entry<String, Object> entry : plugins.entrySet()) {
                String pluginName = entry.getKey();
                Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

                TextColor color = plugin != null && plugin.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED;

                components.add(Component.text(pluginName, color));
            }

            // Split list into sublists for better presentation (like original)
            boolean isFirst = true;
            List<Component> formattedLists = new ArrayList<>();

            for (List<Component> sublist : partitionList(components, 10)) {
                Component component = Component.space();
                if (isFirst) {
                    component = component.append(PLUGIN_TICK);
                    isFirst = false;
                }
                else {
                    component = PLUGIN_TICK_EMPTY;
                }

                formattedLists.add(component.append(Component.join(JoinConfiguration.commas(true), sublist)));
            }

            return formattedLists;
        }
        catch (Exception e) {
            // Simple fallback
            List<Component> fallback = new ArrayList<>();
            fallback.add(Component.text(String.join(", ", plugins.keySet())));
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

    /**
     * Create a header component
     */
    private @NotNull Component header(String header, int color, int count, boolean showSize) {
        TextComponent.Builder componentHeader = Component.text()
                                                         .color(TextColor.color(color))
                                                         .append(Component.text(header));

        if (showSize) {
            componentHeader.appendSpace().append(Component.text("(" + count + ")"));
        }

        return componentHeader.append(Component.text(":")).build();
    }
}