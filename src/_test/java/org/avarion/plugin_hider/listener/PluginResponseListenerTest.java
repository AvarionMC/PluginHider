package org.avarion.plugin_hider.listener;

import com.comphenix.protocol.events.PacketEvent;
import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PluginResponseListenerTest {

    @Mock
    private PluginHider mockPlugin;

    private PluginResponseListener pluginResponseListenerUnderTest;

    @BeforeEach
    void setUp() {
        initMocks(this);
        pluginResponseListenerUnderTest = new PluginResponseListener(mockPlugin);
    }

    @Test
    void testOnPacketReceiving() {
        // Setup
        final PacketEvent event = new PacketEvent("source");
        when(mockPlugin.getMyConfig()).thenReturn(new Config(new PluginHider()));

        // Run the test
        pluginResponseListenerUnderTest.onPacketReceiving(event);

        // Verify the results
    }

    @Test
    void testOnPacketSending() {
        // Setup
        final PacketEvent event = new PacketEvent("source");

        // Run the test
        pluginResponseListenerUnderTest.onPacketSending(event);

        // Verify the results
    }
}
