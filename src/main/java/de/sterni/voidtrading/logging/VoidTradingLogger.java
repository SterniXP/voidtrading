package de.sterni.voidtrading.logging;

import de.sterni.voidtrading.config.VoidTradingConfigModel;
import org.slf4j.event.Level;

import static de.sterni.voidtrading.VoidTrading.CONFIG;
import static de.sterni.voidtrading.VoidTrading.LOGGER;

/**
 * This class collects all logging related code for the void trading mechanic.
 * It provides methods to log events at different log levels and formats the log messages according to the configured log level.
 */
public class VoidTradingLogger {

    /**
     * Logs an event with the given format and arguments. The log level is determined by the configuration and the message is formatted accordingly.
     * @param format the format string, which can contain placeholders for the arguments, e.g. "Player {} traded with the void trader."
     * @param args the arguments to be inserted into the format string, e.g. the player's name. The number of arguments should match the number of placeholders in the format string.
     *             If count does not match, the extra arguments will be ignored or if there are not enough arguments, the placeholders will be left as is.
     */
    public static void logEvent(String format, Object... args) {
        if (!VoidTradingConfigModel.LogLevel.NONE.equals(CONFIG.logLevel())) {
            LOGGER.atLevel(Level.intToLevel(CONFIG.logLevel().getSlf4jLevelInt())).log(format, args);
        }
    }
}
