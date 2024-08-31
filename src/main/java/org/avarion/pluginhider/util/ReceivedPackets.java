package org.avarion.pluginhider.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.avarion.pluginhider.PluginHider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReceivedPackets {
    private final static Pattern amountPluginsPattern = Pattern.compile("\\(\\s*(\\d+)\\s*\\):");
    private final List<String> pluginsSeen = new ArrayList<>();

    public Integer amountOfPlugins = null;
    private int lineCounter = 0;

    public void addSystemChatLine(final String line) {
        lineCounter++;

        if (lineCounter==1) {
            interpretAmountOfPlugins(line);
        } else if (lineCounter==2) {
            assert line.endsWith(":");
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

        pluginsSeen.addAll(Arrays.stream(parts).map(String::trim).filter(p -> !p.isEmpty()).toList());
    }

    private void interpretAmountOfPlugins(final String line) {
        Matcher match = amountPluginsPattern.matcher(line);
        if (match.find()) {
            amountOfPlugins = Integer.parseInt(match.group(1));
        }
    }

    public boolean isFinished() {
        return amountOfPlugins != null && pluginsSeen.size() >= amountOfPlugins;
    }

    // region <Message sending>
    public void sendModifiedMessage(@NotNull Player player) {
        final Player.Spigot pl = player.spigot();

        List<String> newPlugins = pluginsSeen.stream().filter(PluginHider.config::shouldShow).toList();

        TextComponent msg = createFirstLine(newPlugins.size());
        pl.sendMessage(msg);

        if (newPlugins.isEmpty()) {
            return;
        }

        msg = createSecondLine();
        pl.sendMessage(msg);

        msg = createThirdLine(newPlugins);
        pl.sendMessage(msg);
    }

    private @NotNull TextComponent createFirstLine(int amount) {
        TextComponent msg = new TextComponent("Server Plugins (" + amount + "):");
        msg.setColor(ChatColor.WHITE);
        return msg;
    }

    private @NotNull TextComponent createSecondLine() {
        TextComponent msg = new TextComponent("Bukkit Plugins:");
        msg.setColor(net.md_5.bungee.api.ChatColor.of("#ed8106"));

        return msg;
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
            tmp.setColor(pl!=null && pl.isEnabled() ? ChatColor.GREEN : ChatColor.RED);
            tmp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/version " + pluginName));
            msg.addExtra(tmp);

            isFirst = false;
        }

        return msg;
    }
    // endregion
}
