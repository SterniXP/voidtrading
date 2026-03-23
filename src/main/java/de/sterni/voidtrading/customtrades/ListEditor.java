package de.sterni.voidtrading.customtrades;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.*;

import static de.sterni.voidtrading.VoidTrading.LOGGER;

public abstract class ListEditor {
    protected static final JsonElement EMPTY_JSON_ARRAY = new JsonArray();
    public static final String INGREDIENT_1_MATERIAL = "INGREDIENT_1_MATERIAL";
    public static final String INGREDIENT_1_AMOUNT = "INGREDIENT_1_AMOUNT";
    public static final String INGREDIENT_2_MATERIAL = "INGREDIENT_2_MATERIAL";
    public static final String INGREDIENT_2_AMOUNT = "INGREDIENT_2_AMOUNT";
    public static final String RESULT_MATERIAL = "RESULT_MATERIAL";
    public static final String RESULT_AMOUNT = "RESULT_AMOUNT";
    public static final String MAX_USES = "MAX_USES";
    /**
     * Can be used to update trades, internally uses TradeOffer#uses to store the state (uses == 0 -> active)
     */
    private static final String ACTIVE = "ACTIVE";

    final Map<Identifier, LinkedHashSet<TradeOffer>> trades = new HashMap<>();

    public abstract void loadFromFile();

    public String getTradesAsString(boolean onlyActive) {
        StringBuilder builder = new StringBuilder(200);
        for (Map.Entry<Identifier, LinkedHashSet<TradeOffer>> trade : trades.entrySet()) {
            builder.append(trade.getKey()).append(":\n");
            LinkedHashSet<TradeOffer> offers = trade.getValue();
            int i = 1;
            for (TradeOffer offer : offers) {
                if (onlyActive && !isOfferActive(offer)) continue;
                builder.append(i++).append(": [");
                appendItemStack(builder, offer.getFirstBuyItem().itemStack());
                if (offer.getSecondBuyItem().isPresent()) {
                    builder.append(" ");
                    appendItemStack(builder, offer.getSecondBuyItem().get().itemStack());
                }
                builder.append(" -> ");
                appendItemStack(builder, offer.getSellItem());
                builder.append("] x").append(offer.getMaxUses()).append("\n");
            }
        }
        return builder.toString();
    }

    private void appendItemStack(@NonNull StringBuilder builder, @NonNull ItemStack itemStack) {
        builder.append(itemStack.getCount()).append("x").append(itemStack.getItem());
    }

    public void addTrade(@NonNull TradeOffer offer) {
        Identifier resultItem = Registries.ITEM.getId(offer.getSellItem().getItem());
        trades.computeIfAbsent(resultItem, k -> new LinkedHashSet<>()).add(offer);
    }

    /**
     * removes the first trade with the given result item and returns it. If the trade does not exist, an IllegalArgumentException is thrown.
     *
     * @param resultItem the result item of the trade to remove
     * @return the removed trade offer
     * @throws NoSuchElementException if the trade does not exist
     * @see #removeTrade(Identifier, int) for removing a specific trade with the given result item and index
     */
    public TradeOffer removeTrade(@NonNull Identifier resultItem) throws NoSuchElementException {
        return removeTrade(resultItem, 1);
    }

