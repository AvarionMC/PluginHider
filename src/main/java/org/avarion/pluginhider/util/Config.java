package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Config {
    private final Map<String, Boolean> showCache = new LinkedHashMap<>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 1000;
        }
    };
    private final File configLocation;
    private final Settings settings = new Settings();
    private boolean hideAll = false;

    public Config() {
        configLocation = new File(PluginHider.inst.getDataFolder(), "config.yml");
        reload();
    }

    public void reload() {
        try {
            settings.load(configLocation);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }

        showCache.clear();

        hideAll = settings.hidePlugins.contains("*");
    }

    public boolean isOpLike(@Nullable Player player) {
        if (player == null) return false;

        UUID id = player.getUniqueId();
        if (settings.whitelist.contains(id)) return true;
        if (settings.blacklist.contains(id)) return false;

        return settings.operatorCanSeeEverything && player.isOp();
    }

    public boolean getShouldAllowConolOnTabComplete() {
        return settings.shouldAllowColonTabcompletion;
    }

    public boolean shouldShow(@Nullable final String pluginName) {
        return showCache.computeIfAbsent(
                pluginName, k -> {
                    if (k == null) {
                        return false;
                    }

                    String cleanedName = k.toLowerCase().trim().split("\\s+", 2)[0];
                    if (settings.showPlugins.contains(cleanedName)) {
                        return true; // explicitly shown
                    }
                    if (settings.hidePlugins.contains(cleanedName)) {
                        return false; // explicitly hidden
                    }
                    if (hideAll && ("minecraft".equals(cleanedName) || "bukkit".equals(cleanedName))) {
                        return true; // It wasn't explicitly mentioned inside "hide plugins" section: so default MC commands are visible.
                    }

                    return !hideAll; // if all plugins are hidden;
                }
        );
    }
}
