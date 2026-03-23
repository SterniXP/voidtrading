package de.sterni.voidtrading.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.sterni.voidtrading.VoidTrading;
import de.sterni.voidtrading.customtrades.ListEditor;
import de.sterni.voidtrading.customtrades.TradeBlackListEditor;
import de.sterni.voidtrading.customtrades.TradeMaterialsEditor;
import lombok.NonNull;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.BoolArgumentType.*;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static net.minecraft.command.argument.ItemStackArgumentType.*;
import static net.minecraft.server.command.CommandManager.*;
import static de.sterni.voidtrading.VoidTrading.CONFIG;

public class CustomTradesCommands {
    public static final TextColor TEAL = TextColor.fromRgb(0x008080);
    public static final TextColor RED = TextColor.fromRgb(0xFF0000);
    public static final TextColor YELLOW = TextColor.fromRgb(0xFFFF00);
    private static final int DEFAULT_MAX_USES = 12;

    private final TradeMaterialsEditor tradeMaterialsEditor = TradeMaterialsEditor.getInstance();
    private final TradeBlackListEditor tradeBlackListEditor = TradeBlackListEditor.getInstance();

    public static void registerCommands() {
        CustomTradesCommands instance = new CustomTradesCommands();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerListCommands(dispatcher, registryAccess, instance));
    }

    private static void registerListCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CustomTradesCommands instance) {
        dispatcher.register(literal("customtrades")
                .executes(context -> defaultCommandResponse(instance, context))
                .then(literal("list")
                        .executes(context -> instance.showTradeList(context, true))
                        .then(argument("onlyactive", bool())
                                .executes(context -> instance.showTradeList(context, getBool(context, "onlyactive")))
                        )
                        .then(literal("add")
                                .requires(source -> source.hasPermissionLevel(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .executes(instance::addNewTrade)
                                        .then(argument(ListEditor.RESULT_AMOUNT, integer(1))
                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.RESULT_MATERIAL))
                                                .executes(instance::addNewTrade)
                                                .then(argument(ListEditor.INGREDIENT_1_MATERIAL, itemStack(registryAccess))
                                                        .executes(instance::addNewTrade)
                                                        .then(argument(ListEditor.INGREDIENT_1_AMOUNT, integer(1))
                                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.INGREDIENT_1_MATERIAL))
                                                                .executes(instance::addNewTrade)
                                                                .then(argument(ListEditor.INGREDIENT_2_MATERIAL, itemStack(registryAccess))
                                                                        .then(argument(ListEditor.INGREDIENT_2_AMOUNT, integer(1))
                                                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.INGREDIENT_2_MATERIAL))
                                                                                .executes(instance::addNewTrade)
                                                                                .then(argument(ListEditor.MAX_USES, integer(1))
                                                                                        .executes(instance::addNewTrade)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("remove")
                                .requires(source -> source.hasPermissionLevel(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .suggests((context, builder) -> instance.suggestItem(builder, registryAccess))
                                        .executes(instance::addNewTrade)
                                )
                        )
                        .then(literal("blacklist")
                                .executes(instance::showBlackList)
                        )
                )
        );
    }

    private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder builder, CommandRegistryAccess registryAccess) {
        Set<Identifier> allowedIds = tradeMaterialsEditor.getIdentifiers();
        return CommandSource.suggestIdentifiers(allowedIds, builder);
//        CompletableFuture<Suggestions> result;
//        if (builder.getRemaining().contains("[")) {
//            result = ItemStackArgumentType.itemStack(registryAccess).listSuggestions(context, builder);
//        } else {
//            result = CommandSource.suggestIdentifiers(allowedIds, builder);
//        }
//        return result;
    }

    private CompletableFuture<Suggestions> suggestAmount(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, String itemArgName) {
        int maxStackSize = 64;
        try {
            maxStackSize = getItemStackArgument(context, itemArgName).createStack(1, false).getMaxCount();

            int playerInput = Integer.parseInt(builder.getRemaining());
            if (playerInput <= maxStackSize) {
                addNumberSuggestions(builder, maxStackSize);
            } else {
                builder.suggest("§c<=" + maxStackSize);
            }
        } catch (Exception ignored) {
            addNumberSuggestions(builder, maxStackSize);
        }
        return builder.buildFuture();
    }

    private static void addNumberSuggestions(SuggestionsBuilder builder, int maxStackSize) {
        String remaining = builder.getRemaining();
        builder.suggest(1);
        for (int i = 2; i <= maxStackSize; i *= 2) {
            if (String.valueOf(i).contains(remaining)) {
                builder.suggest(i);
            }
        }
    }

    private static int defaultCommandResponse(CustomTradesCommands instance, CommandContext<ServerCommandSource> context) {
        instance.sendMessageToSender(context.getSource(), "Dieser Mod erlaubt es Spielern neue 'Custom Handel' zu Villagern hinzuzufügen.\n" +
                        "Mit dem Befehl '/customtrades list' kannst du eine Liste aller verfügbaren Custom Handel anzeigen lassen.",
                TEAL, false);
        return 1;
    }

    private void sendMessageToSender(ServerCommandSource source, String message, TextColor color, boolean broadcastToOps) {
        source.sendFeedback(() -> Text.literal(message).setStyle(Style.EMPTY.withColor(color)), broadcastToOps);
    }

    public int showTradeList(@NonNull CommandContext<ServerCommandSource> context, boolean showOnlyActive) {
        String intro = """
                Du kannst jedem beliebigem Villager einen Custom Handel hinzufügen, indem du auf diesen, mit dem gewünschten Item in der Hand, rechtsklickst.
                Das Item in deiner Hand stellt das Ergebnis des Handels dar und wird dabei verbraucht.
                Ein Villager kann immer nur eine Kategorie von Custom Handel haben.
                Das heißt, die natürlichen Handel eines Villagers werden nicht beeinflusst, aber die Custom Handel eines Villagers werden überschrieben, wenn du einen neuen Custom Handel hinzufügst.
                
                Wenn mehrere CustomTrades das selbe Item als Ziel haben""";
        if (CONFIG.enableCustomTradeCycling()) {
            intro += " und erneut mit demselben Item in der Hand mit dem Villager interagiert wird, werden die Custom Handel eines Villagers zyklisch innerhalb der Item Kategorie gewechselt. (Bzw. es passiert nichts, wenn es nur einen Custom Handel mit diesem Item als Ziel gibt und der Villager diesen Handel bereits hat.)";
        } else {
            intro += ", werden alle diese Handel dem Villager hinzugefügt. (Bzw. es passiert nichts, wenn der Villager bereits alle diese Custom Handel hat.)";
        }
        sendMessageToSender(context.getSource(), intro, TEAL, false);
        String tradeList = tradeMaterialsEditor.getTradesAsString(showOnlyActive);
        if (tradeList.isEmpty()) {
            sendMessageToSender(context.getSource(), "Die Liste der Custom Handel ist leer.",
                    TEAL, false);
        } else {
            sendMessageToSender(context.getSource(), "Verfügbare Custom Handel:\n" + tradeList,
                    TEAL, false);
        }
        return 1;
    }

    public int addNewTrade(@NonNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStackArgument sellItem = getItemStackArgument(context, ListEditor.RESULT_MATERIAL);
        Optional<Integer> resultAmount = getOptionalArgument(context, ListEditor.RESULT_AMOUNT, Integer.class);
        Optional<ItemStackArgument> ingredient1 = getOptionalArgument(context, ListEditor.INGREDIENT_1_MATERIAL, ItemStackArgument.class);
        Optional<Integer> ingredient1Amount = getOptionalArgument(context, ListEditor.INGREDIENT_1_AMOUNT, Integer.class);
        Optional<ItemStackArgument> ingredient2 = getOptionalArgument(context, ListEditor.INGREDIENT_2_MATERIAL, ItemStackArgument.class);
        Optional<Integer> ingredient2Amount = getOptionalArgument(context, ListEditor.INGREDIENT_2_AMOUNT, Integer.class);
        Optional<Integer> maxUses = getOptionalArgument(context, ListEditor.MAX_USES, Integer.class);

        // TODO: HIER WEITER MACHEN
        TradeOffer offer = new TradeOffer(
                new TradedItem(sellItem.createStack(resultAmount.orElse(1), true).getItem()),

        )


        sendMessageToSender(context.getSource(), String.valueOf(sellItem), TEAL, false);
        return 1;
    }

    private <T> Optional<T> getOptionalArgument(CommandContext<ServerCommandSource> context, String argName, Class<T> type) {
        if (context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(argName))) {
            return Optional.ofNullable(context.getArgument(argName, type));
        }
        return Optional.empty();
    }

    public int showBlackList(@NonNull CommandContext<ServerCommandSource> context) {
        String blackList = tradeBlackListEditor.getTradesAsString(false);
        if (blackList.isEmpty()) {
            sendMessageToSender(context.getSource(), "Die Liste der gebannten Handel ist leer.",
                    TEAL, false);
        } else {
            sendMessageToSender(context.getSource(), "Die Liste der gebannten Handel ist:\n" + blackList,
                    TEAL, false);
        }
        return 1;
    }
}
