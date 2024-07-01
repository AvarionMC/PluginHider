package org.avarion.plugin_hider.util;

import org.avarion.plugin_hider.PluginHider;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class Config {
    private final PluginHider plugin;
    private FileConfiguration config;

    public Config(PluginHider plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public List<String> getHidePlugins() {
        return config.getStringList("hide_plugins");
    }

    public List<String> getShowPlugins() {
        return config.getStringList("show_plugins");
    }
}
