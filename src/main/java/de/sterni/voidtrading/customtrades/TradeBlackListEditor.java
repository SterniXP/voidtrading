package de.sterni.voidtrading.customtrades;

import lombok.Getter;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

import java.util.Set;

public class TradeBlackListEditor extends ListEditor {
    @Getter
    private static final TradeBlackListEditor instance = new TradeBlackListEditor();
    public static final String LIST_NAME = "TradeBlacklist";
    public static final String FILE_NAME = LIST_NAME+".json";
    public static final String SHORT_NAME = "Banliste";
    public static final String LIST_COMMAND_NAME = "blacklist";

    private TradeBlackListEditor() {
        super(LIST_NAME);
        loadFromFile();
    }

    @Override
    public void loadFromFile() {
        setTrades(FileManager.loadListFromFile(FILE_NAME, EMPTY_JSON_ARRAY).getAsJsonArray());
    }

    public String getListName() {
        return LIST_NAME;
    }

    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    @Override
    public String getListCommandName() {
        return LIST_COMMAND_NAME;
    }

    @Override
    public void saveToFile() {
        saveToFile(FILE_NAME);
    }

    public int containsActiveTrade(TradeOffer checkOffer) {
        Set<TradeOffer> offers = trades.get(Registries.ITEM.getId(checkOffer.getSellItem().getItem()));
        if (offers != null) {
            int index = 0;
            for (TradeOffer tradeOffer : offers) {
                if (isOfferActive(tradeOffer) && offersAreEqual(checkOffer, tradeOffer)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }
}
