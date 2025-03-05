package org.avarion.pluginhider;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.TimeStampMode;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.avarion.pluginhider.listener.DeclareCommandsListener;
import org.avarion.pluginhider.listener.HelpCommandListener;
import org.avarion.pluginhider.listener.PluginCommandListener;
import org.avarion.pluginhider.listener.VersionCommandListener;
import org.avarion.pluginhider.util.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public class PluginHider extends JavaPlugin {
    public static PluginHider inst = null;
    public static Logger logger = null;
    public static Config config = null;

    public final Version currentVersion = new Version(getDescription().getVersion());

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

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
    }

    //region <Config>
    private void setupConfig() {
        config = new Config();
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
        Metrics ignored = new Metrics(this, Constants.bstatsPluginId);
    }
    //endregion

    //region <PacketEvents>
    private void setupPacketEvents() {
        PacketEvents.getAPI()
                    .getSettings()
                    .debug(false)
                    .reEncodeByDefault(true)
                    .checkForUpdates(false)
                    .timeStampMode(TimeStampMode.MILLIS);
        PacketEvents.getAPI().getEventManager().registerListeners(
                new DeclareCommandsListener(),
                new PluginCommandListener(),
                new VersionCommandListener(),
                new HelpCommandListener()
        );

        Bukkit.getScheduler().runTaskLater(this, PacketEvents.getAPI()::init, 1);
    }

    private void disableProtocolLib() {
        var theAPI = PacketEvents.getAPI();
        if (theAPI != null) {
            theAPI.terminate();
        }
    }
    //endregion

    //region <check for update>
    private void startUpdateCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> Updater.run());
    }
    //endregion

    private void addListeners() {
        Bukkit.getPluginManager().registerEvents(new VersionCommandListener(), this);
    }

    private void addCommands() {
        PluginCommand cmd = getCommand("pluginhider");
        if (cmd == null) {
            logger.error("Cannot find the pluginhider command??");
            getPluginLoader().disablePlugin(this);
            return;
        }

        PluginHiderCommand phc = new PluginHiderCommand();
        cmd.setExecutor(phc);
        cmd.setTabCompleter(phc);
    }
}
