package de.sterni.voidtrading.customtrades;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class TradeMaterialsEditor extends ListEditor {
    private static TradeMaterialsEditor instance = null;

    public static final String LIST_NAME = "CustomTradesList";
    public static final String FILE_NAME = LIST_NAME+".json";

    public TradeMaterialsEditor() {
        loadFromFile();
    }

    public static TradeMaterialsEditor getInstance() {
        if (instance == null) {
            instance = new TradeMaterialsEditor();
        }
        return instance;
    }

    @Override
    public void loadFromFile() {
        setTrades(FileManager.loadListFromFile(FILE_NAME, EMPTY_JSON_ARRAY));
    }

    /**
     * returns the TradeOffers with the given Item as the result or an empty set
     * <p>Note: returned TradeOffers should be copied before use to prevent side effects
     * @param item the type of item the player receives from the trade as a result
     * @return the set of TradeOffers with the given result item or an empty set
     */
    public LinkedHashSet<TradeOffer> getTradesWithResult(@NotNull Item item) {
        return trades.getOrDefault(item, new LinkedHashSet<>());
    }

    public List<TradeOffer> getCurrentCustomTrades(@NotNull VillagerEntity villager) {
        TradeOfferList offers = villager.getOffers();
        List<TradeOffer> result = new ArrayList<>();
        for (TradeOffer offer : offers) {
            if (containsTrade(offer) != -1) {
                result.add(offer);
            }
        }
        return result;
    }
}
