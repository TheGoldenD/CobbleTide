package com.auy.cobbletide.fishing;

import com.auy.cobbletide.CobbleTide;
import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FishingRoundTransitionManager {

    private static final int NEXT_ROUND_DELAY_TICKS = 14;

    private static final Map<UUID, PendingTransition> TRANSITIONS =
            new ConcurrentHashMap<>();

    private static boolean registered = false;
    private static long serverTick = 0L;

    private FishingRoundTransitionManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        NeoForge.EVENT_BUS.addListener(
                FishingRoundTransitionManager::onServerTick
        );

        CobbleTide.LOGGER.info(
                "Registered fishing round transition manager"
        );
    }

    public static void scheduleNextRound(
            ServerPlayer player,
            FishingSession session
    ) {
        UUID playerId = player.getUUID();

        PendingTransition transition =
                new PendingTransition(
                        session.bobberId(),
                        session.currentRound(),
                        serverTick + NEXT_ROUND_DELAY_TICKS
                );

        TRANSITIONS.put(
                playerId,
                transition
        );

        session.setWaitingForNextRound(true);
    }

    public static void cancel(UUID playerId) {
        TRANSITIONS.remove(playerId);
    }

    public static void clearAll() {
        TRANSITIONS.clear();
        serverTick = 0L;
    }

    private static void onServerTick(
            ServerTickEvent.Post event
    ) {
        serverTick++;

        if (TRANSITIONS.isEmpty()) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        for (Map.Entry<UUID, PendingTransition> entry
                : TRANSITIONS.entrySet()) {

            UUID playerId =
                    entry.getKey();

            PendingTransition transition =
                    entry.getValue();

            if (serverTick < transition.executeAtTick()) {
                continue;
            }

            if (!TRANSITIONS.remove(
                    playerId,
                    transition
            )) {
                continue;
            }

            startNextRound(
                    server,
                    playerId,
                    transition
            );
        }
    }

    private static void startNextRound(
            MinecraftServer server,
            UUID playerId,
            PendingTransition transition
    ) {
        FishingSession session =
                PendingFishingSessions.get(
                        playerId
                );

        if (session == null) {
            return;
        }

        if (!session.waitingForNextRound()
                || session.currentRound()
                != transition.completedRound()) {
            return;
        }

        if (!session.bobberId().equals(
                transition.bobberId()
        )) {
            cleanupSession(playerId);
            return;
        }

        ServerPlayer player =
                server
                        .getPlayerList()
                        .getPlayer(
                                playerId
                        );

        if (player == null) {
            cleanupSession(playerId);
            return;
        }

        if (!(player.fishing
                instanceof PokeRodFishingBobberEntity bobber)) {

            CobbleTide.LOGGER.debug(
                    "Fishing challenge lost its bobber during transition | Player={}",
                    player.getName().getString()
            );

            cleanupSession(playerId);
            player.fishing = null;
            return;
        }

        if (!bobber.getUUID().equals(
                session.bobberId()
        )) {
            cleanupSession(playerId);
            return;
        }

        session.advanceRound();
        session.setWaitingForNextRound(false);

        FishingChallengeManager.startCurrentRound(
                player,
                bobber,
                session
        );
    }

    private static void cleanupSession(
            UUID playerId
    ) {
        FishingSession session =
                PendingFishingSessions.get(
                        playerId
                );

        if (session != null) {
            session.setWaitingForNextRound(false);
        }

        PendingFishingSessions.finish(
                playerId
        );

        TRANSITIONS.remove(
                playerId
        );
    }

    private record PendingTransition(
            UUID bobberId,
            int completedRound,
            long executeAtTick
    ) {
    }
}