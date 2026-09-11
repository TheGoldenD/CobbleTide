package com.auy.cobbletide;

import com.auy.cobbletide.config.CobbleTideConfig;
import com.auy.cobbletide.fishing.CobblemonFishingBridge;
import com.auy.cobbletide.fishing.FishingRoundTransitionManager;
import com.auy.cobbletide.fishing.FishingStateCleanup;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CobbleTide.MOD_ID)
public final class CobbleTide {

    public static final String MOD_ID = "cobbletide";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    public CobbleTide(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        LOGGER.info("CobbleTide is loading!");

        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                CobbleTideConfig.SPEC
        );

        /*
         * Register our own NeoForge event handlers immediately.
         * These do not need Cobblemon/Tide initialization.
         */
        FishingRoundTransitionManager.register();
        FishingStateCleanup.register();

        /*
         * Do NOT touch Cobblemon's event API directly from
         * the mod constructor.
         */
        modEventBus.addListener(
                CobbleTide::onCommonSetup
        );
    }

    private static void onCommonSetup(
            FMLCommonSetupEvent event
    ) {
        /*
         * Cross-mod compatibility registration happens
         * after normal mod construction and on the
         * main thread.
         */
        event.enqueueWork(
                CobbleTide::registerCompatibility
        );
    }

    private static void registerCompatibility() {
        CobblemonFishingBridge.register();

        LOGGER.info(
                "CobbleTide compatibility initialized"
        );
    }
}