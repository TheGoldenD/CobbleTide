package com.auy.cobbletide.fishing;

public enum FishingChallengeType {

    /*
     * Common / Uncommon
     */
    NORMAL(1),

    /*
     * Rare
     */
    RARE(2),

    /*
     * Ultra Rare
     */
    ULTRA_RARE(3),

    /*
     * Legendary
     */
    LEGENDARY(4),

    /*
     * Mythical
     */
    MYTHICAL(5);

    private final int rounds;

    FishingChallengeType(
            int rounds
    ) {
        this.rounds = rounds;
    }

    public int getRounds() {
        return rounds;
    }

    public boolean isMultiRound() {
        return rounds > 1;
    }
}