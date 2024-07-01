package org.avarion.plugin_hider.util;

import org.avarion.plugin_hider.PluginHider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class Updater {
    public static void run(PluginHider plugin, int pluginId) {
        Version lastVersion = getLatestVersion(pluginId);
        if ( lastVersion == null ) {
            plugin.logger.error("Couldn't fetch latest version information from SpigotMC.");
            return;
        }

        if ( lastVersion.compareTo(plugin.currentVersion) < 0) {
            plugin.logger.warning("New version available: " + lastVersion + ", you have: " + plugin.currentVersion);
        }
    }

    public static @Nullable Version getLatestVersion(int pluginId) {
        try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource="
                                               + pluginId).openStream(); Scanner scanner = new Scanner(inputStream)) {

            if (scanner.hasNext()) {
                return new Version(scanner.next());
            }
        }
        catch (IOException ignored) {
        }

        return null;
    }
}
