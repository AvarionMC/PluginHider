package org.avarion.pluginhider;

import org.avarion.yaml.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@YamlFile(
        lenient = Leniency.LENIENT,
        header = """
                Plugin Visibility Configuration Guide
                =====================================

                This guide explains how to control which plugins are visible to players.

                Basic Concepts:
                - hide_plugins: List of plugins to hide
                - show_plugins: List of plugins to explicitly show
                - '*' in hide_plugins: Hides all plugins not listed in show_plugins
                - show_plugins takes priority over hide_plugins

                Example Scenarios
                ----------------
                Let's say you have these plugins installed:
                - pluginA
                - pluginB
                - pluginC
                - pluginD
                - pluginE

                1. Hide a Single Plugin
                   To hide only pluginB:
                   ```
                   hide_plugins:
                     - pluginB
                   ```
                   Result: All plugins except pluginB will be visible

                2. Show Only One Plugin
                   To show only pluginB:
                   ```
                   hide_plugins:
                     - '*'
                   show_plugins:
                     - pluginB
                   ```
                   Result: Only pluginB will be visible

                3. Hide Multiple Specific Plugins
                   To hide pluginB and pluginD:
                   ```
                   hide_plugins:
                     - pluginB
                     - pluginD
                   ```
                   Result: All plugins except pluginB and pluginD will be visible

                4. Show Only Selected Plugins
                   To show only pluginA and pluginC:
                   ```
                   hide_plugins:
                     - '*'
                   show_plugins:
                     - pluginA
                     - pluginC
                   ```
                   Result: Only pluginA and pluginC will be visible

                5. Hide All Plugins
                   To hide all plugins except Minecraft/Bukkit commands:
                   ```
                   hide_plugins:
                     - '*'
                   ```
                   Result: Only default Minecraft/Bukkit commands will be visible

                6. Hide Everything Including Minecraft/Bukkit
                   To hide absolutely everything:
                   ```
                   hide_plugins:
                     - '*'
                     - minecraft
                     - bukkit
                   ```
                   Result: No commands will be visible at all

                Important Notes:
                - Using '*' in hide_plugins will hide all plugin commands BUT KEEP default Minecraft/Bukkit commands visible
                - To hide Minecraft commands, you must explicitly add 'minecraft' to hide_plugins
                - To hide Bukkit commands, you must explicitly add 'bukkit' to hide_plugins
                - If a plugin appears in both hide_plugins and show_plugins, it WILL be shown
                - For complete plugin visibility, consider uninstalling PluginHider instead"""
)
public class Settings extends YamlFileInterface {
    @YamlComment("List of plugins to hide from players. Use '*' to hide all plugins.")
    @YamlKey("hide_plugins")
    public Set<String> hidePlugins = Set.of("PluginHider", "ProtocolLib", "packetevents");

    @YamlComment("List of plugins to show, even if they would otherwise be hidden. Takes priority over hide_plugins.")
    @YamlKey("show_plugins")
    public Set<String> showPlugins = Set.of("*");

    @YamlComment("""
            Controls whether plugin commands can be tab-completed with the plugin name prefix.
            Example: When true, commands can be completed as 'pluginname:command'
            When false, only the command name without the plugin prefix will be shown.""")
    @YamlKey("should_allow_colon_tabcompletion")
    public boolean shouldAllowColonTabcompletion = false;

    @YamlComment("""
            Per-player visibility, on top of the global rules above. Maps a player UUID to what that
            player may additionally see — in tab-completion, /plugins, /version and /help.
              - Use "*" to let that player see everything (this replaces the old whitelist).
              - Or list specific plugins to reveal only those (and their commands) to that player.
            Operators get no special treatment; only the server console and the UUIDs listed here
            ever see more than a normal player. Example:
              player_plugins:
                00000000-0000-0000-0000-000000000000: "*"
                11111111-1111-1111-1111-111111111111:
                  - Essentials
                  - WorldEdit""")
    @YamlKey("player_plugins")
    public Map<UUID, Set<String>> playerPlugins = Map.of();

    public boolean hideAll = true;

    // Normalized form of playerPlugins: parsed UUID -> lowercased plugin names (or "*").
    // Package-private so same-package tests can seed it without a full config load.
    volatile Map<UUID, Set<String>> grants = Map.of();

    private @NotNull Set<String> makeLowerCase(@Nullable Set<String> entries) {
        if (entries == null) {
            return Set.of();
        }

        return entries.stream()
                      .filter(Objects::nonNull)
                      .map(p -> p.toLowerCase(Locale.ENGLISH))
                      .collect(Collectors.toUnmodifiableSet());
    }

    public <T extends YamlFileInterface> void load() throws IOException {
        File config = new File(PluginHider.inst.getDataFolder(), "config.yml");

        super.load(config);

        hidePlugins = makeLowerCase(hidePlugins);
        showPlugins = makeLowerCase(showPlugins);
        grants = normalizeGrants(playerPlugins);

        hideAll = hidePlugins.contains("*");

        super.save(config);
    }

    // yaml parses the UUID keys for us; we only lowercase the plugin-name values so grant lookups
    // are case-insensitive (and keep the "*" wildcard verbatim).
    private @NotNull Map<UUID, Set<String>> normalizeGrants(@Nullable Map<UUID, Set<String>> raw) {
        if (raw == null) {
            return Map.of();
        }

        Map<UUID, Set<String>> result = new HashMap<>();
        for (Map.Entry<UUID, Set<String>> entry : raw.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey(), normalizePlugins(entry.getValue()));
            }
        }
        return Map.copyOf(result);
    }

    // Keep "*" verbatim (it means "everything"); lowercase real plugin names to match lookups.
    private static @NotNull Set<String> normalizePlugins(@NotNull Set<String> plugins) {
        return plugins.stream()
                      .filter(Objects::nonNull)
                      .map(p -> p.equals("*") ? "*" : p.toLowerCase(Locale.ENGLISH))
                      .collect(Collectors.toUnmodifiableSet());
    }

    /** The plugins this player was explicitly granted (lowercased, or "*" for everything); never null. */
    public @NotNull Set<String> grantsFor(@Nullable UUID id) {
        return id == null ? Set.of() : grants.getOrDefault(id, Set.of());
    }

    /**
     * Whether this player may see every plugin and command (a {@code "*"} grant). Being an operator
     * grants nothing here, so op status can't be used to enumerate hidden plugins. (The server
     * console is handled separately and always sees everything.)
     */
    public boolean canSeeEverything(@Nullable Player player) {
        return player != null && grantsFor(player.getUniqueId()).contains("*");
    }
}
