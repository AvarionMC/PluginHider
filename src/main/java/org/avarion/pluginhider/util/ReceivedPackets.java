package org.avarion.pluginhider.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceivedPackets {
    private final static Pattern amountPluginsPattern = Pattern.compile("\\(\\s*(\\d+)\\s*\\):");

    private static final int CACHE_SIZE = 1000;
    private static final Map<String, String> cache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public final ZonedDateTime created = ZonedDateTime.now(ZoneOffset.UTC);
    private final int maxSecondsDelay;
    private final List<String> pluginsSeen = new ArrayList<>();
    private final Config cfg;

    public Integer amountOfPlugins = null;

    public ReceivedPackets(Config cfg, int maxSecondsDelay) {
        this.cfg = cfg;
        this.maxSecondsDelay = maxSecondsDelay;
    }

    public boolean isStale() {
        return created.isBefore(ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(maxSecondsDelay));
    }

    public void addSystemChatLine(final String line) {
        if (amountOfPlugins == null) {
            interpretAmountOfPlugins(line);
        }
        else {
            interpretPluginLine(line);
        }
    }

    private void interpretPluginLine(final String line) {
        if (amountOfPlugins == null) {
            return;
        }

        String fullText = cache.computeIfAbsent(line, this::getTextFromJson);
        if (fullText.endsWith(":")) {
            return;
        }

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

    // region <JSON parsing>
    private void getTextFromNode(@NotNull final StringBuilder tmp, @NotNull final JsonElement current) {
        if (current.isJsonObject()) {
            for (var el : current.getAsJsonObject().entrySet()) {
                final var val = el.getValue();

                switch (el.getKey()) {
                    case "text" -> tmp.append(val.getAsString());
                    case "extra" -> getTextFromNode(tmp, val);
                }
            }
        }
        else if (current.isJsonArray()) {
            for (var el : current.getAsJsonArray()) {
                getTextFromNode(tmp, el);
            }
        }
        else {
            tmp.append(current.getAsString());
        }
    }

    private @NotNull String getTextFromJson(final String line) {
        final Gson json = new Gson();
        final JsonObject root = json.fromJson(line, JsonObject.class);
        final StringBuilder tmp = new StringBuilder();
        getTextFromNode(tmp, root);
        return tmp.toString().trim();
    }
    // endregion

    // region <Message sending>
    public void sendModifiedMessage(@NotNull Player player) {
        final Player.Spigot pl = player.spigot();

        List<String> newPlugins = pluginsSeen.stream().filter(cfg::shouldShow).toList();

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
