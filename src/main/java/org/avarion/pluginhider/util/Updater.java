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
    public static void run(PluginHider plugin, int pluginId) {
        Version lastVersion = getLatestVersion(pluginId);
        if (lastVersion == null) {
            plugin.logger.error("Couldn't fetch latest version information from SpigotMC.");
            return;
        }

        if (lastVersion.compareTo(plugin.currentVersion) < 0) {
            plugin.logger.warning("New version available: " + lastVersion + ", you have: " + plugin.currentVersion);
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
        catch (URISyntaxException | IOException ignored) {
        }

        return null;
    }
}
