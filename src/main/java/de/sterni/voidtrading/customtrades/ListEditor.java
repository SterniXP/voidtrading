package de.sterni.voidtrading.customtrades;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.serialization.JsonOps;
import lombok.NonNull;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.sterni.voidtrading.VoidTrading.LOGGER;

public abstract class ListEditor {
    protected static final JsonElement EMPTY_JSON_ARRAY = new JsonArray();
    public static final String INGREDIENT_1_MATERIAL = "INGREDIENT_1_MATERIAL";
    private static final String INGREDIENT_1_COMPONENTS = "INGREDIENT_1_COMPONENTS";
    public static final String INGREDIENT_1_AMOUNT = "INGREDIENT_1_AMOUNT";
    public static final String INGREDIENT_2_MATERIAL = "INGREDIENT_2_MATERIAL";
    private static final String INGREDIENT_2_COMPONENTS = "INGREDIENT_2_COMPONENTS";
    public static final String INGREDIENT_2_AMOUNT = "INGREDIENT_2_AMOUNT";
    public static final String RESULT_MATERIAL = "RESULT_MATERIAL";
    private static final String RESULT_COMPONENTS = "RESULT_COMPONENTS";
    public static final String RESULT_AMOUNT = "RESULT_AMOUNT";
    public static final String MAX_USES = "MAX_USES";
    /**
     * Can be used to update trades, internally uses TradeOffer#uses to store the state (uses == 0 -> active)
     */
    public static final String ACTIVE = "ACTIVE";

    public static final int DEFAULT_MAX_USES = 12;
    public static final TradedItem DEFAULT_SECOND_BUY_ITEM = new TradedItem(ItemStack.EMPTY.getItem(), ItemStack.EMPTY.getCount());

    protected final Map<Identifier, LinkedHashSet<TradeOffer>> trades = new HashMap<>();

    protected static final Map<String, ListEditor> NAME_INSTANCE_MAP = new HashMap<>();

    private static final Dynamic2CommandExceptionType TRADE_NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType((resultItem, tradeIndex)
            -> Text.literal("Der Handel [" + resultItem + ":" + tradeIndex + "] existiert nicht.")
    );

    public record TradesStringWithCount(String tradesAsString, int count) {}

    protected ListEditor(String listName) {
        NAME_INSTANCE_MAP.put(listName, this);
    }

    public abstract void loadFromFile();

    public TradesStringWithCount getTradesAsString(boolean onlyActive) {
        int count = 0;
        StringBuilder builder = new StringBuilder(200);
        for (Map.Entry<Identifier, LinkedHashSet<TradeOffer>> trade : trades.entrySet()) {
            builder.append(trade.getKey()).append(":\n");
            LinkedHashSet<TradeOffer> offers = trade.getValue();
            count += tradesOfItemToString(offers, builder, onlyActive);
        }
        return new TradesStringWithCount(builder.toString(), count);
    }

    public TradesStringWithCount getTradesAsString(Item resultItem, boolean onlyActive) {
        LinkedHashSet<TradeOffer> offers = trades.get(Registries.ITEM.getId(resultItem));
        if (offers == null) {
            return new TradesStringWithCount("", 0);
        }
        StringBuilder builder = new StringBuilder(200);
        int count = tradesOfItemToString(offers, builder, onlyActive);
        return new TradesStringWithCount(builder.toString(), count);
    }

    private int tradesOfItemToString(Set<TradeOffer> offers, StringBuilder builder, boolean onlyActive) {
        int i = 0;
        for (TradeOffer offer : offers) {
            if (onlyActive && !isOfferActive(offer)) continue;
            i++;
            tradeOfferToString(offer, builder, i);
            builder.append("\n");
        }
        return i;
    }

    public StringBuilder tradeOfferToString(TradeOffer offer, StringBuilder builder, int i) {
        builder.append(i).append(": [");
        appendItemStack(builder, offer.getFirstBuyItem().itemStack());
        if (offer.getSecondBuyItem().isPresent()) {
            builder.append(" ");
            appendItemStack(builder, offer.getSecondBuyItem().get().itemStack());
        }
        builder.append(" -> ");
        appendItemStack(builder, offer.getSellItem());
        builder.append("] x").append(offer.getMaxUses());
        if (!isOfferActive(offer)) {
            builder.append(" (inaktiv)");
        }
        return builder;
    }

