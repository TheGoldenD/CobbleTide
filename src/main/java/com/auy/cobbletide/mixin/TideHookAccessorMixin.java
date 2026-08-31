package com.auy.cobbletide.mixin;

import com.cobblemon.mod.common.item.interactive.PokerodItem;

import com.li64.tide.client.gui.overlays.CatchMinigameOverlay;
import com.li64.tide.registries.entities.misc.fishing.HookAccessor;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = HookAccessor.class,
        remap = false
)
public abstract class TideHookAccessorMixin {

    @Inject(
            method = "bobberRemoved",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cobbletide$acceptPokeRod(
            Player player,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (player == null) {
            return;
        }

        if (!CatchMinigameOverlay.isActive()) {
            return;
        }

        /*
         * player.fishing is NOT reliable enough on the client
         * for Cobblemon's custom bobber.
         *
         * Instead, while Tide's overlay is active and the player
         * is holding a PokerodItem, tell Tide that the bobber
         * still exists.
         */
        boolean holdingPokeRod =
                player.getMainHandItem().getItem()
                        instanceof PokerodItem
                        ||
                        player.getOffhandItem().getItem()
                                instanceof PokerodItem;

        if (holdingPokeRod) {
            cir.setReturnValue(false);
        }
    }
}