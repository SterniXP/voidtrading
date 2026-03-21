package de.sterni.voidtrading.mixin;

import de.sterni.voidtrading.logging.VoidTradingLogger;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

import static de.sterni.voidtrading.VoidTrading.CONFIG;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenCloseMixin {
    @Shadow
    @Final
    private Merchant merchant;
    @Unique
    private static final Map<String, Long> playerCooldownMap = new HashMap<>();

    @Inject(method = "onClosed", at = @At("HEAD"))
    private void onTradingScreenClosed(PlayerEntity player, CallbackInfo ci) {
        if (merchant instanceof MerchantEntity merchantEntity
                && !this.merchant.canInteract(player)
                && Math.abs(player.getWorld().getTime()
                - playerCooldownMap.getOrDefault(player.getUuidAsString(), 0L)) >= CONFIG.cooldown()) {

            VoidTradingLogger.logEvent("Trade of Villager (id: {}) at {}:{} reset.",
                    merchantEntity.getId(),
                    merchantEntity.getEntityWorld().getRegistryKey().getValue(),
                    merchantEntity.getBlockPos());

            for (TradeOffer offer : merchantEntity.getOffers()) {
                offer.resetUses();
            }
            playerCooldownMap.put(player.getUuidAsString(), player.getWorld().getTime());
        }
    }
}