    private void appendItemStack(@NonNull StringBuilder builder, @NonNull ItemStack itemStack) {
        builder.append(itemStack.getItem()).append(" x").append(itemStack.getCount());
    }

    /**
     * adds the given trade offer to the list and returns the index of the added trade (starting at 1).
     * If the trade already exists, does nothing and returns -1.
     * @param offer the trade offer to add
     * @return the index of the added trade or -1 if the trade already exists
     */
    public int addTrade(@NonNull TradeOffer offer) {
        if (containsTrade(offer) != -1) {
            return -1;
        }
        Identifier resultItem = Registries.ITEM.getId(offer.getSellItem().getItem());
        trades.computeIfAbsent(resultItem, k -> new LinkedHashSet<>()).add(offer);
        return trades.get(resultItem).size() - 1;
    }

    /**
     * removes the trade with the given result item and index (starting at 1) and returns it.
     * If the trade does not exist, an {@link #TRADE_NOT_FOUND_EXCEPTION} is thrown.
     *
     * @param resultItem the result item of the trade to remove
     * @param tradeIndex the index of the trade to remove (starting at 1)
     * @return the removed trade offer
     * @throws CommandSyntaxException if the trade does not exist
     */
    public TradeOffer removeTrade(@NonNull Identifier resultItem, int tradeIndex) throws CommandSyntaxException {
        Set<TradeOffer> offers = trades.get(resultItem);
        if (offers == null || offers.size() < tradeIndex || tradeIndex <= 0) {
            throw TRADE_NOT_FOUND_EXCEPTION.create(resultItem, tradeIndex);
        }
        Iterator<TradeOffer> iterator = offers.iterator();
        for (int i = 1; i < tradeIndex; i++) {
            iterator.next();
        }
        TradeOffer removed = iterator.next();
        iterator.remove();
        if (offers.isEmpty()) {
            trades.remove(resultItem);
        }
        return removed;
    }

    protected void setTrades(@NonNull JsonElement jsonTrades) {
        trades.clear();
        for (JsonElement element : jsonTrades.getAsJsonArray()) {
            try {
                Optional<Item> resultItem = Registries.ITEM.getOptionalValue(Identifier.of(element.getAsJsonObject().get(RESULT_MATERIAL).getAsString()));
                if (resultItem.isPresent()) {
                    Item item = resultItem.get();
                    TradeOffer offer = readInTradeOffer(element.getAsJsonObject(), item);
                    trades.computeIfAbsent(Registries.ITEM.getId(item), k -> new LinkedHashSet<>()).add(offer);
                } else {
                    logInvalidTrade(element.getAsJsonObject(), "Ungültiges Ergebnis Item: " + element.getAsJsonObject().get(RESULT_MATERIAL));
                }
            } catch (Exception ex) {
                logInvalidTrade(element.getAsJsonObject(), ex.getMessage());
            }
        }
    }

    private static void logInvalidTrade(JsonObject tradeAsJson, String ex) {
        LOGGER.warn("Ungültiger Handel: {}. Handel wird übersprungen. ({})", tradeAsJson, ex);
    }

