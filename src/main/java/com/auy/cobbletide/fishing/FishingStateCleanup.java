package com.auy.cobbletide.fishing;

import com.auy.cobbletide.CobbleTide;

import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.UUID;

public final class FishingStateCleanup {

    private static boolean registered = false;

    private FishingStateCleanup() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        NeoForge.EVENT_BUS.addListener(
                FishingStateCleanup::onPlayerLoggedOut
        );

        NeoForge.EVENT_BUS.addListener(
                FishingStateCleanup::onPlayerRespawn
        );

        NeoForge.EVENT_BUS.addListener(
                FishingStateCleanup::onPlayerChangedDimension
        );

        NeoForge.EVENT_BUS.addListener(
                FishingStateCleanup::onServerStopped
        );

        CobbleTide.LOGGER.info(
                "Registered fishing state cleanup"
        );
    }

    private static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        cleanupPlayer(
                event.getEntity()
        );
    }

    private static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        cleanupPlayer(
                event.getEntity()
        );
    }

    private static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        cleanupPlayer(
                event.getEntity()
        );
    }

    private static void onServerStopped(
            ServerStoppedEvent event
    ) {
        PendingFishingSessions.clearAll();
        PendingCastCharge.clearAll();
        FishingRoundTransitionManager.clearAll();
    }

    private static void cleanupPlayer(
            Player player
    ) {
        UUID playerId =
                player.getUUID();

        PendingFishingSessions.finish(
                playerId
        );

        PendingCastCharge.remove(
                playerId
        );

        FishingRoundTransitionManager.cancel(
                playerId
        );

        if (player.fishing != null) {
            player.fishing = null;
        }
    }
}