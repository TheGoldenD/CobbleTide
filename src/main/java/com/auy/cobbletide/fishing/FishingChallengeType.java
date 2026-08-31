package com.auy.cobbletide.fishing;

public enum FishingChallengeType {
    NORMAL(1),

    RARE(2),

    ULTRA_RARE(3),

    LEGENDARY(4),

    MYTHICAL(5);
    private final int rounds;

    FishingChallengeType(int rounds) {
        this.rounds = rounds;
    }

    public int getRounds() {
        return rounds;
    }

    public boolean isMultiRound() {
        return rounds > 1;
    }
}
