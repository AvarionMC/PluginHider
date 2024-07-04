package org.avarion.plugin_hider.util;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ReceivedPacketsTest {

    @Mock
    private Config mockCfg;

    private ReceivedPackets receivedPacketsUnderTest;

    @BeforeEach
    void setUp() {
        initMocks(this);
        receivedPacketsUnderTest = new ReceivedPackets(mockCfg, 0);
    }

    @Test
    void testIsStale() {
        // Setup
        // Run the test
        final boolean result = receivedPacketsUnderTest.isStale();

        // Verify the results
        assertFalse(result);
    }

    @Test
    void testAddSystemChatLine() {
        // Setup
        // Run the test
        receivedPacketsUnderTest.addSystemChatLine("line");

        // Verify the results
    }

    @Test
    void testIsFinished() {
        // Setup
        // Run the test
        final boolean result = receivedPacketsUnderTest.isFinished();

        // Verify the results
        assertFalse(result);
    }

    @Test
    void testSendModifiedMessage() {
        // Setup
        final Player player = null;
        when(mockCfg.shouldShow("pluginName")).thenReturn(false);

        // Run the test
        receivedPacketsUnderTest.sendModifiedMessage(player);

        // Verify the results
    }
}