    /**
     * reads in a trade offer from the given JSON object and result item. If the JSON object is invalid, an exception is thrown.
     *
     * @param tradeAsJson the JSON object representing the trade offer
     * @param resultItem  the result item of the trade offer
     * @return the trade offer represented by the given JSON object and result item
     * @throws NullPointerException if the JSON object is invalid (e.g. missing required fields, invalid material, etc.)
     */
    private TradeOffer readInTradeOffer(JsonObject tradeAsJson, Item resultItem) throws NullPointerException {
        Item firstBuyItem = Registries.ITEM.get(Identifier.of(tradeAsJson.get(INGREDIENT_1_MATERIAL).getAsString()));
        ComponentChanges firstBuyItemComponents = deserializeComponents(tradeAsJson.get(INGREDIENT_1_COMPONENTS));
        int amountFirstBuyItem = tradeAsJson.get(INGREDIENT_1_AMOUNT).getAsInt();
        ItemStack firstBuyStack = new ItemStack(firstBuyItem);
        firstBuyStack.applyChanges(firstBuyItemComponents);
        firstBuyStack.setCount(amountFirstBuyItem);
        TradedItem firstTradedItem = new TradedItem(
                firstBuyStack.getRegistryEntry(), amountFirstBuyItem, ComponentPredicate.of(firstBuyStack.getComponents())
        );

        Optional<TradedItem> secondTradedItem = Optional.empty();
        JsonElement secondBuyItemJson = tradeAsJson.get(INGREDIENT_2_MATERIAL);
        if (secondBuyItemJson != null && !secondBuyItemJson.isJsonNull()) {
            Item secondBuyItem = Registries.ITEM.get(Identifier.of(secondBuyItemJson.getAsString()));
            ComponentChanges secondBuyItemComponents = deserializeComponents(tradeAsJson.get(INGREDIENT_2_COMPONENTS));
            int amountSecondBuyItem = tradeAsJson.get(INGREDIENT_2_AMOUNT).getAsInt();
            ItemStack secondBuyStack = new ItemStack(secondBuyItem);
            secondBuyStack.applyChanges(secondBuyItemComponents);
            secondBuyStack.setCount(amountSecondBuyItem);
            secondTradedItem = Optional.of(new TradedItem(
                    secondBuyStack.getRegistryEntry(), amountSecondBuyItem, ComponentPredicate.of(secondBuyStack.getComponents())
            ));
        }

        int resultAmount = tradeAsJson.get(RESULT_AMOUNT).getAsInt();
        ComponentChanges resultItemComponents = deserializeComponents(tradeAsJson.get(RESULT_COMPONENTS));
        ItemStack resultStack = new ItemStack(resultItem);
        resultStack.applyChanges(resultItemComponents);
        resultStack.setCount(resultAmount);

        int maxUses = tradeAsJson.get(MAX_USES).getAsInt();
        // default to true if missing
        boolean active = !tradeAsJson.has(ACTIVE) || tradeAsJson.get(ACTIVE).getAsBoolean();

        TradeOffer offer = new TradeOffer(firstTradedItem, secondTradedItem, resultStack, maxUses, 1, 0.2F);
        return setOfferActive(offer, active);
    }

