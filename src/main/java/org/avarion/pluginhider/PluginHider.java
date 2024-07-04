package org.avarion.pluginhider;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.avarion.pluginhider.listener.CmdCompleteListener;
import org.avarion.pluginhider.listener.PluginCommandListener;
import org.avarion.pluginhider.listener.PluginResponseListener;
import org.avarion.pluginhider.listener.TabCompleteListener;
import org.avarion.pluginhider.util.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;


public class PluginHider extends JavaPlugin {
    public final Version currentVersion = new Version(getDescription().getVersion());
    public final LRUCache<UUID, ReceivedPackets> cachedUsers = new LRUCache<>(1000);
    public Logger logger = null;
    private Config config = null;
    private ProtocolManager protocolManager = null;

    @Override
    public void onEnable() {
        setupLogger();
        setupBStats();
        setupConfig();

        addListeners();
        addCommands();
        setupProtocolLib();

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

    public Config getMyConfig() {
        return config;
    }
    //endregion

    //region <Logger>
    private void setupLogger() {
        logger = new Logger(getLogger());
    }
    //endregion

    //region <bstats>
    private void setupBStats() {
        Metrics ignored = new Metrics(this, Constants.bstatsPluginId);
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
        protocolManager.addPacketListener(new PluginResponseListener(this));
        protocolManager.addPacketListener(new TabCompleteListener(this));

        if (config.hideHiddenPluginCommands) {
            protocolManager.addPacketListener(new CmdCompleteListener(this));
        }
    }

    private void disableProtocolLib() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(this);
            protocolManager = null;
        }
    }
    //endregion

    //region <check for update>
    private void startUpdateCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> Updater.run(this, Constants.spigotPluginId));
    }
    //endregion

    private void addListeners() {
        Bukkit.getPluginManager().registerEvents(new PluginCommandListener(this), this);
    }

    private void addCommands() {
        PluginCommand cmd = getCommand("pluginhider");
        if (cmd == null) {
            logger.error("Cannot find the pluginhider command??");
            getPluginLoader().disablePlugin(this);
            return;
        }

        cmd.setExecutor(new PluginHiderCommand(this));
        cmd.setTabCompleter(new PluginHiderCommand(this));
    }
}
