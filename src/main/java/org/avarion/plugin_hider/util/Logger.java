package org.avarion.plugin_hider.util;

import java.util.logging.Level;

public class Logger {
    private final java.util.logging.Logger logger;

    public Logger(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public void info(String message) {
        logger.info(Constants.PREFIX + message);
    }

    public void warning(String message) {
        logger.warning(Constants.PREFIX + message);
    }

    public void error(String message) {
        logger.severe(Constants.PREFIX + message);
    }

    public void debug(Level level, String message) {
        logger.fine(Constants.PREFIX + message);
    }

    public void log(Level level, String message, Throwable thrown) {
        logger.log(level, Constants.PREFIX + message, thrown);
    }

    public void log(Level level, String message) {
        logger.log(level, Constants.PREFIX + message);
    }
}
