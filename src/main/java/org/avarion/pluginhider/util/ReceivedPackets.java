package org.avarion.pluginhider.util;

import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.exceptions.SectionExpectedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReceivedPackets {
    private static final Pattern amountPluginsPattern = Pattern.compile("\\(\\s*(\\d+)\\s*\\)\\s*:\\s*$");
    private final HashMap<net.kyori.adventure.text.TextComponent, List<String>> pluginsSeen = new LinkedHashMap<>();
    public Integer amountOfPlugins = null;
    private net.kyori.adventure.text.TextComponent currentSection = null;
    private int lineCounter = 0;

    private @NotNull String asString(final @NotNull net.kyori.adventure.text.TextComponent line) {
        StringBuilder sb = new StringBuilder();
        asString(line, sb);
        return sb.toString();
    }

    private void asString(final @NotNull net.kyori.adventure.text.TextComponent line, final @NotNull StringBuilder sb) {
        sb.append(line.content());
        for (var component : line.children()) {
            assert component instanceof net.kyori.adventure.text.TextComponent;
            asString((net.kyori.adventure.text.TextComponent) component, sb);
        }
    }

    public void addSystemChatLine(final net.kyori.adventure.text.TextComponent tc) {
        lineCounter++;

        final String line = asString(tc);
        if (lineCounter == 1) {
            interpretAmountOfPlugins(line);
        }
        else if (line.endsWith(":")) {
            currentSection = tc;
        }
        else if (currentSection == null) {
            throw new SectionExpectedException();
        }
        else {
            interpretPluginLine(line);
        }
    }

    private void interpretPluginLine(@NotNull String fullText) {
        fullText = fullText.trim();
        if (fullText.startsWith("- ")) {
            fullText = fullText.substring(2);
        }
        String[] parts = fullText.split(",");

        var arr = pluginsSeen.computeIfAbsent(currentSection, s -> new ArrayList<>());
        arr.addAll(Arrays.stream(parts).map(String::trim).filter(p -> !p.isEmpty()).toList());
    }

    private void interpretAmountOfPlugins(final String line) {
        Matcher match = amountPluginsPattern.matcher(line);
        if (match.find()) {
            amountOfPlugins = Integer.parseInt(match.group(1));
        }
    }

    private <K, V> int size(@NotNull Map<K, List<V>> map) {
        return map.values().stream().mapToInt(List::size).sum();
    }

    public boolean isFinished() {
        return amountOfPlugins != null && size(pluginsSeen) >= amountOfPlugins;
    }

    // region <Message sending>
    public void sendModifiedMessage(@NotNull Player player) {
        final Player.Spigot pl = player.spigot();

        HashMap<net.kyori.adventure.text.TextComponent, List<String>> filtered = new HashMap<>();
        for (var entry : pluginsSeen.entrySet()) {
            List<String> filteredList = entry.getValue().stream().filter(PluginHider.config::shouldShow).toList();

            if (!filteredList.isEmpty()) {
                filtered.put(entry.getKey(), filteredList);
            }
        }

        int total = size(filtered);
        TextComponent msg = createFirstLine(total);
        pl.sendMessage(msg);

        if (total <= 0) {
            return;
        }

        for (var entry : filtered.entrySet()) {
            var header = createSecondLine(entry.getKey(), entry.getValue().size());
            pl.sendMessage(header);

            msg = createThirdLine(entry.getValue());
            pl.sendMessage(msg);
        }
    }

    private @NotNull TextComponent createFirstLine(int amount) {
        TextComponent msg = new TextComponent("Server Plugins (" + amount + "):");
        msg.setColor(ChatColor.WHITE);
        return msg;
    }

    private @NotNull BaseComponent createSecondLine(final @NotNull net.kyori.adventure.text.TextComponent section, int count) {
        String line = asString(section);
        Matcher match = amountPluginsPattern.matcher(line);

        net.kyori.adventure.text.Component msg = section;
        if (match.find()) {
            // Change plugin count
            msg = section.replaceText(builder -> builder.match(amountPluginsPattern).replacement("(" + count + "):"));
        }

        String json = GsonComponentSerializer.gson().serialize(msg);
        return ComponentSerializer.parse(json)[0];
    }

    private @NotNull TextComponent createThirdLine(final @NotNull List<String> plugins) {
        TextComponent msg = new TextComponent("- ");
        msg.setColor(ChatColor.DARK_GRAY);

        boolean isFirst = true;
        for (var pluginName : plugins) {
            if (!isFirst) {
                TextComponent tmp = new TextComponent(", ");
                msg.addExtra(tmp);
            }

            Plugin pl = Bukkit.getPluginManager().getPlugin(pluginName);

            TextComponent tmp = new TextComponent(pluginName);
            tmp.setColor(pl != null && pl.isEnabled() ? ChatColor.GREEN : ChatColor.RED);
            tmp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/version " + pluginName));
            msg.addExtra(tmp);

            isFirst = false;
        }

        return msg;
    }
    // endregion
}
