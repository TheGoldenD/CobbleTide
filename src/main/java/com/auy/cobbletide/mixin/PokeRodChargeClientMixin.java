package com.auy.cobbletide.mixin;

import com.cobblemon.mod.common.item.interactive.PokerodItem;

import com.li64.tide.Tide;
import com.li64.tide.client.gui.overlays.CastBarOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PokerodItem.class, remap = false)
public abstract class PokeRodChargeClientMixin
        extends FishingRodItem {
    private static final int CHARGE_DURATION = 25;
    private static final int USE_DURATION = 60000;

    protected PokeRodChargeClientMixin(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack rod, int timeLeft) {
        super.onUseTick(level, user, rod, timeLeft);
        if (!Tide.SERVER_CONFIG.general.holdToCast) {
            return;
        }
        if (user != Minecraft.getInstance().player) {
            return;
        }
        if (user instanceof net.minecraft.world.entity.player.Player player && player.fishing != null) {
            return;
        }
        int usedTicks = USE_DURATION - timeLeft;
        usedTicks = Math.min(usedTicks, CHARGE_DURATION);
        float percent = (float) usedTicks / (float) CHARGE_DURATION;
        CastBarOverlay.rodChargeTick(percent);
    }
}
