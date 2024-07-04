package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Config {
    private final PluginHider plugin;
    private final List<String> hiddenPlugins = new ArrayList<>();
    private final List<String> shownPlugins = new ArrayList<>();
    public boolean hideHiddenPluginCommands = true;
    private FileConfiguration config;
    private boolean hideAll = false;

    public Config(PluginHider plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();
        reload();
    }

    private void update(List<String> target, String source, List<String> def) {
        target.clear();

        if (!config.contains(source)) {
            target.addAll(def);
        }
        else {
            config.getStringList(source).stream().map(String::toLowerCase).forEach(target::add);
        }
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        update(hiddenPlugins, "hide_plugins", Collections.emptyList());
        update(shownPlugins, "show_plugins", Collections.singletonList("*"));

        hideAll = hiddenPlugins.contains("*");
        hideHiddenPluginCommands = config.getBoolean("hide_hidden_plugin_commands", true);
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
