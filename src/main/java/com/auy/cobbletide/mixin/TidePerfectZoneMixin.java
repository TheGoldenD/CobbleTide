package com.auy.cobbletide.mixin;

import com.cobblemon.mod.common.item.interactive.PokerodItem;
import com.li64.tide.client.gui.overlays.CatchMinigameOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(
        value = CatchMinigameOverlay.class,
        remap = false
)
public abstract class TidePerfectZoneMixin {
    @Shadow
    private static float area;

    @ModifyConstant(
            method = "interact",
            constant = @Constant(floatValue = 0.1F)
    )
    private static float cobbletide$changePerfectZone(float original) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return original;
        }
        boolean usingPokeRod =
                player.getMainHandItem().getItem()
                        instanceof PokerodItem
                        ||
                        player.getOffhandItem().getItem()
                                instanceof PokerodItem;
        if (!usingPokeRod) {
            return original;
        }
        // Keep PERFECT at 30% of the success area, capped at Tide's default 0.1.
        return Math.min(original, area * 0.30F);
    }
}
