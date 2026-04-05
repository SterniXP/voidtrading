package de.sterni.voidtrading.customtrades.inventoryview;

import lombok.NonNull;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class PreviewMerchant implements Merchant {
    private final ServerPlayerEntity customer;
    private final TradeOfferList offers;
    private final CustomTradesInventoryView inventoryView;
    private final int cameFromPage;

    public PreviewMerchant(@NonNull ServerPlayerEntity customer,
                           @NonNull TradeOfferList offers,
                           CustomTradesInventoryView inventoryView,
                           int cameFromPage) {
        this.customer = customer;
        this.offers = offers;
        this.inventoryView = inventoryView;
        this.cameFromPage = cameFromPage;
    }

    @Override
    public PlayerEntity getCustomer() {
        return customer;
    }

    @Override
    public void setCustomer(PlayerEntity customer) {
        // Preview merchant has a fixed customer.
    }

    @Override
    public TradeOfferList getOffers() {
        return offers;
    }

    @Override
    public void setOffersFromServer(TradeOfferList offers) {
        this.offers.clear();
        this.offers.addAll(offers);
    }

    @Override
    public void trade(TradeOffer offer) {
        // No-op: this screen is for inspection only.
    }

    @Override
    public void onSellingItem(ItemStack stack) {
        // No-op: this screen is for inspection only.
    }

    @Override
    public int getExperience() {
        return 0;
    }

    @Override
    public void setExperienceFromServer(int experience) {
        // No-op.
    }

    @Override
    public boolean isLeveledMerchant() {
        return true;
    }

    @Override
    public boolean isClient() {
        return customer.getEntityWorld().isClient();
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.ENTITY_VILLAGER_YES;
    }

    @Override
    public boolean canInteract(PlayerEntity player) {
        return player == customer;
    }

    public void returnToMainScreenOnClose() {
        inventoryView.openMainPage(customer, cameFromPage);
    }
}
