package de.sterni.voidtrading.mixin;

import de.sterni.voidtrading.customtrades.TradeEditor;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.sterni.voidtrading.VoidTrading.CONFIG;

@Mixin(VillagerEntity.class)
public abstract class VillagerInteractMixin {

    @Shadow
    public abstract VillagerData getVillagerData();

    @Unique
    private final TradeEditor tradeEditor = new TradeEditor();

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void checkTrades(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.getWorld().isClient()
                || player.isSneaking()
                || player.getStackInHand(hand).isOf(Items.VILLAGER_SPAWN_EGG)
                || getVillagerData().getProfession().equals(VillagerProfession.NITWIT)
                || getVillagerData().getProfession().equals(VillagerProfession.NONE)) {
            return;
        }
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (tradeEditor.tryEditTrades(villager, player.getStackInHand(hand).getItem())) {
            if (CONFIG.consumeItemOnTradeChange()) {
                player.getStackInHand(hand).decrementUnlessCreative(1, player);
            }
            villager.playSound(villager.getYesSound());
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
