package de.sterni.voidtrading;

import de.sterni.voidtrading.command.CustomTradesCommands;
import de.sterni.voidtrading.command.TradeLogLevelCommand;
import de.sterni.voidtrading.command.TradeResetCooldownCommand;
import de.sterni.voidtrading.command.suggestions.LogLevelSuggestionProvider;
import de.sterni.voidtrading.config.VoidTradingConfig;
import de.sterni.voidtrading.customtrades.ListEditor;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.*;

public class VoidTrading implements ModInitializer {
    public static final String MOD_ID = "void-trading";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final VoidTradingConfig CONFIG = VoidTradingConfig.createAndLoad();
    public static final int PERMISSION_LEVEL = 2;
    private static final String SET_ARG_NAME = "set";

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Look Mom! I'm void trading.");

        registerVoidTradingCommands();

        CustomTradesCommands.registerCommands();

        registerShutdownHook();
    }

    private static void registerVoidTradingCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("voidtrading")
                        .then(literal(TradeResetCooldownCommand.COOLDOWN_ARG_NAME)
                                .executes(TradeResetCooldownCommand::cooldown)
                                .then(literal(SET_ARG_NAME)
                                        .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL))
                                        .then(argument(TradeResetCooldownCommand.COOLDOWN_ARG_NAME, integer())
                                                .executes(TradeResetCooldownCommand::setCooldown)
                                        )
                                )
                        )
                        //loglevel commands
                        .then(literal(TradeLogLevelCommand.LOG_LEVEL_ARG_NAME)
                                .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL))
                                .executes(TradeLogLevelCommand::logLevel)
                                .then(literal(SET_ARG_NAME)
                                        .then(argument(TradeLogLevelCommand.LOG_LEVEL_ARG_NAME, word())
                                                .suggests(new LogLevelSuggestionProvider())
                                                .executes(TradeLogLevelCommand::setLogLevel)
                                        )
                                )
                        )
                )
        );
    }

    private static void registerShutdownHook() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            CONFIG.save();
            ListEditor.getAllEditors().forEach(ListEditor::saveToFile);
        });
    }
}