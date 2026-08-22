package org.avarion.pluginhider;

import org.avarion.yaml.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
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
            List of player UUIDs that always see every plugin and command, in addition to the server
            console. This is the ONLY way to see the full list — operators are deliberately treated as
            normal players, so op status never reveals hidden plugins.
            Format: List of player UUIDs""")
    @YamlKey("whitelisted_uuids")
    public Set<UUID> whitelist = Set.of();

    public boolean hideAll = true;

    private @NotNull Set<String> makeLowerCase(@Nullable Set<String> entries) {
        if (entries == null) {
            return Set.of();
        }

        return entries.stream()
                      .filter(Objects::nonNull)
                      .map(p -> p.toLowerCase(Locale.ENGLISH))
                      .collect(Collectors.toUnmodifiableSet());
    }

    private <T> @NotNull Set<T> cleanUp(@Nullable Set<T> entries) {
        if (entries == null) {
            return Set.of();
        }

        return entries.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    public <T extends YamlFileInterface> void load() throws IOException {
        File config = new File(PluginHider.inst.getDataFolder(), "config.yml");

        super.load(config);

        hidePlugins = makeLowerCase(hidePlugins);
        showPlugins = makeLowerCase(showPlugins);
        whitelist = cleanUp(whitelist);

        hideAll = hidePlugins.contains("*");

        super.save(config);
    }

    /**
     * Whether this player may see every plugin and command. Only explicitly whitelisted players
     * qualify — being an operator grants nothing here, so op status can't be used to enumerate
     * hidden plugins. (The server console is handled separately and always sees everything.)
     */
    public boolean canSeeEverything(@Nullable Player player) {
        return player != null && whitelist.contains(player.getUniqueId());
    }
}
