package com.auy.cobbletide.fishing;

import java.util.UUID;

public final class FishingSession {

    private final UUID bobberId;
    private final String rarity;
    private final FishingChallengeType challengeType;
    private final int totalRounds;

    private final float pokemonScale;
    private final float sizeAreaMultiplier;
    private final boolean alpha;

    private int currentRound = 1;
    private FishingResult result = FishingResult.WAITING;

    // Stays true only when every completed round was PERFECT.
    private boolean allRoundsPerfect = true;

    private String lastBehaviorName = null;
    private boolean waitingForNextRound = false;

    public FishingSession(
            UUID bobberId,
            String rarity,
            FishingChallengeType challengeType,
            float pokemonScale,
            float sizeAreaMultiplier,
            boolean alpha
    ) {
        this.bobberId = bobberId;
        this.rarity = rarity;
        this.challengeType = challengeType;
        this.totalRounds = challengeType.getRounds();

        this.pokemonScale = pokemonScale;
        this.sizeAreaMultiplier = sizeAreaMultiplier;
        this.alpha = alpha;
    }

    public UUID bobberId() {
        return bobberId;
    }

    public String rarity() {
        return rarity;
    }

    public FishingChallengeType challengeType() {
        return challengeType;
    }

    public int currentRound() {
        return currentRound;
    }

    public int totalRounds() {
        return totalRounds;
    }

    public FishingResult result() {
        return result;
    }

    public boolean allRoundsPerfect() {
        return allRoundsPerfect;
    }

    public String lastBehaviorName() {
        return lastBehaviorName;
    }

    public boolean waitingForNextRound() {
        return waitingForNextRound;
    }

    public float pokemonScale() {
        return pokemonScale;
    }

    public float sizeAreaMultiplier() {
        return sizeAreaMultiplier;
    }

    public boolean alpha() {
        return alpha;
    }

    public void recordRoundResult(
            FishingResult roundResult
    ) {
        if (roundResult == FishingResult.SUCCESS) {
            allRoundsPerfect = false;
        }
    }

    public boolean hasMoreRounds() {
        return currentRound < totalRounds;
    }

    public void advanceRound() {
        if (hasMoreRounds()) {
            currentRound++;
        }
    }

    public FishingResult calculateFinalResult() {
        return allRoundsPerfect
                ? FishingResult.PERFECT
                : FishingResult.SUCCESS;
    }

    public void setResult(
            FishingResult result
    ) {
        this.result = result;
    }

    public void setLastBehaviorName(
            String lastBehaviorName
    ) {
        this.lastBehaviorName = lastBehaviorName;
    }

    public void setWaitingForNextRound(
            boolean waiting
    ) {
        this.waitingForNextRound = waiting;
    }
}