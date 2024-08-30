package org.avarion.pluginhider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Constants;
import org.avarion.pluginhider.util.ReceivedPackets;
import org.avarion.pluginhider.util.Reflection;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class PluginResponseListener extends PacketAdapter {
    public PluginResponseListener() {
        super(PluginHider.inst, ListenerPriority.HIGHEST, Arrays.asList(PacketType.Play.Server.SYSTEM_CHAT, PacketType.Play.Client.CHAT_COMMAND));
    }

    @Override
    public void onPacketReceiving(@NotNull PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CHAT_COMMAND) {
            return;
        }

        String cmd = event.getPacket().getModifier().read(0).toString().toLowerCase();
        if (!Constants.isPluginCmd("/" + cmd.split(" ", 2)[0])) {
            return;
        }

        PluginHider.inst.cachedUsers.put(event.getPlayer().getUniqueId(), new ReceivedPackets(PluginHider.config, 10));
    }

    @Override
    public void onPacketSending(@NotNull PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT) {
            return;
        }

        UUID player = event.getPlayer().getUniqueId();
        if (!PluginHider.inst.cachedUsers.containsKey(player)) {
            return;
        }

        ReceivedPackets entry = PluginHider.inst.cachedUsers.get(player);

        String text = readChatMessage(event.getPacket());
        entry.addSystemChatLine(text);

        if (entry.amountOfPlugins == 0) {
            // No plugins...
            PluginHider.inst.cachedUsers.remove(player);
            return;
        }

        // Don't send out the original text.
        event.setCancelled(true);

        if (entry.isFinished()) {
            // Remove this from our cache, so we don't intercept it again
            PluginHider.inst.cachedUsers.remove(player);
            // Send messages if there is anything to send.
            entry.sendModifiedMessage(event.getPlayer());
        }
    }

    private @Nullable String readChatMessage(PacketContainer packet) {
        String json;
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

        return null;
    }
}
