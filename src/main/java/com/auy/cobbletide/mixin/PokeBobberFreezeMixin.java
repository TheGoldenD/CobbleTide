package com.auy.cobbletide.mixin;

import com.auy.cobbletide.fishing.PendingFishingSessions;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = PokeRodFishingBobberEntity.class,
        remap = false
)
public abstract class PokeBobberFreezeMixin {

    @Inject(
            method = "tickFishingLogic",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbletide$freezeFishing(
            BlockPos pos,
            CallbackInfo ci
    ) {

        PokeRodFishingBobberEntity bobber =
                (PokeRodFishingBobberEntity)
                        (Object) this;

        if (!(bobber.getOwner()
                instanceof Player player)) {

            return;
        }

        if (PendingFishingSessions.isActive(
                player.getUUID()
        )) {

            /*
             * Prevent Cobblemon's bite timer from expiring
             * while the Tide game is active.
             */
            ci.cancel();
        }
    }
}