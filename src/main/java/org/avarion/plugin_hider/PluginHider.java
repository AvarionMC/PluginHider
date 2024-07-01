package org.avarion.plugin_hider;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import org.avarion.plugin_hider.listener.PluginCommandListener;
import org.avarion.plugin_hider.listener.PluginResponseListener;
import org.avarion.plugin_hider.plib.TabComplete;
import org.avarion.plugin_hider.util.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;


public class PluginHider extends JavaPlugin {
    Config config = null;

    private ProtocolManager protocolManager = null;

    public Logger logger = null;
    public final Version currentVersion = new Version(getDescription().getVersion());
    public final LRUCache<UUID, ReceivedPackets> cachedUsers = new LRUCache<>(1000);

    @Override
    public void onEnable() {
        setupLogger();
        setupBStats();
        setupConfig();

        setupProtocolLib();
        addCommands();

        logger.info("Loaded version: " + currentVersion);
        startUpdateCheck();
    }

    @Override
    public void onDisable() {
        disableProtocolLib();
        disableConfig();

        cachedUsers.clear();
    }

    //region <Config>
    private void setupConfig() {
        config = new Config(this);
    }

    private void disableConfig() {
        config = null;
    }
    //endregion

    //region <Logger>
    private void setupLogger() {
        logger = new Logger(getLogger());
    }
    //endregion

    //region <bstats>
    private void setupBStats() {
        Metrics metrics = new Metrics(this, Constants.bstatsPluginId);
    }
    //endregion

    //region <ProtocolLibrary>
    private void setupProtocolLib() {
        Version protocolVersion = new Version(ProtocolLibrary.getPlugin().getDescription().getVersion());
        if (protocolVersion.major < 5) {
            logger.error("ProtocolLib 5 or higher is needed. You have: " + protocolVersion);
            getPluginLoader().disablePlugin(this);
            return;
        }

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PluginCommandListener(this));
        protocolManager.addPacketListener(new PluginResponseListener(this));
    }

    private void disableProtocolLib() {
        if ( protocolManager != null ) {
            protocolManager.removePacketListeners(this);
            protocolManager = null;
        }
    }
    //endregion

    //region <check for update>
    private void startUpdateCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Updater.run(this, Constants.spigotPluginId);
        });
    }
    //endregion

    private void addCommands() {
        PluginCommand cmd = getCommand("pluginhide");
        if ( cmd == null ) {
            logger.error("Cannot find the pluginhide command??");
            getPluginLoader().disablePlugin(this);
            return;
        }

        cmd.setExecutor(new PluginHiderCommand(this));
        cmd.setTabCompleter(new PluginHiderCommand(this));
    }
}
