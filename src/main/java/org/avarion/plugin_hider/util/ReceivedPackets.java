package org.avarion.plugin_hider.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatMessage;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceivedPackets {
    private final static Pattern amountPluginsPattern = Pattern.compile("\\(\\s*(\\d+)\\s*\\):");

    public final ZonedDateTime created = ZonedDateTime.now(ZoneOffset.UTC);
    private final int maxSecondsDelay;
    private final ArrayList<String> receivedMessages = new ArrayList<>();
    private final ArrayList<String> pluginsSeen = new ArrayList<>();

    public Integer amountOfPlugins = null;
    public Integer maxLineLength = 0;

    public ReceivedPackets(int maxSecondsDelay) {
        this.maxSecondsDelay = maxSecondsDelay;

        buildCurrentPluginList();
    }

    private void buildCurrentPluginList() {
        pluginsSeen.clear();
        for (var plugin : Bukkit.getPluginManager().getPlugins()) {
            String tmp = plugin.getDescription().getName();
            if (!plugin.getDescription().getProvides().isEmpty()) {
                tmp += " (" + String.join(", ", plugin.getDescription().getProvides()) + ")";
            }
            pluginsSeen.add(tmp);
        }

        TextComponent chat = new TextComponent("txt");
        Bukkit.getServer().getPlayer("ABC").sendRawMessage();
        chat.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/version ..."));

        ChatMessage msg = new ChatMessage("message");
        msg.msg.getChatModifier().setChatClickable(new ChatClickable(EnumClickAction.RUN_COMMAND))
        pluginsSeen.sort(String::compareToIgnoreCase);
    }

    public boolean isStale() {
        return created.isBefore(ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(maxSecondsDelay));
    }

    public void addSystemChatLine(final String line) {
        System.out.println(line);

        receivedMessages.add(line);
        interpretPluginLine(line);
        interpretAmountOfPlugins(line);
    }

    private void interpretPluginLine(final String line) {
        if (amountOfPlugins == null) {
            return;
        }

        String fullText = getTextFromJson(line);
        if (fullText.length() > maxLineLength) {
            maxLineLength = fullText.length();
        }

        //[16:47:08 INFO]: Sensei_Orange issued server command: /pl
        //[16:47:08 INFO]: [PluginHider] [STDOUT] {"color":"white","text":"Server Plugins (12):"}
        //[16:47:09 INFO]: [PluginHider] [STDOUT] {"color":"#ED8106","text":"Bukkit Plugins:"}
        //[16:47:09 INFO]: [PluginHider] [STDOUT] {"extra":[{"color":"dark_gray","text":"- "},{"extra":[{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version Essentials"},"text":"Essentials"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version EssentialsChat"},"text":"EssentialsChat"}],"text":""},{"text":", "},{"extra":[{"color":"red","clickEvent":{"action":"run_command","value":"/version MoneyFromMobs"},"text":"MoneyFromMobs"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version MyCommand"},"text":"MyCommand"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version MythicMobs"},"text":"MythicMobs"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version NotBounties"},"text":"NotBounties"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version PluginHider"},"text":"PluginHider"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version ProtocolLib"},"text":"ProtocolLib"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version TAB"},"text":"TAB"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version TreeCuter"},"text":"TreeCuter"}],"text":""}],"text":""}],"text":" "}
        //[16:47:09 INFO]: [PluginHider] [STDOUT] {"extra":[{"extra":[{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version ViaBackwards"},"text":"ViaBackwards"}],"text":""},{"text":", "},{"extra":[{"color":"green","clickEvent":{"action":"run_command","value":"/version ViaVersion"},"text":"ViaVersion"}],"text":""}],"text":""}],"text":" "}
        System.out.println(line);
    }

    private void interpretAmountOfPlugins(final String line) {
        if (amountOfPlugins != null) {
            return;
        }

        Matcher match = amountPluginsPattern.matcher(line);
        if (match.find()) {
            amountOfPlugins = Integer.parseInt(match.group(1));
        }
    }

    public boolean isFinished() {
        return amountOfPlugins != null && pluginsSeen.size() == amountOfPlugins;
    }

    private void getTextFromNode(@NotNull StringBuilder tmp, @NotNull JsonNode current) {
        if (current.isObject()) {
            if (current.has("extra")) {
                getTextFromNode(tmp, current.get("extra"));
            }

            if (current.has("text")) {
                tmp.append(current.get("text"));
            }
        }
        else if (current.isArray()) {
            for (var el : current) {
                getTextFromNode(tmp, el);
            }
        }
    }

    private @NotNull String getTextFromJson(final String line) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(line);
            StringBuilder tmp = new StringBuilder();
            getTextFromNode(tmp, root);
            return tmp.toString();
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
