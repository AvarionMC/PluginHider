package org.avarion.plugin_hider.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.protocol.game.PacketPlayOutCommands;
import org.avarion.plugin_hider.PluginHider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CmdCompleteListener extends PacketAdapter {
    private final PluginHider plugin;
    private final CommandBuildContext context = CommandDispatcher.a(VanillaRegistries.a());

    public CmdCompleteListener(@NotNull PluginHider plugin) {
        super(plugin, ListenerPriority.HIGH, Collections.singletonList(PacketType.Play.Server.COMMANDS), ListenerOptions.ASYNC);

        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(@NotNull PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null || player instanceof TemporaryPlayer) {
            plugin.logger.warning("null/temporary player found!");
            event.setCancelled(true);
            return;
        }

        var packet = event.getPacket().getHandle();
        event.setPacket(new PacketContainer(PacketType.Play.Server.COMMANDS, modifyPacket(packet)));
    }

    @SuppressWarnings("unchecked")
    private @NotNull Object modifyPacket(Object handle) {
        PacketPlayOutCommands cmds = (PacketPlayOutCommands) handle;
        RootCommandNode root = cmds.a(this.context);

        // region <Building up a map between plugin name -> commands>
        Map<String, List<String>> plugin2cmd = new HashMap<>();
        var it = root.getChildren().iterator();
        while (it.hasNext()) {
            CommandNode child = (CommandNode) it.next();
            String[] parts = child.getName().split(":", 2);
            if (parts.length == 2 && !plugin.getMyConfig().shouldShow(parts[0])) {
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