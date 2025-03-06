package org.avarion.pluginhider.util;

import org.avarion.pluginhider.PluginHider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;


public class Updater {
    public static void run() {
        Version lastVersion = getLatestVersion(Constants.spigotPluginId);
        if (lastVersion == null) {
            PluginHider.logger.error("Couldn't fetch latest version information from SpigotMC.");
            return;
        }

        if (lastVersion.compareTo(PluginHider.inst.currentVersion) < 0) {
            PluginHider.logger.warning("New version available: "
                                       + lastVersion
                                       + ", you have: "
                                       + PluginHider.inst.currentVersion);
        }
    }

    public static @Nullable Version getLatestVersion(int pluginId) {
        URL url;
        try {
            url = new URI("https://api.spigotmc.org/legacy/update.php?resource=" + pluginId).toURL();

            InputStream inputStream = url.openStream();
            Scanner scanner = new Scanner(inputStream);

            if (scanner.hasNext()) {
                return new Version(scanner.next());
            }
        }
        catch (URISyntaxException | IOException e) {
            PluginHider.logger.warning("Couldn't fetch latest version information from SpigotMC: " + e.getMessage());
        }

        return null;
    }
}
