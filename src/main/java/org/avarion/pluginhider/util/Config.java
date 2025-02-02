package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Config {
    private final List<String> hiddenPlugins = new ArrayList<>();
    private final List<String> shownPlugins = new ArrayList<>();
    private final Set<UUID> hideFromUUIDs = new HashSet<>();
    private final Map<String, Boolean> showCache = new LinkedHashMap<>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 1000;
        }
    };
    private boolean shouldAllowConolOnTabComplete = false;
    private boolean operatorCanSeeEverything = false;
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
        showCache.clear();

        update(hiddenPlugins, "hide_plugins", List.of());
        update(shownPlugins, "show_plugins", Collections.singletonList("*"));

        List<String> tmp = new ArrayList<>();
        update(tmp, "uuids_to_explicitly_disallow", List.of());

        hideFromUUIDs.clear();
        tmp.forEach(s -> hideFromUUIDs.add(UUID.fromString(s)));

        hideAll = hiddenPlugins.contains("*");
        shouldAllowConolOnTabComplete = config.getBoolean("should_allow_colon_tabcompletion", false);
        operatorCanSeeEverything = config.getBoolean("operator_can_see_everything", false);
    }

    public boolean isOpLike(@Nullable Player player) {
        return player != null && player.isOp() && operatorCanSeeEverything && !hideFromUUIDs.contains(player.getUniqueId());
    }

    public boolean getShouldAllowConolOnTabComplete() {
        return shouldAllowConolOnTabComplete;
    }

    public boolean shouldShow(@Nullable final String pluginName) {
        return showCache.computeIfAbsent(
                pluginName, k -> {
                    if (k == null) {
                        return false;
                    }

                    String cleaned_name = k.toLowerCase().trim().split("\\s+", 2)[0];
                    if (shownPlugins.contains(cleaned_name)) {
                        return true; // explicitly shown
                    }
                    if (hiddenPlugins.contains(cleaned_name)) {
                        return false; // explicitly hidden
                    }
                    if (hideAll && ("minecraft".equals(cleaned_name) || "bukkit".equals(cleaned_name))) {
                        return true; // It wasn't explicitly mentioned inside "hide plugins" section: so default MC commands are visible.
                    }

                    return !hideAll; // if all plugins are hidden;
                }
        );
    }
}
