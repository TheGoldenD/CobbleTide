package com.auy.cobbletide.fishing;

import java.util.UUID;

public final class FishingSession {

    /*
     * The exact Cobblemon bobber belonging
     * to this fishing challenge.
     */
    private final UUID bobberId;

    /*
     * Cobblemon fishing rarity:
     *
     * item
     * common
     * uncommon
     * rare
     * ultra-rare
     */
    private final String rarity;

    /*
     * NORMAL
     * RARE
     * ULTRA_RARE
     * LEGENDARY
     * MYTHICAL
     */
    private final FishingChallengeType challengeType;

    /*
     * Total number of rounds required.
     */
    private final int totalRounds;

    /*
     * Current round.
     *
     * Starts at 1.
     */
    private int currentRound = 1;

    /*
     * Final catch result.
     *
     * Remains WAITING until the entire
     * challenge has been completed.
     */
    private FishingResult result =
            FishingResult.WAITING;

    /*
     * Starts true.
     *
     * If ANY round is only SUCCESS instead
     * of PERFECT, this becomes false.
     */
    private boolean allRoundsPerfect = true;

    /*
     * Used to prevent the next minigame from
     * choosing the same movement behavior.
     */
    private String lastBehaviorName = null;

    /*
     * True during the short visual/audio transition
     * between one completed round and the next.
     *
     * This also protects against duplicate Tide
     * result packets during the transition.
     */
    private boolean waitingForNextRound = false;

    public FishingSession(
            UUID bobberId,
            String rarity,
            FishingChallengeType challengeType
    ) {

        this.bobberId =
                bobberId;

        this.rarity =
                rarity;

        this.challengeType =
                challengeType;

        this.totalRounds =
                challengeType.getRounds();
    }

    /*
     * =========================================================
     * GETTERS
     * =========================================================
     */

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

    /*
     * =========================================================
     * ROUND RESULT
     * =========================================================
     */

    public void recordRoundResult(
            FishingResult roundResult
    ) {

        /*
         * One ordinary SUCCESS means the overall
         * catch can no longer qualify for PERFECT.
         */
        if (roundResult == FishingResult.SUCCESS) {

            allRoundsPerfect = false;
        }
    }

    /*
     * =========================================================
     * ROUND PROGRESSION
     * =========================================================
     */

    public boolean hasMoreRounds() {

        return currentRound < totalRounds;
    }

    public void advanceRound() {

        if (hasMoreRounds()) {
            currentRound++;
        }
    }

    /*
     * =========================================================
     * FINAL RESULT
     * =========================================================
     */

    public FishingResult calculateFinalResult() {

        return allRoundsPerfect
                ? FishingResult.PERFECT
                : FishingResult.SUCCESS;
    }

    public void setResult(
            FishingResult result
    ) {

        this.result =
                result;
    }

    /*
     * =========================================================
     * PRESET HISTORY
     * =========================================================
     */

    public void setLastBehaviorName(
            String lastBehaviorName
    ) {

        this.lastBehaviorName =
                lastBehaviorName;
    }

    /*
     * =========================================================
     * ROUND TRANSITION
     * =========================================================
     */

    public void setWaitingForNextRound(
            boolean waiting
    ) {

        this.waitingForNextRound =
                waiting;
    }
}