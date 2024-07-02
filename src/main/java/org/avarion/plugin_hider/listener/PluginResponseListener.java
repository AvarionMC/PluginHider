package org.avarion.plugin_hider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.ReceivedPackets;
import org.avarion.plugin_hider.util.Reflection;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

public class PluginResponseListener extends PacketAdapter {
    final private PluginHider plugin;

    public PluginResponseListener(PluginHider plugin) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT);

        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        if (!plugin.cachedUsers.containsKey(player)) {
            return;
        }

        ReceivedPackets entry = plugin.cachedUsers.get(player);
        if (entry.isFinished()) {
            return;
        }

        // https://github.com/kangarko/Foundation/blob/master/src/main/java/org/mineacademy/fo/model/PacketListener.java#L307C26-L307C82
        String text = readChatMessage(event.getPacket());
        entry.addSystemChatLine(text);
        int a = 1;
    }

    private @Nullable String readChatMessage(PacketContainer packet) {
        String json = null;
        try {
            json = packet.getChatComponents().read(0).getJson();
        }
        catch (Exception ignored) {
            json = packet.getStrings().read(0);
        }

        if (json != null) {
            return json;
        }

        try {
            // https://github.com/dmulloy2/ProtocolLib/issues/2330
            final StructureModifier<Object> adventureModifier = packet.getModifier()
                                                                      .withType(AdventureComponentConverter.getComponentClass());

            if (!adventureModifier.getFields().isEmpty()) {
                final Object comp = adventureModifier.read(0);

                final Class<?> clazz = Reflection.getClass("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
                final Object gson = Reflection.callStatic(clazz, "gson");

                final Class<?> component = Reflection.getClass("net.kyori.adventure.text.Component");
                final Method gsonMethod = Reflection.getMethod(gson.getClass(), "serialize", component);

                return (String) gsonMethod.invoke(gson, comp);
            }
        }
        catch (Throwable ignored) {
        }
        //
        //        final Object adventureContent = Reflection.getFieldContent(event.getPacket().getHandle(), "adventure$content");
        //
        //        if (adventureContent != null) {
        //            final List<String> contents = new ArrayList<>();
        //
        //            this.mergeChildren(adventureContent, contents);
        //            final String mergedContents = String.join("", contents);
        //
        //            return mergedContents;
        //        }
        return null;
    }
}
