package com.auy.cobbletide.mixin;

import com.auy.cobbletide.fishing.PendingCastCharge;

import com.cobblemon.mod.common.item.interactive.PokerodItem;

import com.li64.tide.Tide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokerodItem.class, remap = false)
public abstract class PokeRodChargeMixin extends FishingRodItem {

    /*
     * Prevents our call to PokerodItem.use() on release
     * from starting another charge.
     *
     * ThreadLocal is important because integrated server and
     * client run on separate threads.
     */
    private static final ThreadLocal<Boolean> COBBLETIDE_BYPASS =
            ThreadLocal.withInitial(() -> false);

    private static final int CHARGE_DURATION = 25;
    private static final int USE_DURATION = 60000;

    protected PokeRodChargeMixin(Item.Properties properties) {
        super(properties);
    }

    /*
     * Replace Cobblemon's immediate cast with charging,
     * but ONLY when there is no bobber already out.
     */
    @Inject(
            method = "use",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobbletide$startCharging(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (COBBLETIDE_BYPASS.get()) {
            return;
        }

        /*
         * Bobber already exists:
         *
         * Do NOT interfere.
         *
         * This lets the existing reel/minigame system continue working.
         */
        if (player.fishing != null) {
            return;
        }

        /*
         * Respect Tide's own Hold To Cast setting.
         */
        if (!Tide.SERVER_CONFIG.general.holdToCast) {
            return;
        }

        player.startUsingItem(hand);

        cir.setReturnValue(
                InteractionResultHolder.consume(
                        player.getItemInHand(hand)
                )
        );
    }

    /*
     * Called when right-click is released.
     */
    @Override
    public void releaseUsing(
            ItemStack rod,
            Level level,
            LivingEntity user,
            int timeLeft
    ) {
        if (!(user instanceof Player player)) {
            return;
        }

        if (player.fishing != null) {
            return;
        }

        if (!Tide.SERVER_CONFIG.general.holdToCast) {
            return;
        }

        int usedTicks =
                USE_DURATION - timeLeft;

        usedTicks = Math.min(
                usedTicks,
                CHARGE_DURATION
        );

        /*
         * Same formula Tide uses:
         *
         * 0 ticks  = 0.5x
         * ~12 ticks = 1.0x
         * 25 ticks = 1.5x
         */
        float chargeMultiplier =
                ((float) usedTicks / (float) CHARGE_DURATION)
                        + 0.5f;

        /*
         * Only the server needs the actual launch multiplier.
         */
        if (!level.isClientSide()) {
            PendingCastCharge.set(
                    player.getUUID(),
                    chargeMultiplier
            );
        }

        InteractionHand hand =
                player.getUsedItemHand();

        /*
         * Now run Cobblemon's ORIGINAL casting code.
         *
         * Our bypass flag prevents our use() injection
         * from starting another charge.
         */
        COBBLETIDE_BYPASS.set(true);

        try {
            ((PokerodItem) (Object) this)
                    .use(level, player, hand);
        }
        finally {
            COBBLETIDE_BYPASS.set(false);
        }
    }

    /*
     * Allows the rod to remain held while charging.
     */
    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return USE_DURATION;
    }

    /*
     * Same animation Tide uses.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}