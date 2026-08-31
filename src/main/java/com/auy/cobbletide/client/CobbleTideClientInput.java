package com.auy.cobbletide.client;

import com.auy.cobbletide.CobbleTide;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;

import com.li64.tide.client.gui.overlays.CatchMinigameOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(
        modid = CobbleTide.MOD_ID,
        value = Dist.CLIENT
)
public final class CobbleTideClientInput {
    private static boolean minigameSeen = false;
    private static boolean waitingForRelease = false;

    private CobbleTideClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        LocalPlayer player =
                minecraft.player;

        if (player == null) {
            reset();
            return;
        }

        boolean active =
                CatchMinigameOverlay.isActive();
        if (!active) {
            reset();
            return;
        }
        if (!minigameSeen) {

            minigameSeen = true;
            waitingForRelease = true;

            CobbleTide.LOGGER.debug(
                    "CobbleTide minigame opened - waiting for right-click release"
            );

            return;
        }
        if (waitingForRelease
                && !minecraft.options.keyUse.isDown()) {

            waitingForRelease = false;

            CobbleTide.LOGGER.debug(
                    "CobbleTide minigame armed"
            );
        }
    }
    @SubscribeEvent
    public static void onInteraction(
            InputEvent.InteractionKeyMappingTriggered event
    ) {

        if (!event.isUseItem()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        LocalPlayer player =
                minecraft.player;

        if (player == null) {
            return;
        }
        if (!CatchMinigameOverlay.isActive()) {
            return;
        }
        if (!(player.fishing
                instanceof PokeRodFishingBobberEntity)) {

            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (!minigameSeen || waitingForRelease) {

            CobbleTide.LOGGER.debug(
                    "Ignoring original Poké Rod reel input"
            );

            return;
        }
        CobbleTide.LOGGER.debug(
                "Tide minigame interaction accepted"
        );

        CatchMinigameOverlay.interact();
    }

    private static void reset() {
        minigameSeen = false;
        waitingForRelease = false;
    }
}