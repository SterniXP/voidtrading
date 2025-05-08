package de.sterni.voidtrading.command;

import com.mojang.brigadier.context.CommandContext;
import de.sterni.voidtrading.config.VoidTradingConfigModel.LogLevel;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.text.MessageFormat;
import java.util.Arrays;

import static de.sterni.voidtrading.VoidTrading.CONFIG;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
public class TradeLogLevelCommand {
    public static final String LOG_LEVEL_ARG_NAME = "loglevel";

    public static int logLevel(CommandContext<ServerCommandSource> context) {
        LogLevel logLevel = CONFIG.logLevel();
        context.getSource().sendFeedback(() -> Text.literal(MessageFormat.format("The current void trading log level is {0}.", logLevel)), true);
        return 1;
    }

    public static int setLogLevel(CommandContext<ServerCommandSource> context) {
        String logLevelString = getString(context, LOG_LEVEL_ARG_NAME).toUpperCase();
        try {
            LogLevel logLevel = LogLevel.valueOf(logLevelString);
            CONFIG.logLevel(logLevel);
            context.getSource().sendFeedback(() -> Text.literal(MessageFormat.format("The void trading log level has been set to {0}.", logLevel)), true);
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFeedback(() -> Text.literal(MessageFormat.format("Failed to set log level to \"{0}\".\nAllowed Values: {1}", logLevelString, Arrays.toString(LogLevel.values()))), true);
        }
        return 1;
    }
}
