package de.sterni.voidtrading.customtrades;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

public class TradeBlackListEditor extends ListEditor {
    @Getter
    private static final TradeBlackListEditor instance = new TradeBlackListEditor();
    public static final String LIST_NAME = "TradeBlacklist";
    public static final String FILE_NAME = LIST_NAME+".json";
    public static final String LIST_COMMAND_NAME = "blacklist";

    private TradeBlackListEditor() {
        super(LIST_NAME);
        loadFromFile();
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
    public String getListCommandName() {
        return LIST_COMMAND_NAME;
    }

    @Override
    public void saveToFile() {
        saveToFile(FILE_NAME);
    }
}
