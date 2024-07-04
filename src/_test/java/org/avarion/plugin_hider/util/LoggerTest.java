package org.avarion.plugin_hider.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.logging.Level;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

class LoggerTest {

    @Mock
    private java.util.logging.Logger mockLogger;

    private Logger loggerUnderTest;

    @BeforeEach
    void setUp() {
        initMocks(this);
        loggerUnderTest = new Logger(mockLogger);
    }

    @Test
    void testInfo() {
        // Setup
        // Run the test
        loggerUnderTest.info("message", "args");

        // Verify the results
        verify(mockLogger).log(eq(Level.INFO), eq("message"), any(Object[].class));
    }

    @Test
    void testWarning() {
        // Setup
        // Run the test
        loggerUnderTest.warning("message", "args");

        // Verify the results
        verify(mockLogger).log(eq(Level.WARNING), eq("message"), any(Object[].class));
    }

    @Test
    void testError() {
        // Setup
        // Run the test
        loggerUnderTest.error("message", "args");

        // Verify the results
        verify(mockLogger).log(eq(Level.SEVERE), eq("message"), any(Object[].class));
    }

    @Test
    void testDebug() {
        // Setup
        // Run the test
        loggerUnderTest.debug("message", "args");

        // Verify the results
        verify(mockLogger).log(eq(Level.FINE), eq("message"), any(Object[].class));
    }
}
