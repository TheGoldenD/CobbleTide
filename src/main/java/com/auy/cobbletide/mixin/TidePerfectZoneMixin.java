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

    /*
     * Tide's current success-zone size.
     *
     * CobbleTide sends:
     *
     * Common     = 0.55
     * Uncommon   = 0.42
     * Rare       = 0.28
     * Ultra Rare = 0.16
     */
    @Shadow
    private static float area;

    /*
     * Tide normally uses:
     *
     * accuracy < 0.1f
     *
     * for PERFECT.
     *
     * Replace that 0.1 only while a Poké Rod
     * is being used.
     */
    @ModifyConstant(
            method = "interact",
            constant = @Constant(floatValue = 0.1F)
    )
    private static float cobbletide$changePerfectZone(
            float original
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        LocalPlayer player =
                minecraft.player;

        if (player == null) {
            return original;
        }

        /*
         * Don't change normal Tide fishing rods.
         */
        boolean usingPokeRod =
                player.getMainHandItem().getItem()
                        instanceof PokerodItem
                        ||
                        player.getOffhandItem().getItem()
                                instanceof PokerodItem;

        if (!usingPokeRod) {
            return original;
        }

        /*
         * PERFECT = 30% of the successful zone,
         * but never larger than Tide's normal 0.1.
         */
        return Math.min(
                original,
                area * 0.30F
        );
    }
}