    /**
     * checks if the given trade offer is part of the trades of this class and returns its index (starting at 0) or -1 if it is not part of the trades.
     * @param offer the trade offer to check
     * @return the index of the trade offer in the trades of this class (starting at 0) or -1 if it is not part of the trades
     */
    public int containsTrade(@NonNull TradeOffer offer) {
        Set<TradeOffer> offers = trades.get(Registries.ITEM.getId(offer.getSellItem().getItem()));
        if (offers != null) {
            int index = 0;
            for (TradeOffer tradeOffer : offers) {
                if (offersAreEqual(offer, tradeOffer)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    public static boolean offersAreEqual(@NonNull TradeOffer offer, TradeOffer other) {
        Optional<TradedItem> offerSecondBuyItem = offer.getSecondBuyItem();
        Optional<TradedItem> otherSecondBuyItem = other.getSecondBuyItem();
        boolean xor = offerSecondBuyItem.isPresent() ^ otherSecondBuyItem.isPresent();
        return !xor && ItemStack.areEqual(offer.getOriginalFirstBuyItem(), other.getOriginalFirstBuyItem())
                && ItemStack.areEqual(offerSecondBuyItem.orElse(DEFAULT_SECOND_BUY_ITEM).itemStack(),
                    otherSecondBuyItem.orElse(DEFAULT_SECOND_BUY_ITEM).itemStack())
                && ItemStack.areEqual(offer.getSellItem(), other.getSellItem())
                && offer.getMaxUses() == other.getMaxUses()
                && offer.getPriceMultiplier() == other.getPriceMultiplier()
                && offer.getMerchantExperience() == other.getMerchantExperience();
    }

    public abstract void saveToFile();

    protected void saveToFile(@NonNull String fileName) {
        JsonArray jsonTrades = new JsonArray();
        for (Map.Entry<Identifier, LinkedHashSet<TradeOffer>> trade : trades.entrySet()) {
            for (TradeOffer offer : trade.getValue()) {
                JsonObject jsonTrade = new JsonObject();
                jsonTrade.addProperty(RESULT_MATERIAL, Registries.ITEM.getId(offer.getSellItem().getItem()).toString());
                jsonTrade.add(RESULT_COMPONENTS, serializeComponents(offer.getSellItem()));
                jsonTrade.addProperty(RESULT_AMOUNT, offer.getSellItem().getCount());
                jsonTrade.addProperty(INGREDIENT_1_MATERIAL, Registries.ITEM.getId(offer.getOriginalFirstBuyItem().getItem()).toString());
                jsonTrade.add(INGREDIENT_1_COMPONENTS, serializeComponents(offer.getOriginalFirstBuyItem()));
                jsonTrade.addProperty(INGREDIENT_1_AMOUNT, offer.getOriginalFirstBuyItem().getCount());
                if (offer.getSecondBuyItem().isPresent()) {
                    jsonTrade.addProperty(INGREDIENT_2_MATERIAL, Registries.ITEM.getId(offer.getSecondBuyItem().get().itemStack().getItem()).toString());
                    jsonTrade.add(INGREDIENT_2_COMPONENTS, serializeComponents(offer.getSecondBuyItem().get().itemStack()));
                    jsonTrade.addProperty(INGREDIENT_2_AMOUNT, offer.getSecondBuyItem().get().itemStack().getCount());
                }
                jsonTrade.addProperty(MAX_USES, offer.getMaxUses());
                jsonTrade.addProperty(ACTIVE, isOfferActive(offer));
                jsonTrades.add(jsonTrade);
            }
        }
        FileManager.saveToFile(fileName, jsonTrades);
    }

    private JsonElement serializeComponents(ItemStack stack) {
        ComponentChanges componentChanges = stack.getComponentChanges();
        if (componentChanges.isEmpty()) {
            return EMPTY_JSON_ARRAY;
        }
        return ComponentChanges.CODEC.encodeStart(JsonOps.INSTANCE, componentChanges).getOrThrow();
    }

    private ComponentChanges deserializeComponents(JsonElement json) {
        if (json == null || json.isJsonNull() || (json.isJsonArray() && json.getAsJsonArray().isEmpty())) {
            return ComponentChanges.EMPTY;
        }
        return ComponentChanges.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private boolean isOfferActive(@NonNull TradeOffer offer) {
        return !offer.hasBeenUsed();
    }

    public static TradeOffer setOfferActive(@NonNull TradeOffer offer, boolean active) {
        if (active) {
            offer.resetUses();
        } else {
            offer.disable();
        }
        return offer;
    }

    /**
     * returns the trade offer with the given result item and index (starting at 1). If the trade does not exist, an Exception is thrown.
     * @param resultItem the result item of the trade to get
     * @param tradeIndex the index of the trade to get (starting at 1)
     * @return the trade offer with the given result item and index
     * @throws CommandSyntaxException if the trade does not exist
     */
    public TradeOffer getOffer(@NonNull Identifier resultItem, int tradeIndex) throws CommandSyntaxException {
        LinkedHashSet<TradeOffer> offers = trades.get(resultItem);
        if (offers == null || offers.size() < tradeIndex || tradeIndex <= 0) {
            throw TRADE_NOT_FOUND_EXCEPTION.create(resultItem, tradeIndex);
        }
        Iterator<TradeOffer> iterator = offers.iterator();
        for (int i = 1; i < tradeIndex; i++) {
            iterator.next();
        }
        return iterator.next();
    }

    public Set<Identifier> getIdentifiers() {
        return trades.keySet();
    }

    /**
     * returns the TradeOffers with the given Item as the result or an empty set
     * <p>Note: returned TradeOffers should be copied before use to prevent side effects
     * @param item the type of item the player receives from the trade as a result
     * @return the set of TradeOffers with the given result item or an empty set
     */
    public LinkedHashSet<TradeOffer> getTradesWithResult(@NotNull Item item, boolean onlyActive) {
        LinkedHashSet<TradeOffer> result = trades.getOrDefault(Registries.ITEM.getId(item), new LinkedHashSet<>());
        if (onlyActive) {
            result = result.stream().filter(this::isOfferActive).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return result;
    }

    public abstract String getListName();

    public abstract String getListCommandName();

    public static ListEditor getInstance(String listName) {
        return NAME_INSTANCE_MAP.get(listName);
    }

    public static List<ListEditor> getAllEditors() {
        return new ArrayList<>(NAME_INSTANCE_MAP.values());
    }

    public static Set<String> getAllEditorNames() {
        return NAME_INSTANCE_MAP.keySet();
    }
}
