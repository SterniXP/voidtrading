package de.sterni.voidtrading.command;

import com.mojang.brigadier.context.CommandContext;
import io.wispforest.owo.config.Option;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.text.MessageFormat;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static de.sterni.voidtrading.VoidTrading.CONFIG;

public class TradeResetCooldownCommand {
    public static final String COOLDOWN_ARG_NAME = "cooldown";

    public static int cooldown(CommandContext<ServerCommandSource> context) {
        int resetCooldown = CONFIG.cooldown();
        double resetCooldownInSeconds = resetCooldown / 20.0;
        context.getSource().sendFeedback(() -> Text.literal(MessageFormat.format("The current trade reset cooldown is {0} ticks ({1}s).", resetCooldown, resetCooldownInSeconds)), false);
        return 1;
    }

    public static int setCooldown(CommandContext<ServerCommandSource> context) {
        int resetCooldown = getInteger(context, COOLDOWN_ARG_NAME);
        Option<Object> option = CONFIG.optionForKey(CONFIG.keys.cooldown);
        if (option == null) {
            context.getSource().sendFeedback(() -> Text.literal("Failed to set cooldown because the config is broken."), true);
        }
        else if (option.verifyConstraint(resetCooldown)) {
            double resetCooldownInSeconds = resetCooldown / 20.0;
            CONFIG.cooldown(resetCooldown);
            context.getSource().sendFeedback(() -> Text.literal(MessageFormat.format("The trade reset cooldown has been set to {0} ticks ({1}s).", resetCooldown, resetCooldownInSeconds)), true);
        } else {
            context.getSource().sendFeedback(() -> Text.literal(MessageFormat.format("Failed to set cooldown to {0}.\nAllowed Range: {1} ticks.", resetCooldown, option.constraint().formatted())), true);
        }
        return 1;
    }
}
