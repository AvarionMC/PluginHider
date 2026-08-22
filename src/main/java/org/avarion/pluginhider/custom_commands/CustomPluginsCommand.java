package org.avarion.pluginhider.custom_commands;

import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Caches;
import org.avarion.pluginhider.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.PluginsCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;


/**
 * Mirrors {@code io.papermc.paper.command.PaperPluginsCommand} exactly (info icon with hover/click,
 * the Paper/Bukkit section colours, the legacy {@code *} star, the per-name {@code /version} click
 * and the 10-per-line layout) for the plugins a player may see — so hidden plugins are
 * indistinguishable from ones that were never installed. Plugin providers are still enumerated by
 * reflection because those types are server-internal, not part of the API.
 */
public class CustomPluginsCommand extends PluginsCommand implements MyCustomCommand {
    private static final TextColor INFO_COLOR = TextColor.color(52, 159, 218);

    private static final Component SERVER_PLUGIN_INFO = Component.text("ℹ What is a server plugin?", INFO_COLOR)
            .append(asPlainComponents("""
                    Server plugins can add new behavior to your server!
                    You can find new plugins on Paper's plugin repository, Hangar.

                    https://hangar.papermc.io/
                    """));

    private static final Component LEGACY_PLUGIN_INFO = Component.text("ℹ What is a legacy plugin?", INFO_COLOR)
            .append(asPlainComponents("""
                    A legacy plugin is a plugin that was made on
                    very old unsupported versions of the game.

                    It is encouraged that you replace this plugin,
                    as they might not work in the future and may cause
                    performance issues.
                    """));

    private static final Component LEGACY_PLUGIN_STAR = Component.text('*', TextColor.color(255, 212, 42))
                                                                 .hoverEvent(LEGACY_PLUGIN_INFO);
    private static final Component INFO_ICON_START = Component.text("ℹ ", INFO_COLOR);
    private static final Component PLUGIN_TICK = Component.text("- ", NamedTextColor.DARK_GRAY);
    private static final Component PLUGIN_TICK_EMPTY = Component.text(" ");
    private static final Component INFO_ICON_SERVER_PLUGIN = INFO_ICON_START.hoverEvent(SERVER_PLUGIN_INFO)
                                                                            .clickEvent(ClickEvent.openUrl(
                                                                                    "https://docs.papermc.io/paper/adding-plugins"));

    public CustomPluginsCommand() {
        super("plugins");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        Caches.load();

        try {
            sendPaperStyle(sender);
        }
        catch (Exception e) {
            // The Paper reflection path is the norm on the supported server; if it ever breaks, make
            // the (non-identical) degradation visible instead of silently changing the format.
            PluginHider.logger.error("Failed to render /plugins in Paper style; using plain fallback", e);
            sendPlainFallback(sender);
        }
        return true;
    }

    private void sendPaperStyle(@NotNull CommandSender sender) {
        final Class<?> entrypointClass = ReflectionUtils.getClass("io.papermc.paper.plugin.entrypoint.Entrypoint");
        final Class<?> handlerClass = ReflectionUtils.getClass(
                "io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler");
        final Class<?> paperProviderClass = ReflectionUtils.getClass(
                "io.papermc.paper.plugin.provider.type.paper.PaperPluginParent$PaperServerPluginProvider");
        final Class<?> spigotProviderClass = ReflectionUtils.getClass(
                "io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider");
        final Class<?> craftMagicNumbers = ReflectionUtils.getClass("org.bukkit.craftbukkit.util.CraftMagicNumbers");

        final Object handler = ReflectionUtils.getStaticFieldValue(handlerClass, "INSTANCE", Object.class);
        final Object pluginEntrypoint = ReflectionUtils.getStaticFieldValue(entrypointClass, "PLUGIN", Object.class);
        final Object registered = ReflectionUtils.invoke(handler, "get", pluginEntrypoint);
        final Collection<?> providers = (Collection<?>) ReflectionUtils.invoke(registered, "getRegisteredProviders");
        if (providers == null) {
            throw new IllegalStateException("No registered plugin providers");
        }

        // Keyed by display name, exactly like PaperPluginsCommand.
        final TreeMap<String, Component> paperPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final TreeMap<String, Component> spigotPlugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Object provider : providers) {
            // getMeta() is server-internal, but its result implements the API's PluginMeta — cast to
            // it so we can call getName()/getDisplayName() directly (getDisplayName() is a default
            // interface method that reflection over the concrete class can't resolve).
            final PluginMeta meta = (PluginMeta) ReflectionUtils.invoke(provider, "getMeta");
            final String name = meta.getName();

            if (!isAllowedPlugin(sender, name)) {
                continue; // hidden -> as if it were never installed
            }

            final String displayName = meta.getDisplayName();
            final boolean spigot = spigotProviderClass != null && spigotProviderClass.isInstance(provider);
            final boolean legacy = spigot && isLegacy(craftMagicNumbers, meta);
            final Component formatted = formatProvider(name, legacy, colorFor(name));

            if (spigot) {
                spigotPlugins.put(displayName, formatted);
            }
            else if (paperProviderClass != null && paperProviderClass.isInstance(provider)) {
                paperPlugins.put(displayName, formatted);
            }
        }

