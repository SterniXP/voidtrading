package de.sterni.voidtrading.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FireworkRocketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketItem.class)
public class FireworkRocketItemMixin {
    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;detachAllHeldLeashes(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean preventLeashDetachOnFireworkUse(PlayerEntity instance, PlayerEntity playerEntity) {
        return false; // Prevent detaching leashes when using a firework rocket
    }
}
