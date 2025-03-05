package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class Config {
    public final static Map<String, Boolean> showCache = new LRUCache<>(1_000);
    public final static Map<String, Boolean> showCachePlugins = new LRUCache<>(1_000);
    private final File configLocation;
    private final static Settings settings = new Settings();
    private static boolean hideAll = false;

    public Config() {
        configLocation = new File(PluginHider.inst.getDataFolder(), "config.yml");
        reload();
    }

    public void reload() {
        try {
            settings.load(configLocation);
        }
        catch (IOException e) {
            Bukkit.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }

        showCache.clear();

        hideAll = settings.hidePlugins.contains("*");
    }

    public boolean isOpLike(@Nullable Player player) {
        if (player == null) {
            return false;
        }

        UUID id = player.getUniqueId();
        if (settings.whitelist.contains(id)) {
            return true;
        }
        if (settings.blacklist.contains(id)) {
            return false;
        }

        return settings.operatorCanSeeEverything && player.isOp();
    }

    public boolean getShouldAllowConolOnTabComplete() {
        return settings.shouldAllowColonTabcompletion;
    }

    @Contract("null -> new")
    public static String @NotNull [] splitPluginName(@Nullable final String pluginName) {
        if (pluginName == null || pluginName.isBlank()) {
            return new String[]{
                    null, null
            };
        }

        String cleaned = pluginName.toLowerCase().trim().split("\\s+", 2)[0];

        String[] parts = cleaned.split(":", 2);
        if (parts.length == 2) {
            final String plugin = parts[0];
            final String cmd = parts[1];
            Constants.cacheCommand2Plugin.putIfAbsent(cmd, plugin);
            Constants.cachePlugin2Commands.computeIfAbsent(parts[0], p -> new HashSet<>()).add(cmd);
            return parts;
        }

        return new String[]{null, parts[0]};
    }

    public static boolean shouldShowPlugin(@Nullable final String pluginName) {
        return showCachePlugins.computeIfAbsent(
                pluginName, k -> {
                    if (k == null) {
                        return false;
                    }

                    if (settings.showPlugins.contains(k)) {
                        return true; // explicitly shown -- remember that `servers` are automagically added in Settings::load!
                    }
                    if (settings.hidePlugins.contains(k)) {
                        return false; // explicitly hidden
                    }

                    return !hideAll; // if all plugins are hidden;
                }
        );
    }

    public static boolean shouldShow(@Nullable final String pluginName) {
        return showCache.computeIfAbsent(
                pluginName, k -> {
                    String[] parts = splitPluginName(pluginName);
                    if (parts[1] == null) { // Unknown command
                        return false;
                    }

                    String trueName = parts[0];
                    if (trueName != null) {
                        if (!PluginHider.config.getShouldAllowConolOnTabComplete()) {
                            return false;
                        }
                    }
                    else {
                        trueName = Constants.cacheCommand2Plugin.getOrDefault(parts[1], null);
                        if (trueName == null) {
                            PluginHider.logger.info("shouldShow didn't knew what '" + pluginName + "' is?");
                            return true;
                        }
                    }

                    return shouldShowPlugin(trueName);
                }
        );
    }
}
