package org.avarion.plugin_hider.util;

import org.avarion.plugin_hider.PluginHider;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private final PluginHider plugin;
    private final List<String> hiddenPlugins = new ArrayList<>();
    private final List<String> shownPlugins = new ArrayList<>();
    private FileConfiguration config;
    private boolean hideAll = false;

    public Config(PluginHider plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();
        reload();
    }

    private void update(List<String> target, String source) {
        config.getStringList(source).stream().map(String::toLowerCase).forEach(target::add);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        update(hiddenPlugins, "hide_plugins");
        update(shownPlugins, "show_plugins");

        hideAll = hiddenPlugins.contains("*");
    }

    public boolean shouldShow(String pluginName) {
        pluginName = pluginName.toLowerCase();
        if (shownPlugins.contains(pluginName)) {
            return true; // explicitly shown
        }
        if (hiddenPlugins.contains(pluginName)) {
            return false; // explicitly hidden
        }
        return !hideAll; // if all plugins are hidden
    }
}
