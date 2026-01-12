package org.avarion.pluginhider.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class PluginGrouper {

    public Map<String, List<Plugin>> groupPlugins() {
        Map<String, List<Plugin>> groupedPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        groupedPlugins.put("paper", new ArrayList<>());
        groupedPlugins.put("bukkit", new ArrayList<>());

        if (Constants.isPaperServer()) {
            // Paper server implementation
            try {
                Class<?>
                        paperPluginProviderClass
                        = Class.forName("io.papermc.paper.plugin.provider.PaperPluginProvider");
                Class<?> entrypointHandlerClass = Class.forName(
                        "io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler");

                // Get INSTANCE field
                Field instanceField = entrypointHandlerClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                Object handlerInstance = instanceField.get(null);

                // Get Entrypoint.PLUGIN value
                Class<?> entrypointClass = Class.forName("io.papermc.paper.plugin.entrypoint.Entrypoint");
                Field pluginField = entrypointClass.getDeclaredField("PLUGIN");
                Object pluginEntrypoint = pluginField.get(null);

                // Call get() method
                Method getMethod = entrypointHandlerClass.getDeclaredMethod("get", entrypointClass);
                Object result = getMethod.invoke(handlerInstance, pluginEntrypoint);

                // Get registered providers
                Method getRegisteredProvidersMethod = result.getClass().getDeclaredMethod("getRegisteredProviders");
                Object providers = getRegisteredProvidersMethod.invoke(result);

                // Process each provider
                for (Object provider : (Iterable<?>) providers) {
                    String providerClassName = provider.getClass().getName();
                    Plugin plugin = getPluginFromProvider(provider);

                    if (plugin != null) {
                        if (providerClassName.contains("PaperServerPluginProvider") || providerClassName.contains(
                                "PaperPluginProvider")) {
                            groupedPlugins.get("paper").add(plugin);
                        }
                        else if (providerClassName.contains("SpigotPluginProvider")) {
                            groupedPlugins.get("bukkit").add(plugin);
                        }
                    }
                }

                return groupedPlugins;
            }
            catch (Exception ignored) {
            }
        }

        return fallbackGroupPlugins();
    }

    private @Nullable Plugin getPluginFromProvider(Object provider) {
        try {
            // Try to get plugin instance from provider
            Method getPluginMethod = provider.getClass().getDeclaredMethod("getPlugin");
            return (Plugin) getPluginMethod.invoke(provider);
        }
        catch (Exception e) {
            try {
                // Alternative approach
                Method getMetaMethod = provider.getClass().getDeclaredMethod("getMeta");
                Object meta = getMetaMethod.invoke(provider);
                Method getNameMethod = meta.getClass().getDeclaredMethod("getDisplayName");
                String name = (String) getNameMethod.invoke(meta);

                // Find plugin by name
                for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                    if (plugin.getName().equalsIgnoreCase(name)) {
                        return plugin;
                    }
                }
            }
            catch (Exception ex) {
                // Ignore
            }
            return null;
        }
    }

    private @NotNull Map<String, List<Plugin>> fallbackGroupPlugins() {
        Map<String, List<Plugin>> groupedPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        groupedPlugins.put("plugins", new ArrayList<>());

        // Just put all plugins in one group
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            groupedPlugins.get("plugins").add(plugin);
        }

        return groupedPlugins;
    }
}