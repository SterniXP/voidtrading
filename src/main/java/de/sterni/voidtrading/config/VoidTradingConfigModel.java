package de.sterni.voidtrading.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;
import lombok.Getter;
import org.slf4j.event.Level;

@SuppressWarnings("unused")
@Config(name = "void-trading-config", wrapperName = "VoidTradingConfig")
public class VoidTradingConfigModel {

    /**
     * Minimum time between two trades in ticks
     */
    @RangeConstraint(min = 0, max = Integer.MAX_VALUE)
    public int cooldown = 60;
    /**
     * Log Level used to log each time the mechanic is used.
     * @see LogLevel
     */
    public LogLevel logLevel = LogLevel.NONE;
    /**
     * Log level options for void trading events. Setting this to NONE will disable all event logging,
     * while TRACE, DEBUG, INFO, WARN, and ERROR will log events at their respective levels.
     */
    public enum LogLevel {
        NONE(-69),
        TRACE(Level.TRACE.toInt()),
        DEBUG(Level.DEBUG.toInt()),
        INFO(Level.INFO.toInt()),
        WARN(Level.WARN.toInt()),
        ERROR(Level.ERROR.toInt());

        @Getter
        private final int slf4jLevelInt;

        LogLevel(int slf4jLevelInt) {
            this.slf4jLevelInt = slf4jLevelInt;
        }
    }

    /**
     * Whether to enable custom trade cycling. If enabled, the custom trades of the same result item
     * will be cycled through each time the player attempts to add a trade.
     * if disabled, all custom trades with the same result item will be added at once.
     */
    public boolean enableCustomTradeCycling = false;

    public boolean consumeItemOnTradeChange = true;
}
