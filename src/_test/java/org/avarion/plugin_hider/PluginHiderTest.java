package org.avarion.plugin_hider;

import org.avarion.plugin_hider.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginHiderTest {

    private PluginHider pluginHiderUnderTest;

    @BeforeEach
    void setUp() {
        pluginHiderUnderTest = new PluginHider();
    }

    @Test
    void testOnEnable() {
        // Setup
        // Run the test
        pluginHiderUnderTest.onEnable();

        // Verify the results
    }

    @Test
    void testOnDisable() {
        // Setup
        // Run the test
        pluginHiderUnderTest.onDisable();

        // Verify the results
    }

    @Test
    void testGetMyConfig() {
        final Config result = pluginHiderUnderTest.getMyConfig();
    }
}
