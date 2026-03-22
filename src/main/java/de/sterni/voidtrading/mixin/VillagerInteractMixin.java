package de.sterni.voidtrading.mixin;

import de.sterni.voidtrading.customtrades.TradeEditor;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
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

@Mixin(VillagerEntity.class)
public abstract class VillagerInteractMixin {

    @Shadow
    public abstract VillagerData getVillagerData();

    @Unique
    private final TradeEditor tradeEditor = new TradeEditor();

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void checkTrades(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!player.getWorld().isClient()
                || player.isSneaking()
                || player.getStackInHand(hand).isOf(Items.VILLAGER_SPAWN_EGG)
                || getVillagerData().getProfession().equals(VillagerProfession.NITWIT)
                || getVillagerData().getProfession().equals(VillagerProfession.NONE)) {
            return;
        }
        if (tradeEditor.tryEditTrades((VillagerEntity) (Object) this, player.getStackInHand(hand).getItem())) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
