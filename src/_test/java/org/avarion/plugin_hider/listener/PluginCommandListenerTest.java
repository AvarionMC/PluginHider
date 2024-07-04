package org.avarion.plugin_hider.listener;

import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.Config;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PluginCommandListenerTest {

    @Mock
    private PluginHider mockPlugin;

    private PluginCommandListener pluginCommandListenerUnderTest;

    @BeforeEach
    void setUp() {
        initMocks(this);
        pluginCommandListenerUnderTest = new PluginCommandListener(mockPlugin);
    }

    @Test
    void testOnCommand() {
        // Setup
        final PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(null, "message");
        when(mockPlugin.getMyConfig()).thenReturn(new Config(new PluginHider()));

        // Run the test
        pluginCommandListenerUnderTest.onCommand(event);

        // Verify the results
    }
}
