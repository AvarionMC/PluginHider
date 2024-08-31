package org.avarion.pluginhider;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.TimeStampMode;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.avarion.pluginhider.listener.TabCompleteListener;
import org.avarion.pluginhider.util.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;


public class PluginHider extends JavaPlugin {
    public static PluginHider inst;
    public static Logger logger = null;
    public static Config config = null;

    public final Version currentVersion = new Version(getDescription().getVersion());
    public final LRUCache<UUID, ReceivedPackets> cachedUsers = new LRUCache<>(1000);

    @Override
    public void onEnable() {
        inst = this;

        setupLogger();
        setupBStats();
        setupConfig();

        addListeners();
        addCommands();
        setupPacketEvents();

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
        config = new Config();
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

    //region <PacketEvents>
    private void setupPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(PluginHider.inst));

        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getSettings().debug(false).reEncodeByDefault(true).checkForUpdates(false).timeStampMode(TimeStampMode.MILLIS);
        PacketEvents.getAPI().init();

        PacketEvents.getAPI().getEventManager().registerListener(new TabCompleteListener());
    }

    private void disableProtocolLib() {
        var theAPI = PacketEvents.getAPI();
        if (theAPI!=null) {
            theAPI.terminate();
        }
    }
    //endregion

    //region <check for update>
    private void startUpdateCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> Updater.run(this, Constants.spigotPluginId));
    }
    //endregion

    private void addListeners() {
//        Bukkit.getPluginManager().registerEvents(new PluginCommandListener(), this);
    }

    private void addCommands() {
        PluginCommand cmd = getCommand("pluginhider");
        if (cmd == null) {
            logger.error("Cannot find the pluginhider command??");
            getPluginLoader().disablePlugin(this);
            return;
        }

        cmd.setExecutor(new PluginHiderCommand());
        cmd.setTabCompleter(new PluginHiderCommand());
    }
}
