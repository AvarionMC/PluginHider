package org.avarion.pluginhider.custom_commands;

import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.avarion.pluginhider.util.Caches;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VersionCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Mirrors {@code io.papermc.paper.command.PaperVersionCommand} so that, for the plugins a player is
 * allowed to see, {@code /version} is byte-identical to an unmodified server — and a hidden plugin
 * looks exactly as if it were never installed.
 */
public class CustomVersionCommand extends VersionCommand implements MyCustomCommand {
    private static final Component NOT_RUNNING = Component.text()
            .append(Component.text("This server is not running any plugin by that name."))
            .appendNewline()
            .append(Component.text("Use /plugins to get a list of plugins.")
                             .clickEvent(ClickEvent.suggestCommand("/plugins")))
            .build();

    private static final JoinConfiguration PLAYER_JOIN_CONFIGURATION = JoinConfiguration.separators(
            Component.text(", ", NamedTextColor.WHITE),
            Component.text(", and ", NamedTextColor.WHITE)
    );

    public CustomVersionCommand() {
        super("version");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        Caches.load();

        if (!testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            // No plugin data is exposed here, so hand the server-version output straight to the real
            // command — that keeps the async update check, the copy-to-clipboard hover and every
            // byte identical to an unmodified server.
            return super.execute(sender, currentAlias, args);
        }

        final String pluginName = String.join(" ", args).toLowerCase(Locale.ROOT);
        final Plugin plugin = findVisiblePlugin(sender, pluginName);
        if (plugin != null) {
            sendPluginInfo(plugin, sender);
        }
        else {
            sender.sendMessage(NOT_RUNNING);
        }
        return true;
    }

    private @Nullable Plugin findVisiblePlugin(@NotNull CommandSender sender, @NotNull String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            for (Plugin candidate : Bukkit.getPluginManager().getPlugins()) {
                if (candidate.getName().toLowerCase(Locale.ROOT).contains(pluginName)) {
                    plugin = candidate;
                    break;
                }
            }
        }

        // A hidden plugin must be indistinguishable from one that isn't installed.
        if (plugin != null && !isAllowedPlugin(sender, plugin.getName())) {
            return null;
        }
        return plugin;
    }

    private void sendPluginInfo(@NotNull Plugin plugin, @NotNull CommandSender sender) {
        final PluginMeta meta = plugin.getPluginMeta();

        final TextComponent.Builder builder = Component.text()
                .append(Component.text(meta.getName()))
                .append(Component.text(" version "))
                .append(Component.text(meta.getVersion(), NamedTextColor.GREEN)
                                 .hoverEvent(Component.translatable("chat.copy.click"))
                                 .clickEvent(ClickEvent.copyToClipboard(meta.getVersion())));

        if (meta.getDescription() != null) {
            builder.appendNewline().append(Component.text(meta.getDescription()));
        }

        if (meta.getWebsite() != null) {
            builder.appendNewline()
                   .append(Component.text("Website: ")
                                    .append(Component.text(meta.getWebsite(), NamedTextColor.GREEN)
                                                     .clickEvent(ClickEvent.openUrl(meta.getWebsite()))));
        }

        if (!meta.getAuthors().isEmpty()) {
            final String prefix = meta.getAuthors().size() == 1 ? "Author: " : "Authors: ";
            builder.appendNewline().append(Component.text(prefix).append(formatNameList(meta.getAuthors())));
        }

        if (!meta.getContributors().isEmpty()) {
            builder.appendNewline()
                   .append(Component.text("Contributors: ").append(formatNameList(meta.getContributors())));
        }

        sender.sendMessage(builder.build());
    }

    private static @NotNull Component formatNameList(@NotNull List<String> names) {
        return Component.join(PLAYER_JOIN_CONFIGURATION, names.stream().map(Component::text).toList())
                        .color(NamedTextColor.GREEN);
    }

    @Override
    public @NotNull List<String> tabComplete(
            @NotNull CommandSender sender,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        Caches.load();

        if (args.length != 1) {
            return List.of();
        }

        final List<String> completions = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (StringUtil.startsWithIgnoreCase(plugin.getName(), args[0]) && isAllowedPlugin(
                    sender,
                    plugin.getName()
            )) {
                completions.add(plugin.getName());
            }
        }
        return completions;
    }
}
