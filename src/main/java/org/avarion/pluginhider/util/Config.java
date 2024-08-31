package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Config {
    private final List<String> hiddenPlugins = new ArrayList<>();
    private final List<String> shownPlugins = new ArrayList<>();
    public boolean hideHiddenPluginCommands = true;
    public boolean shouldAllowConolOnTabComplete = false;
    private FileConfiguration config;
    private boolean hideAll = false;

    public Config() {
        reload();
    }

    private void update(@NotNull List<String> target, String source, List<String> def) {
        target.clear();

        if (!config.contains(source)) {
            target.addAll(def);
        }
        else {
            config.getStringList(source).stream().map(String::toLowerCase).forEach(target::add);
        }
    }

    public void reload() {
        PluginHider.inst.saveDefaultConfig();
        PluginHider.inst.reloadConfig();
        config = PluginHider.inst.getConfig();

        update(hiddenPlugins, "hide_plugins", Collections.emptyList());
        update(shownPlugins, "show_plugins", Collections.singletonList("*"));

        hideAll = hiddenPlugins.contains("*");
        hideHiddenPluginCommands = config.getBoolean("hide_hidden_plugin_commands", true);
        shouldAllowConolOnTabComplete = config.getBoolean("should_allow_colon_tabcompletion", false);
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
