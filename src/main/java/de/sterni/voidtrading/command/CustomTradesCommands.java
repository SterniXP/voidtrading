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
import de.sterni.voidtrading.logging.VoidTradingLogger;
import de.sterni.voidtrading.mixin.MerchantAccessorMixin;
import lombok.NonNull;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.component.ComponentMapPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static de.sterni.voidtrading.VoidTrading.CONFIG;
import static net.minecraft.command.argument.ItemStackArgumentType.getItemStackArgument;
import static net.minecraft.command.argument.ItemStackArgumentType.itemStack;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CustomTradesCommands {
    public static final TextColor TEAL = TextColor.fromFormatting(Formatting.AQUA);
    public static final TextColor RED = TextColor.fromRgb(0xFF0000);
    public static final TextColor YELLOW = TextColor.fromRgb(0xFFFF00);
    public static final ItemStack DEFAULT_INGREDIENT = new ItemStack(Items.EMERALD, 8);
    public static final String BAN = "ban?";
    private static final String RESTORE = "restore?";
    public static final String ONLY_ACTIVE = "onlyactive?";
    private static final String INDEX = "index";
    private static final String LISTS_NAMES = "list names";

    private final TradeMaterialsEditor tradeMaterialsEditor = TradeMaterialsEditor.getInstance();
    private final TradeBlackListEditor tradeBlackListEditor = TradeBlackListEditor.getInstance();

    public static void registerCommands() {
        CustomTradesCommands instance = new CustomTradesCommands();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, _) -> {
            registerListCommands(dispatcher, registryAccess, instance);
            registerBlacklistCommands(dispatcher, registryAccess, instance);
            registerReloadCommands(dispatcher, instance);
            registerSaveCommands(dispatcher, instance);
        });
    }

    private static void registerListCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                             CommandRegistryAccess registryAccess,
                                             CustomTradesCommands instance) {
        dispatcher.register(literal("customtrades")
                .executes(context -> defaultCommandResponse(instance, context))
                .then(literal(TradeMaterialsEditor.LIST_COMMAND_NAME)
                        .executes(instance::showTradeList)
                        .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                .suggests((_, builder) -> instance.suggestItems(builder, instance.tradeMaterialsEditor))
                                .executes(instance::showTradeList)
                                .then(argument(ONLY_ACTIVE, bool())
                                        .executes(instance::showTradeList)
                                )
                        )
                        .then(argument(ONLY_ACTIVE, bool()).executes(instance::showTradeList))
                        .then(literal("items").executes(context -> instance.showAvailableItems(context, instance.tradeMaterialsEditor)))
                        .then(literal("add")
                                .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .executes(instance::addNewWhiteTrade)
                                        .then(argument(ListEditor.RESULT_AMOUNT, integer(1))
                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.RESULT_MATERIAL))
                                                .executes(instance::addNewWhiteTrade)
                                                .then(argument(ListEditor.INGREDIENT_1_MATERIAL, itemStack(registryAccess))
                                                        .executes(instance::addNewWhiteTrade)
                                                        .then(argument(ListEditor.INGREDIENT_1_AMOUNT, integer(1))
                                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.INGREDIENT_1_MATERIAL))
                                                                .executes(instance::addNewWhiteTrade)
                                                                .then(argument(ListEditor.INGREDIENT_2_MATERIAL, itemStack(registryAccess))
                                                                        .executes(instance::addNewWhiteTrade)
                                                                        .then(argument(ListEditor.INGREDIENT_2_AMOUNT, integer(1))
                                                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.INGREDIENT_2_MATERIAL))
                                                                                .executes(instance::addNewWhiteTrade)
                                                                                .then(argument(ListEditor.MAX_USES, integer(1))
                                                                                        .executes(instance::addNewWhiteTrade)
                                                                                        .then(argument(ListEditor.ACTIVE, bool())
                                                                                                .executes(instance::addNewWhiteTrade)
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                                .then(argument(ListEditor.MAX_USES, integer(1))
                                                                        .executes(instance::addNewWhiteTrade)
                                                                        .then(argument(ListEditor.ACTIVE, bool())
                                                                                .executes(instance::addNewWhiteTrade)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("remove")
                                .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .suggests((_, builder) -> instance.suggestItems(builder, instance.tradeMaterialsEditor))
                                        .executes(context -> instance.removeTrade(context, instance.tradeMaterialsEditor, instance.tradeBlackListEditor, BAN, true))
                                        .then(argument(INDEX, integer(1))
                                                .suggests((context, builder) -> instance.suggestIndices(context, builder, instance.tradeMaterialsEditor))
                                                .executes(context -> instance.removeTrade(context, instance.tradeMaterialsEditor, instance.tradeBlackListEditor, BAN, true))
                                                .then(argument(BAN, bool())
                                                        .executes(context -> instance.removeTrade(context, instance.tradeMaterialsEditor, instance.tradeBlackListEditor, BAN, true))
                                                )
                                        )
                                )
                        )
                        .then(literal("setactive")
                                .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .suggests((_, builder) -> instance.suggestItems(builder, instance.tradeMaterialsEditor))
                                        .then(argument(INDEX, integer(1))
                                                .suggests((context, builder) -> instance.suggestIndices(context, builder, instance.tradeMaterialsEditor))
                                                .then(argument(ListEditor.ACTIVE, bool())
                                                        .executes(context -> instance.setActive(context, instance.tradeMaterialsEditor))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static void registerBlacklistCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                                  CommandRegistryAccess registryAccess,
                                                  CustomTradesCommands instance) {
        dispatcher.register(literal("customtrades")
                .then(literal(TradeBlackListEditor.LIST_COMMAND_NAME)
                        .executes(instance::showBlackList)
                        .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                .suggests((_, builder) -> instance.suggestItems(builder, instance.tradeBlackListEditor))
                                .executes(instance::showBlackList)
                                .then(argument(ONLY_ACTIVE, bool())
                                        .executes(instance::showBlackList)
                                )
                        )
                        .then(argument(ONLY_ACTIVE, bool()).executes(instance::showBlackList))
                        .then(literal("items").executes(context -> instance.showAvailableItems(context, instance.tradeBlackListEditor)))
                        .then(literal("add")
                                .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .executes(instance::addNewBlackTrade)
                                        .then(argument(ListEditor.RESULT_AMOUNT, integer(1))
                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.RESULT_MATERIAL))
                                                .executes(instance::addNewBlackTrade)
                                                .then(argument(ListEditor.INGREDIENT_1_MATERIAL, itemStack(registryAccess))
                                                        .executes(instance::addNewBlackTrade)
                                                        .then(argument(ListEditor.INGREDIENT_1_AMOUNT, integer(1))
                                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.INGREDIENT_1_MATERIAL))
                                                                .executes(instance::addNewBlackTrade)
                                                                .then(argument(ListEditor.INGREDIENT_2_MATERIAL, itemStack(registryAccess))
                                                                        .executes(instance::addNewBlackTrade)
                                                                        .then(argument(ListEditor.INGREDIENT_2_AMOUNT, integer(1))
                                                                                .suggests((context, builder) -> instance.suggestAmount(context, builder, ListEditor.INGREDIENT_2_MATERIAL))
                                                                                .executes(instance::addNewBlackTrade)
                                                                                .then(argument(ListEditor.MAX_USES, integer(1))
                                                                                        .executes(instance::addNewBlackTrade)
                                                                                        .then(argument(ListEditor.ACTIVE, bool())
                                                                                                .executes(instance::addNewBlackTrade)
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                                .then(argument(ListEditor.MAX_USES, integer(1))
                                                                        .executes(instance::addNewBlackTrade)
                                                                        .then(argument(ListEditor.ACTIVE, bool())
                                                                                .executes(instance::addNewBlackTrade)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("remove")
                                .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .suggests((_, builder) -> instance.suggestItems(builder, instance.tradeBlackListEditor))
                                        .executes(context -> instance.removeTrade(context, instance.tradeBlackListEditor, instance.tradeMaterialsEditor, RESTORE, false))
                                        .then(argument(INDEX, integer(1))
                                                .suggests((context, builder) -> instance.suggestIndices(context, builder, instance.tradeBlackListEditor))
                                                .executes(context -> instance.removeTrade(context, instance.tradeBlackListEditor, instance.tradeMaterialsEditor, RESTORE, false))
                                                .then(argument(RESTORE, bool())
                                                        .executes(context -> instance.removeTrade(context, instance.tradeBlackListEditor, instance.tradeMaterialsEditor, RESTORE, false))
                                                )
                                        )
                                )
                        )
                        .then(literal("setactive")
                                .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                                .then(argument(ListEditor.RESULT_MATERIAL, itemStack(registryAccess))
                                        .suggests((_, builder) -> instance.suggestItems(builder, instance.tradeBlackListEditor))
                                        .then(argument(INDEX, integer(1))
                                                .suggests((context, builder) -> instance.suggestIndices(context, builder, instance.tradeBlackListEditor))
                                                .then(argument(ListEditor.ACTIVE, bool())
                                                        .executes(context -> instance.setActive(context, instance.tradeBlackListEditor))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private CompletableFuture<Suggestions> suggestIndices(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, ListEditor listEditor) {
        int size = listEditor.getTradesWithResult(getItemStackArgument(context, ListEditor.RESULT_MATERIAL).getItem(), false).size();
        for (int i = 1; i <= size; i++) if (String.valueOf(i).contains(builder.getRemaining())) builder.suggest(i);
        return builder.buildFuture();
    }

    private static void registerReloadCommands(CommandDispatcher<ServerCommandSource> dispatcher, CustomTradesCommands instance) {
        final String successMessage = "neu von der Festplatte geladen.";
        dispatcher.register(literal("customtrades")
                .then(literal("reload")
                        .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                        .executes(context -> instance.makeListsDo(context, ListEditor::loadFromFile, successMessage))
                        .then(argument(LISTS_NAMES, greedyString())
                                .suggests(CustomTradesCommands::suggestListNames)
                                .executes(context -> instance.makeListsDo(context, ListEditor::loadFromFile, successMessage))
                        )
                )
        );
    }

    private static void registerSaveCommands(CommandDispatcher<ServerCommandSource> dispatcher, CustomTradesCommands instance) {
        final String successMessage = "erfolgreich gespeichert.";
        dispatcher.register(literal("customtrades")
                .then(literal("save")
                        .requires(source -> source.getPermissions().hasPermission(VoidTrading.PERMISSION_LEVEL))
                        .executes(context -> instance.makeListsDo(context, ListEditor::saveToFile, successMessage))
                        .then(argument(LISTS_NAMES, greedyString())
                                .suggests(CustomTradesCommands::suggestListNames)
                                .executes(context -> instance.makeListsDo(context, ListEditor::saveToFile, successMessage))
                        )
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestListNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        ListEditor.getAllEditorNames().stream().filter(name -> name.toLowerCase().contains(builder.getRemaining().toLowerCase())).forEach(builder::suggest);
        builder.suggest("ALL");
        return builder.buildFuture();
    }

    private int makeListsDo(CommandContext<ServerCommandSource> context, Consumer<ListEditor> method, String wereWhat) {
        String listsToReload = getOptionalArgument(context, LISTS_NAMES, String.class).orElse("ALL");
        if (listsToReload.contains("ALL")) {
            ListEditor.getAllEditors().forEach(method);
            sendFeedbackToSender(context.getSource(), "Alle Listen wurden ", TEAL, true);
            return 1;
        }
        String[] listNames = listsToReload.split(" ");
        int result = 0;
        for (String listName : listNames) {
            ListEditor listEditor = ListEditor.getInstance(listName);
            if (listEditor != null) {
                method.accept(listEditor);
                sendFeedbackToSender(context.getSource(), "Die " + listName + " wurde " + wereWhat, TEAL, true);
                result |= 1;
            } else {
                sendErrorToSender(context.getSource(), "Die Liste '" + listName + "' existiert nicht. Verfügbare Listen: " + ListEditor.getAllEditorNames());
            }
        }
        return result;
    }

    private int showAvailableItems(CommandContext<ServerCommandSource> context, ListEditor listEditor) {
        Set<Identifier> identifiers = listEditor.getIdentifiers();
        if (identifiers.isEmpty()) {
            sendMessageToSender(context.getSource(), "Die Liste '" + listEditor.getListName() + "' ist komplett leer.");
        } else {
            StringBuilder message = new StringBuilder("Die Liste '" + listEditor.getListName() + "' enthält folgende Items als Handelsergebnis:\n");
            identifiers.forEach(id -> message.append("- ").append(id.toString()).append("\n"));
            sendMessageToSender(context.getSource(), message.toString());
        }
        return 1;
    }

    private int setActive(CommandContext<ServerCommandSource> context, ListEditor listEditor) throws CommandSyntaxException {
        Identifier itemId = Registries.ITEM.getId(getItemStackArgument(context, ListEditor.RESULT_MATERIAL).getItem());
        int index = context.getArgument(INDEX, Integer.class);
        boolean active = context.getArgument(ListEditor.ACTIVE, Boolean.class);

        TradeOffer offer = listEditor.getOffer(itemId, index);
        ListEditor.setOfferActive(offer, active);
        sendFeedbackToSender(context.getSource(), "Der Handel '" +
                        listEditor.tradeOfferToString(offer, new StringBuilder(), index).toString() + "' wurde auf " +
                        (active ? "aktiv" : "inaktiv") + " gesetzt.", TEAL, true);
        return 1;
    }

    private int removeTrade(CommandContext<ServerCommandSource> context, ListEditor removeFrom, ListEditor addTo, String shouldAddName, boolean closeScreens) throws CommandSyntaxException {
        Identifier itemId = Registries.ITEM.getId(getItemStackArgument(context, ListEditor.RESULT_MATERIAL).getItem());
        int index = getOptionalArgument(context, INDEX, Integer.class).orElse(1);
        boolean shouldAdd = getOptionalArgument(context, shouldAddName, Boolean.class).orElse(false);
        TradeOffer removed = removeFrom.removeTrade(itemId, index);
        ListEditor.setOfferActive(removed, true);
        sendFeedbackToSender(context.getSource(), "Der Handel '" +
                        removeFrom.tradeOfferToString(removed, new StringBuilder(), -1).toString() +
                        "' wurde von der " + removeFrom.getListName() + " entfernt.", TEAL, true);
        if (shouldAdd && addTo.addTrade(removed) != -1) {
            if (closeScreens) closeAllVillagerMerchantScreenHandler(context.getSource().getServer());
            sendFeedbackToSender(context.getSource(), "Und zur " + addTo.getListName() + " hinzugefügt.", TEAL, true);
        }
        return 1;
    }

    private CompletableFuture<Suggestions> suggestItems(SuggestionsBuilder builder, ListEditor listEditor) {
        Set<Identifier> allowedIds = listEditor.getIdentifiers();
        return CommandSource.suggestIdentifiers(allowedIds, builder);
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
                        "Mit dem Befehl '/customtrades list' kannst du eine Liste aller verfügbaren Custom Handel anzeigen lassen."
        );
        return 1;
    }

    private void sendErrorToSender(ServerCommandSource source, String message) {
        source.sendError(Text.literal(message).setStyle(Style.EMPTY.withColor(RED)));
    }

    private void sendFeedbackToSender(ServerCommandSource source, String message, TextColor color, boolean log) {
        source.sendFeedback(() -> Text.literal(message).setStyle(Style.EMPTY.withColor(color)), false);
        if (log) {
            VoidTradingLogger.logEvent(message);
        }
    }

    private void sendMessageToSender(ServerCommandSource source, String message) {
        source.sendMessage(Text.literal(message).setStyle(Style.EMPTY.withColor(TEAL)));
    }

    public int showTradeList(@NonNull CommandContext<ServerCommandSource> context) {
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
        sendMessageToSender(context.getSource(), intro);
        boolean onlyActive = getOptionalArgument(context, ONLY_ACTIVE, Boolean.class).orElse(true);
        String onlyActiveText = onlyActive ? "aktiven" : "aller";
        ListEditor.TradesStringWithCount tradeList = getTradesStringFromList(context, tradeMaterialsEditor, onlyActive);
        if (tradeList.tradesAsString().isEmpty()) {
            sendMessageToSender(context.getSource(), "Die Liste der " + onlyActiveText + " Custom Handel ist leer.");
        } else {
            sendMessageToSender(context.getSource(), "Die Liste der " + onlyActiveText + " Custom Handel enthält " +
                    tradeList.count() + " Einträge:\n" + tradeList.tradesAsString());
        }
        return 1;
    }

    private ListEditor.TradesStringWithCount getTradesStringFromList(CommandContext<ServerCommandSource> context,
                                                                     ListEditor listEditor,
                                                                     boolean onlyActive) {
        Optional<ItemStackArgument> stackArgument = getOptionalArgument(context, ListEditor.RESULT_MATERIAL, ItemStackArgument.class);
        ListEditor.TradesStringWithCount result;
        if (stackArgument.isPresent()) {
            result = listEditor.getTradesAsString(stackArgument.get().getItem(), onlyActive);
        } else {
            result = listEditor.getTradesAsString(onlyActive);
        }
        return result;
    }

    private int addNewWhiteTrade(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return addNewTrade(context, tradeMaterialsEditor, tradeBlackListEditor);
    }

    private int addNewBlackTrade(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        closeAllVillagerMerchantScreenHandler(context.getSource().getServer());
        return addNewTrade(context, tradeBlackListEditor, tradeMaterialsEditor);
    }

    public int addNewTrade(@NonNull CommandContext<ServerCommandSource> context, @NonNull ListEditor addTo, @NonNull ListEditor removeFrom) throws CommandSyntaxException {
        ItemStackArgument sellItem = getItemStackArgument(context, ListEditor.RESULT_MATERIAL);
        Optional<Integer> resultAmount = getOptionalArgument(context, ListEditor.RESULT_AMOUNT, Integer.class);
        Optional<ItemStackArgument> ingredient1 = getOptionalArgument(context, ListEditor.INGREDIENT_1_MATERIAL, ItemStackArgument.class);
        Optional<Integer> ingredient1Amount = getOptionalArgument(context, ListEditor.INGREDIENT_1_AMOUNT, Integer.class);
        Optional<ItemStackArgument> ingredient2 = getOptionalArgument(context, ListEditor.INGREDIENT_2_MATERIAL, ItemStackArgument.class);
        Optional<Integer> ingredient2Amount = getOptionalArgument(context, ListEditor.INGREDIENT_2_AMOUNT, Integer.class);
        Optional<Integer> maxUses = getOptionalArgument(context, ListEditor.MAX_USES, Integer.class);
        Optional<Boolean> active = getOptionalArgument(context, ListEditor.ACTIVE, Boolean.class);

        TradedItem firstBuyItem = getTradedItemFrom(ingredient1, ingredient1Amount, true);
        Optional<TradedItem> secondBuyItem = Optional.ofNullable(getTradedItemFrom(ingredient2, ingredient2Amount, false));

        TradeOffer offer = new TradeOffer(
                firstBuyItem,
                secondBuyItem,
                getValidStackFrom(sellItem, resultAmount),
                maxUses.orElse(ListEditor.DEFAULT_MAX_USES),
                1,
                0.2F
        );
        ListEditor.setOfferActive(offer, active.orElse(true));
        int newIndex = addTo.addTrade(offer);
        int result = 1;
        if (newIndex != -1) {
            sendFeedbackToSender(context.getSource(),
                    "Der Custom Handel wurde erfolgreich zur " + addTo.getListName() + " hinzugefügt:\n" +
                            sellItem.getItem().toString() + ":" + addTo.tradeOfferToString(offer, new StringBuilder(),
                            (newIndex + 1)).toString(), TEAL, true);
        } else {
            sendFeedbackToSender(context.getSource(), "Der gewünschte Handel ist bereits auf der Liste. " +
                            "Die Liste der Handel kann mit dem \"/customtrades " + addTo.getListCommandName() +
                            "\" Befehl angezeigt werden.", YELLOW, false);
            result = 0;
        }
        int removeIndex = removeFrom.containsTrade(offer);
        if (removeIndex != -1) {
            removeFrom.removeTrade(Registries.ITEM.getId(offer.getSellItem().getItem()), removeIndex + 1);
            sendFeedbackToSender(context.getSource(), "Wurde aber von der " + removeFrom.getListName() + " entfernt, da er nicht auf beiden Listen gleichzeitig sein kann.", TEAL, true);
            result = 1;
        }
        return result;
    }

    private <T> Optional<T> getOptionalArgument(CommandContext<ServerCommandSource> context, String argName, Class<T> type) {
        if (context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(argName))) {
            return Optional.ofNullable(context.getArgument(argName, type));
        }
        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")// Optionals are not persisted, IntelliJ is dumb
    private TradedItem getTradedItemFrom(Optional<ItemStackArgument> argument, Optional<Integer> amount, boolean useDefaultStack) throws CommandSyntaxException {
        if (argument.isPresent()) {
            ItemStack stack = getValidStackFrom(argument.get(), amount);
            ComponentMapPredicate components = ComponentMapPredicate.of(stack.getComponents());
            return new TradedItem(stack.getRegistryEntry(), stack.getCount(), components);
        } else if (useDefaultStack) {
            return new TradedItem(DEFAULT_INGREDIENT.getItem(), DEFAULT_INGREDIENT.getCount());
        } else {
            return null;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")// Optionals are not persisted, IntelliJ is dumb
    private ItemStack getValidStackFrom(ItemStackArgument argument, Optional<Integer> amount) throws CommandSyntaxException {
        ItemStack stack = argument.createStack(1, false);
        ItemStack result;
        if (amount.isPresent()) {
            result = argument.createStack(amount.get(), true);
        } else if (stack.getMaxCount() > DEFAULT_INGREDIENT.getCount()) {
            result = argument.createStack(DEFAULT_INGREDIENT.getCount(), true);
        } else {
            result = stack;
        }
        return result;
    }

    public int showBlackList(@NonNull CommandContext<ServerCommandSource> context) {
        sendMessageToSender(context.getSource(), "Die " + tradeBlackListEditor.getListName() + " enthält alle Handel, " +
                "die von Villagern bei nächster Gelegenheit entfernt werden");
        boolean onlyActive = getOptionalArgument(context, ONLY_ACTIVE, Boolean.class).orElse(true);
        String onlyActiveText = onlyActive ? "aktiven" : "aller";
        ListEditor.TradesStringWithCount blackList = getTradesStringFromList(context, tradeBlackListEditor, onlyActive);
        if (blackList.tradesAsString().isEmpty()) {
            sendMessageToSender(context.getSource(), "Die Liste der " + onlyActiveText + " gebannten Handel ist leer.");
        } else {
            sendMessageToSender(context.getSource(), "Die Liste der " + onlyActiveText + " gebannten Handel enthält " +
                    blackList.count() + " Einträge:\n" + blackList.tradesAsString());
        }
        return 1;
    }

    private void closeAllVillagerMerchantScreenHandler(@NonNull MinecraftServer server) {
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (player.currentScreenHandler instanceof MerchantAccessorMixin screenHandler
                    && screenHandler.getMerchant() instanceof VillagerEntity) {
                player.closeHandledScreen();
            }
        });
    }
}
