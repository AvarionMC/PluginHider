package org.avarion.plugin_hider.util;

import org.avarion.plugin_hider.PluginHider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ConfigTest {

    @Mock
    private PluginHider mockPlugin;

    private Config configUnderTest;

    @BeforeEach
    void setUp() {
        initMocks(this);
        configUnderTest = new Config(mockPlugin);
    }

    @Test
    void testReload() {
        // Setup
        when(mockPlugin.getConfig()).thenReturn(null);

        // Run the test
        configUnderTest.reload();

        // Verify the results
        verify(mockPlugin).reloadConfig();
    }

    @Test
    void testShouldShow() {
        // Setup
        // Run the test
        final boolean result = configUnderTest.shouldShow("pluginName");

        // Verify the results
        assertFalse(result);
    }
}
