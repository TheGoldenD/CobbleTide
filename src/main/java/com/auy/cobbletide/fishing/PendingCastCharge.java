package com.auy.cobbletide.fishing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingCastCharge {

    private static final Map<UUID, Float> PENDING =
            new ConcurrentHashMap<>();

    private PendingCastCharge() {
    }

    public static void set(
            UUID playerId,
            float multiplier
    ) {
        PENDING.put(playerId, multiplier);
    }

    public static float consume(UUID playerId) {
        Float multiplier = PENDING.remove(playerId);

        return multiplier != null
                ? multiplier
                : 1.0F;
    }

    public static void remove(UUID playerId) {
        PENDING.remove(playerId);
    }

    public static void clearAll() {
        PENDING.clear();
    }
}