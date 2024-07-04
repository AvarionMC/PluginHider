package org.avarion.plugin_hider.listener;

import com.comphenix.protocol.events.PacketEvent;
import org.avarion.plugin_hider.PluginHider;
import org.avarion.plugin_hider.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class CmdCompleteListenerTest {

    @Mock
    private PluginHider mockPlugin;

    private CmdCompleteListener cmdCompleteListenerUnderTest;

    @BeforeEach
    void setUp() {
        initMocks(this);
        cmdCompleteListenerUnderTest = new CmdCompleteListener(mockPlugin);
    }

    @Test
    void testOnPacketSending() {
        // Setup
        final PacketEvent event = new PacketEvent("source");
        when(mockPlugin.getMyConfig()).thenReturn(new Config(new PluginHider()));

        // Run the test
        cmdCompleteListenerUnderTest.onPacketSending(event);

        // Verify the results
    }
}
