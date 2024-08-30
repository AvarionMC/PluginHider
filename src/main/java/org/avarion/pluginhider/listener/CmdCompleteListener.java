package org.avarion.pluginhider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.protocol.game.PacketPlayOutCommands;
import org.avarion.pluginhider.PluginHider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CmdCompleteListener extends PacketAdapter {
    private final CommandBuildContext context = CommandDispatcher.a(VanillaRegistries.a());

    public CmdCompleteListener() {
        super(PluginHider.inst, ListenerPriority.HIGH, Collections.singletonList(PacketType.Play.Server.COMMANDS), ListenerOptions.ASYNC);
    }

    @Override
    public void onPacketSending(@NotNull PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || player instanceof TemporaryPlayer) {
            PluginHider.logger.warning("null/temporary player found!");
            event.setCancelled(true);
            return;
        }

        var packet = event.getPacket().getHandle();
        event.setPacket(new PacketContainer(PacketType.Play.Server.COMMANDS, modifyPacket(packet)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private @NotNull Object modifyPacket(Object handle) {
        PacketPlayOutCommands cmds = (PacketPlayOutCommands) handle;
        RootCommandNode root = cmds.a(this.context);

        // region <Building up a map between plugin name -> commands>
        Map<String, List<String>> plugin2cmd = new HashMap<>();
        for (Object o : root.getChildren()) {
            CommandNode child = (CommandNode) o;
            String[] parts = child.getName().split(":", 2);
            if (parts.length == 2 && !PluginHider.inst.getMyConfig().shouldShow(parts[0])) {
                plugin2cmd.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
            }
        }
        // endregion

        for (var el : plugin2cmd.entrySet()) {
            el.getValue().forEach(root::removeCommand);
            el.getValue().forEach(cmd -> root.removeCommand(el.getKey() + ":" + cmd));
        }

        return new PacketPlayOutCommands(root);
    }
}