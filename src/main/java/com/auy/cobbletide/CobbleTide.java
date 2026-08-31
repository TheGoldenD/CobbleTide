package com.auy.cobbletide;

import com.auy.cobbletide.config.CobbleTideConfig;
import com.auy.cobbletide.fishing.CobblemonFishingBridge;
import com.auy.cobbletide.fishing.FishingRoundTransitionManager;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CobbleTide.MOD_ID)
public final class CobbleTide {
    public static final String MOD_ID = "cobbletide";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CobbleTide(ModContainer modContainer) {
        LOGGER.info("CobbleTide is loading!");
        modContainer.registerConfig(ModConfig.Type.COMMON, CobbleTideConfig.SPEC);
        CobblemonFishingBridge.register();
        FishingRoundTransitionManager.register();
    }
}
