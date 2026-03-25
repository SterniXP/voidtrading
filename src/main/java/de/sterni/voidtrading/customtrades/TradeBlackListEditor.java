package de.sterni.voidtrading.customtrades;

import lombok.NonNull;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

public class TradeBlackListEditor extends ListEditor {
    private static TradeBlackListEditor instance = null;
    public static final String LIST_NAME = "TradeBlacklist";
    public static final String FILE_NAME = LIST_NAME+".json";

    public TradeBlackListEditor() {
        super(LIST_NAME);
        loadFromFile();
    }

    public static TradeBlackListEditor getInstance() {
        if (instance == null) {
            instance = new TradeBlackListEditor();
        }
        return instance;
    }

    @Override
    public void loadFromFile() {
        setTrades(FileManager.loadListFromFile(FILE_NAME, EMPTY_JSON_ARRAY).getAsJsonArray());
    }

    @Override
    public int addTrade(@NonNull TradeOffer offer) {
        int newIndex = super.addTrade(offer);
        int indexMaterials = TradeMaterialsEditor.getInstance().containsTrade(offer);
        if (indexMaterials != -1) {
            TradeMaterialsEditor.getInstance().removeTrade(Registries.ITEM.getId(offer.getSellItem().getItem()), indexMaterials);
        }
        return newIndex;
    }

    public String getListName() {
        return LIST_NAME;
    }

    @Override
    public void saveToFile() {
        saveToFile(FILE_NAME);
    }
}
