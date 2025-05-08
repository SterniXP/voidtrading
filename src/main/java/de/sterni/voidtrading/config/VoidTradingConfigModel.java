package de.sterni.voidtrading.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Config(name = "void-trading-config", wrapperName = "VoidTradingConfig")
public class VoidTradingConfigModel {

    @RangeConstraint(min = 0, max = Integer.MAX_VALUE)
    public int cooldown = 60;
    public LogLevel logLevel = LogLevel.NONE;
    public enum LogLevel {
        NONE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
