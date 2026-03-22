package de.sterni.voidtrading.customtrades;

public class TradeBlackListEditor extends ListEditor {
    private static TradeBlackListEditor instance = null;
    public static final String LIST_NAME = "TradeBlacklist";
    public static final String FILE_NAME = LIST_NAME+".json";

    public TradeBlackListEditor() {
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
}
