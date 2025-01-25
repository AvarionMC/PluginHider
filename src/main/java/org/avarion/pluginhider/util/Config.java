package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Config {
    private final List<String> hiddenPlugins = new ArrayList<>();
    private final List<String> shownPlugins = new ArrayList<>();
    public boolean shouldAllowConolOnTabComplete = false;
    public boolean operatorCanSeeEverything = false;
    private FileConfiguration config;
    private boolean hideAll = false;
    private final Map<String, Boolean> showCache = new LinkedHashMap<>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 1000;
        }
    };

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
        showCache.clear();

        update(hiddenPlugins, "hide_plugins", Collections.emptyList());
        update(shownPlugins, "show_plugins", Collections.singletonList("*"));

        hideAll = hiddenPlugins.contains("*");
        shouldAllowConolOnTabComplete = config.getBoolean("should_allow_colon_tabcompletion", false);
        operatorCanSeeEverything = config.getBoolean("operator_can_see_everything", false);
    }

    public boolean getOperatorCanSeeEverything() {
        return operatorCanSeeEverything;
    }

    public boolean shouldShow(@Nullable final String pluginName) {
        return showCache.computeIfAbsent(pluginName, k -> {
            if (k==null) return false;

            String[] parts = k.toLowerCase().trim().split("\\s+", 2);
            if (shownPlugins.contains(parts[0])) {
                return true; // explicitly shown
            }
            if (hiddenPlugins.contains(parts[0])) {
                return false; // explicitly hidden
            }
            return !hideAll; // if all plugins are hidden;
        });
    }
}
