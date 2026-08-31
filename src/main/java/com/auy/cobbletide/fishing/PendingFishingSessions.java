package com.auy.cobbletide.fishing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingFishingSessions {

    private static final Map<UUID, FishingSession> ACTIVE =
            new ConcurrentHashMap<>();

    private PendingFishingSessions() {
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    public static FishingSession get(UUID playerId) {
        return ACTIVE.get(playerId);
    }

    public static FishingSession start(
            UUID playerId,
            UUID bobberId,
            String rarity,
            FishingChallengeType challengeType
    ) {
        FishingSession session = new FishingSession(
                bobberId,
                rarity,
                challengeType
        );

        ACTIVE.put(playerId, session);
        return session;
    }

    public static void setResult(
            UUID playerId,
            FishingResult result
    ) {
        FishingSession session = ACTIVE.get(playerId);

        if (session != null) {
            session.setResult(result);
        }
    }

    public static FishingSession finish(UUID playerId) {
        return ACTIVE.remove(playerId);
    }

    public static boolean matchesBobber(
            UUID playerId,
            UUID bobberId
    ) {
        FishingSession session = ACTIVE.get(playerId);

        return session != null
                && session.bobberId().equals(bobberId);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }
}