package de.sterni.voidtrading.customtrades;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.village.TradeOffer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.sterni.voidtrading.VoidTrading.CONFIG;

public class TradeEditor {

    @Getter
    private static final TradeEditor instance = new TradeEditor();

    private final TradeBlackListEditor blackListEditor = TradeBlackListEditor.getInstance();
    private final TradeMaterialsEditor materialsEditor = TradeMaterialsEditor.getInstance();

    private TradeEditor() {}

    /**
     * Tries to edit the trades of the given villager based on the item in the player's hand.
     * Banned trades will be removed and new trades will be added if the item in hand matches any of the custom trade sell materials.
     * @param villager the villager whose trades should be edited
     * @param handItem the item in the player's hand that is used to determine which trades to add
     * @return true if the trades were ADDED. false if only banned trades were removed or no changes were made
     */
    public boolean tryEditTrades(VillagerEntity villager, Item handItem) {
        if (handItem.getDefaultStack().isEmpty()) {
            return false;
        }
        if (CONFIG.enableCustomTradeCycling()) {
            return cycleTradeFor(villager, handItem);
        } else {
            return addAllTradesFor(villager, handItem);
        }
    }

    public void removeBannedTrades(VillagerEntity villager) {
        villager.getOffers().removeIf(offer -> blackListEditor.containsTrade(offer) != -1);
    }

    private boolean cycleTradeFor(VillagerEntity villager, Item handItem) {
        Set<TradeOffer> newTrades = materialsEditor.getTradesWithResult(handItem, true);
        if (newTrades.isEmpty()) {
            return false;
        }
        List<TradeOffer> currentCustomTrades = materialsEditor.getCurrentCustomTrades(villager);
        if (newTrades.size() == 1 && currentCustomTrades.stream().anyMatch(
                trade -> ListEditor.offersAreEqual(trade, newTrades.iterator().next()))) {
            return false;
        }
        int index = -1;
        for (TradeOffer trade : currentCustomTrades) {
            villager.getOffers().remove(trade);
            if (trade.getSellItem().getItem().equals(handItem)) {
                int candidate = materialsEditor.containsTrade(trade);
                if (candidate > index) {
                    index = candidate;
                }
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

    private boolean addAllTradesFor(@NonNull VillagerEntity villager, @NonNull Item handItem) {
        Set<TradeOffer> newTrades = materialsEditor.getTradesWithResult(handItem, false);
        if (newTrades.isEmpty()) {
            return false;
        }
        List<TradeOffer> currentCustomTrades = materialsEditor.getCurrentCustomTrades(villager);
        if (tradeOfferListContains(currentCustomTrades, newTrades.stream().toList())) {
            return false;
        }
        villager.getOffers().removeAll(currentCustomTrades);
        villager.getOffers().addAll(newTrades.stream().map(TradeOffer::copy).toList());
        return true;
    }

    private boolean tradeOfferListContains(@NonNull List<TradeOffer> containsOther, @NonNull List<TradeOffer> other) {
        if (containsOther.size() < other.size()) return false;
        return other.stream().allMatch(offer -> containsOther.stream().anyMatch(trade -> ListEditor.offersAreEqual(trade, offer)));
    }
}
