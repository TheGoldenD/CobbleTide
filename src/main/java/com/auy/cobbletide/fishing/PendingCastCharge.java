package com.auy.cobbletide.fishing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingCastCharge {

    private static final Map<UUID, Float> CHARGES =
            new ConcurrentHashMap<>();

    private PendingCastCharge() {
    }

    public static void set(UUID playerId, float multiplier) {
        CHARGES.put(playerId, multiplier);
    }

    public static float consume(UUID playerId) {
        Float value = CHARGES.remove(playerId);

        return value != null ? value : 1.0f;
    }
}