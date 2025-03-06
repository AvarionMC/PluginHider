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
        lenient = Leniency.LENIENT, header = "Plugin Visibility Configuration Guide\n"
                                             + "=====================================\n"
                                             + "\n"
                                             + "This guide explains how to control which plugins are visible to players.\n"
                                             + "\n"
                                             + "Basic Concepts:\n"
                                             + "- hide_plugins: List of plugins to hide\n"
                                             + "- show_plugins: List of plugins to explicitly show\n"
                                             + "- '*' in hide_plugins: Hides all plugins not listed in show_plugins\n"
                                             + "- show_plugins takes priority over hide_plugins\n"
                                             + "\n"
                                             + "Example Scenarios\n"
                                             + "----------------\n"
                                             + "Let's say you have these plugins installed:\n"
                                             + "- pluginA\n"
                                             + "- pluginB\n"
                                             + "- pluginC\n"
                                             + "- pluginD\n"
                                             + "- pluginE\n"
                                             + "\n"
                                             + "1. Hide a Single Plugin\n"
                                             + "   To hide only pluginB:\n"
                                             + "   ```\n"
                                             + "   hide_plugins:\n"
                                             + "     - pluginB\n"
                                             + "   ```\n"
                                             + "   Result: All plugins except pluginB will be visible\n"
                                             + "\n"
                                             + "2. Show Only One Plugin\n"
                                             + "   To show only pluginB:\n"
                                             + "   ```\n"
                                             + "   hide_plugins:\n"
                                             + "     - '*'\n"
                                             + "   show_plugins:\n"
                                             + "     - pluginB\n"
                                             + "   ```\n"
                                             + "   Result: Only pluginB will be visible\n"
                                             + "\n"
                                             + "3. Hide Multiple Specific Plugins\n"
                                             + "   To hide pluginB and pluginD:\n"
                                             + "   ```\n"
                                             + "   hide_plugins:\n"
                                             + "     - pluginB\n"
                                             + "     - pluginD\n"
                                             + "   ```\n"
                                             + "   Result: All plugins except pluginB and pluginD will be visible\n"
                                             + "\n"
                                             + "4. Show Only Selected Plugins\n"
                                             + "   To show only pluginA and pluginC:\n"
                                             + "   ```\n"
                                             + "   hide_plugins:\n"
                                             + "     - '*'\n"
                                             + "   show_plugins:\n"
                                             + "     - pluginA\n"
                                             + "     - pluginC\n"
                                             + "   ```\n"
                                             + "   Result: Only pluginA and pluginC will be visible\n"
                                             + "\n"
                                             + "5. Hide All Plugins\n"
                                             + "   To hide all plugins except Minecraft/Bukkit commands:\n"
                                             + "   ```\n"
                                             + "   hide_plugins:\n"
                                             + "     - '*'\n"
                                             + "   ```\n"
                                             + "   Result: Only default Minecraft/Bukkit commands will be visible\n"
                                             + "\n"
                                             + "6. Hide Everything Including Minecraft/Bukkit\n"
                                             + "   To hide absolutely everything:\n"
                                             + "   ```\n"
                                             + "   hide_plugins:\n"
                                             + "     - '*'\n"
                                             + "     - minecraft\n"
                                             + "     - bukkit\n"
                                             + "   ```\n"
                                             + "   Result: No commands will be visible at all\n"
                                             + "\n"
                                             + "Important Notes:\n"
                                             + "- Using '*' in hide_plugins will hide all plugin commands BUT KEEP default Minecraft/Bukkit commands visible\n"
                                             + "- To hide Minecraft commands, you must explicitly add 'minecraft' to hide_plugins\n"
                                             + "- To hide Bukkit commands, you must explicitly add 'bukkit' to hide_plugins\n"
                                             + "- If a plugin appears in both hide_plugins and show_plugins, it WILL be shown\n"
                                             + "- For complete plugin visibility, consider uninstalling PluginHider instead"
)
public class Settings extends YamlFileInterface {
    @YamlComment("List of plugins to hide from players. Use '*' to hide all plugins.")
    @YamlKey("hide_plugins")
    public Set<String> hidePlugins = Set.of("PluginHider", "ProtocolLib", "packetevents");

    @YamlComment("List of plugins to show, even if they would otherwise be hidden. Takes priority over hide_plugins.")
    @YamlKey("show_plugins")
    public Set<String> showPlugins = Set.of("*");

    @YamlComment(
            "Controls whether plugin commands can be tab-completed with the plugin name prefix.\n"
            + "Example: When true, commands can be completed as 'pluginname:command'\n"
            + "When false, only the command name without the plugin prefix will be shown."
    )
    @YamlKey("should_allow_colon_tabcompletion")
    public boolean shouldAllowColonTabcompletion = false;

    @YamlComment(
            "When true, server operators (ops) can see all plugin commands regardless of hide/show settings.\n"
            + "Set to false if you want hiding rules to apply to operators as well."
    )
    @YamlKey("operator_can_see_everything")
    public boolean operatorCanSeeEverything = false;

    @YamlComment(
            "List of operator UUIDs that should see all commands, even when he is not an operator or operator_can_see_everything is false.\n"
            + "Format: List of player UUIDs"
    )
    @YamlKey("whitelisted_uuids")
    public Set<UUID> whitelist = Set.of();

    @YamlComment(
            "List of operator UUIDs that should always be treated as normal users, even when he's an operator and operator_can_see_everything is true.\n"
            + "Format: List of player UUIDs"
    )
    @YamlKey("blacklisted_uuids")
    public Set<UUID> blacklist = Set.of();

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
        blacklist = cleanUp(blacklist);

        hideAll = hidePlugins.contains("*");

        super.save(config);
    }

    public boolean isOpLike(@Nullable Player player) {
        if (player == null) {
            return false;
        }

        UUID id = player.getUniqueId();
        if (whitelist.contains(id)) {
            return true;
        }
        if (blacklist.contains(id)) {
            return false;
        }

        return operatorCanSeeEverything && player.isOp();
    }
}