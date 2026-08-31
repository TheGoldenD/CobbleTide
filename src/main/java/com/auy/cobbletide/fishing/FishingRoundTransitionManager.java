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
    // About 0.7 seconds at 20 TPS.
    private static final int NEXT_ROUND_DELAY_TICKS = 14;
    private static final Map<UUID, PendingTransition> TRANSITIONS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private FishingRoundTransitionManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(FishingRoundTransitionManager::onServerTick);
        CobbleTide.LOGGER.info("Registered fishing round transition manager");
    }

    public static void scheduleNextRound(ServerPlayer player, FishingSession session) {
        UUID playerId = player.getUUID();
        PendingTransition transition =
                new PendingTransition(session.bobberId(), session.currentRound(), NEXT_ROUND_DELAY_TICKS);
        TRANSITIONS.put(playerId, transition);
        session.setWaitingForNextRound(true);
        CobbleTide.LOGGER.debug(
                "Fishing next round scheduled | " +
                        "Player={} | " +
                        "CurrentRound={} | " +
                        "Delay={} ticks",
                player.getName().getString(),
                session.currentRound(),
                NEXT_ROUND_DELAY_TICKS
        );
    }

    public static void cancel(UUID playerId) {
        TRANSITIONS.remove(playerId);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (TRANSITIONS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        TRANSITIONS.replaceAll((playerId, transition) -> transition.tick());
        for (Map.Entry<UUID, PendingTransition> entry : TRANSITIONS.entrySet()) {
            UUID playerId = entry.getKey();
            PendingTransition transition = entry.getValue();
            if (transition.ticksRemaining() > 0) {
                continue;
            }
            if (!TRANSITIONS.remove(playerId, transition)) {
                continue;
            }
            startNextRound(server, playerId, transition);
        }
    }

    private static void startNextRound(MinecraftServer server, UUID playerId, PendingTransition transition) {
        FishingSession session = PendingFishingSessions.get(playerId);
        if (session == null) {
            return;
        }
        if (!session.waitingForNextRound() || session.currentRound() != transition.completedRound()) {
            return;
        }
        if (!session.bobberId().equals(transition.bobberId())) {
            PendingFishingSessions.finish(playerId);
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            PendingFishingSessions.finish(playerId);
            return;
        }
        if (!(player.fishing instanceof PokeRodFishingBobberEntity bobber)) {
            CobbleTide.LOGGER.warn(
                    "Fishing challenge lost its bobber during round transition | Player={}",
                    player.getName().getString()
            );
            session.setWaitingForNextRound(false);
            PendingFishingSessions.finish(playerId);
            player.fishing = null;
            return;
        }
        if (!bobber.getUUID().equals(session.bobberId())) {
            CobbleTide.LOGGER.warn(
                    "Fishing challenge bobber changed during transition | Player={}",
                    player.getName().getString()
            );
            session.setWaitingForNextRound(false);
            PendingFishingSessions.finish(playerId);
            return;
        }
        session.advanceRound();
        session.setWaitingForNextRound(false);
        CobbleTide.LOGGER.info(
                "Fishing challenge advancing | " +
                        "Player={} | " +
                        "Round={}/{}",
                player.getName().getString(),
                session.currentRound(),
                session.totalRounds()
        );
        FishingChallengeManager.startCurrentRound(player, bobber, session);
    }

    private record PendingTransition(UUID bobberId, int completedRound, int ticksRemaining) {
        private PendingTransition tick() {
            return new PendingTransition(bobberId, completedRound, ticksRemaining - 1);
        }
    }
}
