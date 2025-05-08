package de.sterni.voidtrading.command.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

import static de.sterni.voidtrading.command.TradeLogLevelCommand.LOG_LEVEL_ARG_NAME;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static de.sterni.voidtrading.config.VoidTradingConfigModel.LogLevel;

public class LogLevelSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String currentArgument;
        try {
            currentArgument = getString(context, LOG_LEVEL_ARG_NAME).toUpperCase();
        } catch (IllegalArgumentException | NullPointerException ignored) {
            currentArgument = "";
        }

        for (LogLevel logLevel : LogLevel.values()) {
            if (logLevel.name().contains(currentArgument)) {
                builder.suggest(logLevel.name());
            }
        }

        // Lock the suggestions after we've modified them.
        return builder.buildFuture();
    }
}