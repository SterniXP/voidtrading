package de.sterni.voidtrading.customtrades;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.village.TradeOffer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TradeEditor {

    private final TradeBlackListEditor blackListEditor = TradeBlackListEditor.getInstance();
    private final TradeMaterialsEditor materialsEditor = TradeMaterialsEditor.getInstance();

    /**
     * Tries to edit the trades of the given villager based on the item in the player's hand.
     * Banned trades will be removed and new trades will be added if the item in hand matches any of the custom trade sell materials.
     * @param villager the villager whose trades should be edited
     * @param handItem the item in the player's hand that is used to determine which trades to add
     * @return true if the trades were ADDED. false if only banned trades were removed or no changes were made
     */
    public boolean tryEditTrades(VillagerEntity villager, Item handItem) {
        removeBannedTrades(villager);
        return cycleTradeFor(villager, handItem);
    }

    private void removeBannedTrades(VillagerEntity villager) {
        villager.getOffers().removeIf(offer -> blackListEditor.containsTrade(offer) != -1);
    }

    private boolean cycleTradeFor(VillagerEntity villager, Item handItem) {
        Set<TradeOffer> newTrades = materialsEditor.getTradesWithResult(handItem);
        if (newTrades.isEmpty()) {
            return false;
        }
        List<TradeOffer> currentTrades = materialsEditor.getCurrentCustomTrades(villager);
        int index = -1;
        for (TradeOffer trade : currentTrades) {
            villager.getOffers().remove(trade);
            int candidate = materialsEditor.containsTrade(trade);
            if (candidate > index) {
                index = candidate;
            }
        }
        if (index + 1 >= newTrades.size()) index = 0;
        Optional<TradeOffer> newTrade = newTrades.stream().skip(index).findFirst();
        if (newTrade.isPresent()) {
            villager.getOffers().add(newTrade.get());
            return true;
        }
        return false;
    }
}
