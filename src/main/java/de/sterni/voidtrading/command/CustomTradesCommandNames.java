package de.sterni.voidtrading.command;

/**
 * Name of all Commands
 */
public interface CustomTradesCommandNames {
    /**
     * use .toString to get the actual Names and Names.getValue(String) to get values using the actual names and ignoring case
     */
    enum Names {
        CUSTOM_TRADE("CustomHandel"),
        TRADE_BLACKLIST("TradeBlackList"),
        ADD_CUSTOM_HANDEL("AddCustomHandel"),
        REMOVE_CUSTOM_HANDEL("RemoveCustomHandel"),
        ADD_TO_BLACKLIST("AddToBlacklist"),
        REMOVE_FROM_BLACKLIST("RemoveFromBlacklist"),
        RELOAD_LISTS("ReloadTradeLists"),
        SAVE_LISTS("SaveLists");

        private final String cmdName;
        Names(String cmdName) {
            this.cmdName = cmdName;
        }

        public static Names getValue(String cmdName) {
            for (Names value : values()) {
                if (value.toString().equalsIgnoreCase(cmdName)) {
                    return value;
                }
            }
            throw new IllegalArgumentException();
        }
        @Override
        public String toString() {
            return cmdName;
        }
    }
}