        final int sizePaper = paperPlugins.size();
        final int sizeSpigot = spigotPlugins.size();
        final boolean hasBoth = sizePaper > 0 && sizeSpigot > 0;

        sender.sendMessage(Component.text()
                                    .append(INFO_ICON_SERVER_PLUGIN)
                                    .append(Component.text(
                                            "Server Plugins (%s):".formatted(sizePaper + sizeSpigot),
                                            NamedTextColor.WHITE
                                    ))
                                    .build());

        if (!paperPlugins.isEmpty()) {
            sender.sendMessage(header("Paper Plugins", 0x0288D1, sizePaper, hasBoth));
            formatLines(paperPlugins.values()).forEach(sender::sendMessage);
        }

        if (!spigotPlugins.isEmpty()) {
            sender.sendMessage(header("Bukkit Plugins", 0xED8106, sizeSpigot, hasBoth));
            formatLines(spigotPlugins.values()).forEach(sender::sendMessage);
        }
    }

    private static boolean isLegacy(Class<?> craftMagicNumbers, PluginMeta meta) {
        if (craftMagicNumbers == null) {
            return false;
        }
        try {
            // Look the method up by its PluginMeta parameter type — meta's concrete runtime class is
            // PluginDescriptionFile, so a lookup keyed on meta.getClass() would miss it.
            final Method isLegacy = ReflectionUtils.getStaticMethod(craftMagicNumbers, "isLegacy", PluginMeta.class);
            return (Boolean) isLegacy.invoke(null, meta);
        }
        catch (Exception e) {
            return false;
        }
    }

    private static @NotNull NamedTextColor colorFor(@NotNull String name) {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return (plugin != null && plugin.isEnabled()) ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    private static @NotNull Component formatProvider(@NotNull String name, boolean legacy, @NotNull NamedTextColor color) {
        final TextComponent.Builder builder = Component.text();
        if (legacy) {
            builder.append(LEGACY_PLUGIN_STAR);
        }
        builder.append(Component.text(name, color).clickEvent(ClickEvent.runCommand("/version " + name)));
        return builder.build();
    }

    private static @NotNull List<Component> formatLines(@NotNull Collection<Component> plugins) {
        final List<Component> all = new ArrayList<>(plugins);
        final List<Component> lines = new ArrayList<>();

        boolean first = true;
        for (int i = 0; i < all.size(); i += 10) {
            final List<Component> chunk = all.subList(i, Math.min(all.size(), i + 10));
            final Component prefix = first ? Component.space().append(PLUGIN_TICK) : PLUGIN_TICK_EMPTY;
            first = false;
            lines.add(prefix.append(Component.join(JoinConfiguration.commas(true), chunk)));
        }
        return lines;
    }

    private static @NotNull Component header(@NotNull String header, int color, int count, boolean showSize) {
        final TextComponent.Builder builder = Component.text()
                                                       .color(TextColor.color(color))
                                                       .append(Component.text(header));
        if (showSize) {
            builder.appendSpace().append(Component.text("(" + count + ")"));
        }
        return builder.append(Component.text(":")).build();
    }

    private static @NotNull Component asPlainComponents(@NotNull String strings) {
        final TextComponent.Builder builder = Component.text();
        for (String string : strings.split("\n")) {
            builder.append(Component.newline());
            builder.append(Component.text(string, NamedTextColor.WHITE));
        }
        return builder.build();
    }

    private void sendPlainFallback(@NotNull CommandSender sender) {
        final List<String> names = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (isAllowedPlugin(sender, plugin.getName())) {
                names.add(plugin.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        sender.sendMessage("Plugins (" + names.size() + "): " + String.join(", ", names));
    }

    @Override
    public @NotNull List<String> tabComplete(
            @NotNull CommandSender sender,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        Caches.load();

        final List<String> result = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (isAllowedPlugin(sender, plugin.getName())) {
                result.add(plugin.getName());
            }
        }
        return result;
    }
}