    /**
     * removes the trade with the given result item and index (starting at 1) and returns it. If the trade does not exist, an IllegalArgumentException is thrown.
     *
     * @param resultItem the result item of the trade to remove
     * @param tradeIndex the index of the trade to remove (starting at 1)
     * @return the removed trade offer
     * @throws NoSuchElementException if the trade does not exist
     */
    public TradeOffer removeTrade(@NonNull Identifier resultItem, int tradeIndex) throws NoSuchElementException {
        Set<TradeOffer> offers = trades.get(resultItem);
        if (offers == null || offers.size() < tradeIndex || tradeIndex <= 0) {
            throw new NoSuchElementException("Der Handel [" + resultItem + ":" + tradeIndex + "] existiert nicht.");
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

    void setTrades(@NonNull JsonElement jsonTrades) {
        trades.clear();
        for (JsonElement element : jsonTrades.getAsJsonArray()) {
            try {
                Optional<Item> resultItem = Registries.ITEM.getOptionalValue(Identifier.of(element.getAsJsonObject().get(RESULT_MATERIAL).getAsString()));
                if (resultItem.isPresent()) {
                    Item item = resultItem.get();
                    TradeOffer offer = readInTradeOffer(element.getAsJsonObject(), item);
                    trades.computeIfAbsent(Registries.ITEM.getId(item), k -> new LinkedHashSet<>()).add(offer);
                } else {
                    logInvalidTrade(element.getAsJsonObject(), "Ungültiges Ergebnis Material: " + element.getAsJsonObject().get(RESULT_MATERIAL));
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
        int amountFirstBuyItem = tradeAsJson.get(INGREDIENT_1_AMOUNT).getAsInt();
        TradedItem firstTradedItem = new TradedItem(firstBuyItem, amountFirstBuyItem);

        Optional<TradedItem> secondTradedItem = Optional.empty();
        JsonElement secondBuyItemJson = tradeAsJson.get(INGREDIENT_2_MATERIAL);
        if (!secondBuyItemJson.isJsonNull()) {
            Item secondBuyItem = Registries.ITEM.get(Identifier.of(secondBuyItemJson.getAsString()));
            if (!Items.AIR.equals(secondBuyItem)) {
                int amountSecondBuyItem = tradeAsJson.get(INGREDIENT_2_AMOUNT).getAsInt();
                secondTradedItem = Optional.of(new TradedItem(secondBuyItem, amountSecondBuyItem));
            }
        }

        // default to true if missing or read value
        boolean active = !tradeAsJson.has(ACTIVE) || tradeAsJson.get(ACTIVE).getAsBoolean();

        int resultAmount = tradeAsJson.get(RESULT_AMOUNT).getAsInt();
        int maxUses = tradeAsJson.get(MAX_USES).getAsInt();

        return new TradeOffer(firstTradedItem, secondTradedItem, new ItemStack(resultItem, resultAmount), maxUses, active?1:0,0.2F);
    }

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

    public boolean offersAreEqual(@NonNull TradeOffer offer, TradeOffer other) {
        Optional<TradedItem> offerSecondBuyItem = offer.getSecondBuyItem();
        Optional<TradedItem> otherSecondBuyItem = other.getSecondBuyItem();
        boolean xor = offerSecondBuyItem.isPresent() ^ otherSecondBuyItem.isPresent();
        return !xor && offer.getOriginalFirstBuyItem().equals(other.getOriginalFirstBuyItem())
                && offerSecondBuyItem.equals(otherSecondBuyItem)
                && offer.getSellItem().equals(other.getSellItem())
                && offer.getMaxUses() == other.getMaxUses()
                && offer.getPriceMultiplier() == other.getPriceMultiplier()
                && offer.getMerchantExperience() == other.getMerchantExperience();
    }

    public void safeToFile(@NonNull String fileName) {
        JsonArray jsonTrades = new JsonArray();
        for (Map.Entry<Identifier, LinkedHashSet<TradeOffer>> trade : trades.entrySet()) {
            for (TradeOffer offer : trade.getValue()) {
                JsonObject jsonTrade = new JsonObject();
                jsonTrade.addProperty(RESULT_MATERIAL, Registries.ITEM.getId(offer.getSellItem().getItem()).toString());
                jsonTrade.addProperty(RESULT_AMOUNT, offer.getSellItem().getCount());
                jsonTrade.addProperty(MAX_USES, offer.getMaxUses());
                jsonTrade.addProperty(ACTIVE, isOfferActive(offer));
                jsonTrade.addProperty(INGREDIENT_1_MATERIAL, Registries.ITEM.getId(offer.getOriginalFirstBuyItem().getItem()).toString());
                jsonTrade.addProperty(INGREDIENT_1_AMOUNT, offer.getOriginalFirstBuyItem().getCount());
                if (offer.getSecondBuyItem().isPresent()) {
                    jsonTrade.addProperty(INGREDIENT_2_MATERIAL, Registries.ITEM.getId(offer.getSecondBuyItem().get().itemStack().getItem()).toString());
                    jsonTrade.addProperty(INGREDIENT_2_AMOUNT, offer.getSecondBuyItem().get().itemStack().getCount());
                }
                jsonTrades.add(jsonTrade);
            }
        }
        FileManager.saveToFile(fileName, jsonTrades);
    }

    private boolean isOfferActive(@NonNull TradeOffer offer) {
        return offer.getUses() == 0;
    }

    public Set<Identifier> getIdentifiers() {
        return trades.keySet();
    }
}
