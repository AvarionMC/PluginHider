package org.avarion.plugin_hider.util;

import java.util.logging.Level;

public class Logger {
    private final java.util.logging.Logger logger;

    public Logger(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public void info(String message, Object... args) {
        logger.log(Level.INFO, message, args);
    }

    public void warning(String message, Object... args) {
        logger.log(Level.WARNING, message, args);
    }

    public void error(String message, Object... args) {
        logger.log(Level.SEVERE, message, args);
    }

    public void debug(String message, Object... args) {
        logger.log(Level.FINE, message, args);
    }
}
