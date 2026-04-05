package de.sterni.voidtrading.customtrades.inventoryview;

import lombok.NonNull;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ReadOnlyMerchantScreenHandler extends MerchantScreenHandler {
    private final PreviewMerchant merchant;
    private boolean reopening = false;

    public ReadOnlyMerchantScreenHandler(int syncId, PlayerInventory playerInventory, PreviewMerchant merchant) {
        super(syncId, playerInventory, merchant);
        this.merchant = merchant;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // blocking all interactions
    }

    @Override
    public ItemStack quickMove(@NonNull PlayerEntity player, int slot) {
        // blocking
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(@NonNull PlayerEntity player) {
        // Allow viewing only
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (merchant == null || reopening || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        reopening = true;
        MinecraftServer server = serverPlayer.getEntityWorld().getServer();
        if (server != null) {
            server.execute(merchant::returnToMainScreenOnClose);
        }
    }
}
