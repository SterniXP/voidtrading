package de.sterni.voidtrading.customtrades;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TradeMaterialsEditor extends ListEditor {
    @Getter
    private static final TradeMaterialsEditor instance = new TradeMaterialsEditor();

    public static final String LIST_NAME = "CustomTradesList";
    public static final String FILE_NAME = LIST_NAME+".json";
    public static final String LIST_COMMAND_NAME = "list";

    private TradeMaterialsEditor() {
        super(LIST_NAME);
        loadFromFile();
    }

    @Override
    public void loadFromFile() {
        setTrades(FileManager.loadListFromFile(FILE_NAME, EMPTY_JSON_ARRAY));
    }

    @Override
    public int addTrade(@NonNull TradeOffer offer) {
        int newIndex = super.addTrade(offer);
        int indexBlackList = TradeBlackListEditor.getInstance().containsTrade(offer);
        if (indexBlackList != -1) {
            TradeBlackListEditor.getInstance().removeTrade(Registries.ITEM.getId(offer.getSellItem().getItem()), indexBlackList);
        }
        return newIndex;
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

    public String getListName() {
        return LIST_NAME;
    }

    @Override
    public String getListCommandName() {
        return LIST_COMMAND_NAME;
    }

    @Override
    public void saveToFile() {
        saveToFile(FILE_NAME);
    }
}
