package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.Settings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;


public class Config {
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
            PluginHider.logger.error("Failed to load config.yml: " + e.getMessage());
        }

        Caches.showCache.clear();

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

    public boolean shouldShowPluginCleaned(@NotNull final String cleaned) {
        if (settings.showPlugins.contains(cleaned)) {
            return true; // explicitly shown -- remember that `servers` are automagically added in Settings::load!
        }
        if (settings.hidePlugins.contains(cleaned)) {
            return false; // explicitly hidden
        }
        if (Constants.servers.contains(cleaned)) {
            return true;
        }

        return !hideAll; // if all plugins are hidden;
    }

    public boolean shouldShowPlugin(@Nullable final String pluginName) {
        if (pluginName == null) {
            return false;
        }

        String cleaned = pluginName.toLowerCase(Locale.ENGLISH).trim();
        return shouldShowPluginCleaned(cleaned);
    }

    public boolean shouldShow(@Nullable final String pluginName) {
        String[] parts = Caches.splitPluginName(pluginName);
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
            trueName = Caches.cacheCommand2Plugin.getOrDefault(parts[1], null);
            if (trueName == null) {
                PluginHider.logger.info("shouldShow didn't knew what '" + pluginName + "' is?");
                return true;
            }
        }

        return shouldShowPluginCleaned(trueName);
    }
}
