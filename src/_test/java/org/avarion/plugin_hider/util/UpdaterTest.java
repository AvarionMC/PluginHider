package org.avarion.plugin_hider.util;

import org.avarion.plugin_hider.PluginHider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdaterTest {

    @Test
    void testRun() {
        // Setup
        final PluginHider plugin = new PluginHider();

        // Run the test
        Updater.run(plugin, 0);

        // Verify the results
    }

    @Test
    void testGetLatestVersion() {
        // Setup
        final Version expectedResult = new Version("version");

        // Run the test
        final Version result = Updater.getLatestVersion(0);

        // Verify the results
        assertEquals(expectedResult, result);
    }
}
