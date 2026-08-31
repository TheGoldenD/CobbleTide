package com.auy.cobbletide.mixin;

import com.auy.cobbletide.CobbleTide;

import com.auy.cobbletide.fishing.FishingResult;
import com.auy.cobbletide.fishing.FishingRoundTransitionManager;
import com.auy.cobbletide.fishing.FishingSession;
import com.auy.cobbletide.fishing.PendingFishingSessions;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;
import com.cobblemon.mod.common.item.interactive.PokerodItem;

import com.li64.tide.network.messages.MinigameServerMsg;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = MinigameServerMsg.class,
        remap = false
)
public abstract class TideMinigameServerMixin {

    @Inject(
            method = "handle",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cobbletide$handleResult(
            MinigameServerMsg message,
            Player player,
            CallbackInfo ci
    ) {

        FishingSession session =
                PendingFishingSessions.get(
                        player.getUUID()
                );

        /*
         * Not a CobbleTide minigame.
         *
         * Allow Tide to handle it normally.
         */
        if (session == null) {
            return;
        }

        /*
         * =====================================================
         * ROUND TRANSITION PROTECTION
         * =====================================================
         *
         * The previous round already finished and we're
         * currently showing the confirmation cue.
         *
         * Ignore any accidental duplicate packets.
         */
        if (session.waitingForNextRound()) {

            CobbleTide.LOGGER.debug(
                    "Ignoring Tide result during fishing round transition | Player={}",
                    player.getName().getString()
            );

            ci.cancel();

            return;
        }

        byte result =
                message.event();

        /*
         * =====================================================
         * FAILURE
         * =====================================================
         *
         * Tide:
         *
         * 0 = TIMEOUT
         * 1 = MISS
         */
        if (result == 0
                || result == 1) {

            String resultName =
                    result == 0
                            ? "TIMEOUT"
                            : "MISS";

            CobbleTide.LOGGER.info(
                    "Fishing challenge failed | " +
                            "Player={} | " +
                            "Challenge={} | " +
                            "Round={}/{} | " +
                            "Result={}",
                    player.getName().getString(),
                    session.challengeType(),
                    session.currentRound(),
                    session.totalRounds(),
                    resultName
            );

            FishingRoundTransitionManager.cancel(
                    player.getUUID()
            );

            PendingFishingSessions.finish(
                    player.getUUID()
            );

            failCatch(
                    player
            );

            ci.cancel();

            return;
        }

        /*
         * =====================================================
         * SUCCESS / PERFECT
         * =====================================================
         *
         * Tide:
         *
         * 2 = SUCCESS
         * 3 = PERFECT
         */
        FishingResult roundResult;

        if (result == 3) {

            roundResult =
                    FishingResult.PERFECT;

        } else if (result == 2) {

            roundResult =
                    FishingResult.SUCCESS;

        } else {

            CobbleTide.LOGGER.warn(
                    "Unknown Tide result {} during CobbleTide challenge",
                    result
            );

            FishingRoundTransitionManager.cancel(
                    player.getUUID()
            );

            PendingFishingSessions.finish(
                    player.getUUID()
            );

            failCatch(
                    player
            );

            ci.cancel();

            return;
        }

        /*
         * Record whether this round was merely SUCCESS
         * or a PERFECT.
         */
        session.recordRoundResult(
                roundResult
        );

        CobbleTide.LOGGER.info(
                "Fishing challenge round complete | " +
                        "Player={} | " +
                        "Challenge={} | " +
                        "Round={}/{} | " +
                        "Result={} | " +
                        "StillPerfect={}",
                player.getName().getString(),
                session.challengeType(),
                session.currentRound(),
                session.totalRounds(),
                roundResult,
                session.allRoundsPerfect()
        );

        /*
         * =====================================================
         * MORE ROUNDS REMAIN
         * =====================================================
         *
         * Instead of immediately opening another Tide
         * minigame:
         *
         * 1. Play particles.
         * 2. Play sound.
         * 3. Show action-bar feedback.
         * 4. Wait ~0.7 seconds.
         * 5. Start the next randomized round.
         */
        if (session.hasMoreRounds()) {

            if (!(player
                    instanceof ServerPlayer serverPlayer)) {

                FishingRoundTransitionManager.cancel(
                        player.getUUID()
                );

                PendingFishingSessions.finish(
                        player.getUUID()
                );

                failCatch(
                        player
                );

                ci.cancel();

                return;
            }

            if (!(player.fishing
                    instanceof PokeRodFishingBobberEntity bobber)) {

                FishingRoundTransitionManager.cancel(
                        player.getUUID()
                );

                PendingFishingSessions.finish(
                        player.getUUID()
                );

                failCatch(
                        player
                );

                ci.cancel();

                return;
            }

            /*
             * Make sure this is still the exact bobber
             * belonging to the challenge.
             */
            if (!bobber
                    .getUUID()
                    .equals(
                            session.bobberId()
                    )) {

                FishingRoundTransitionManager.cancel(
                        player.getUUID()
                );

                PendingFishingSessions.finish(
                        player.getUUID()
                );

                failCatch(
                        player
                );

                ci.cancel();

                return;
            }

            /*
             * Play immediate hit-confirmation feedback.
             */
            playRoundCompleteCue(
                    serverPlayer,
                    bobber,
                    session,
                    roundResult
            );

            /*
             * Do NOT increment the round here.
             *
             * FishingRoundTransitionManager does it
             * after the 0.7-second pause.
             */
            FishingRoundTransitionManager.scheduleNextRound(
                    serverPlayer,
                    session
            );

            ci.cancel();

            return;
        }

        /*
         * =====================================================
         * FINAL ROUND COMPLETE
         * =====================================================
         */

        FishingResult finalResult =
                session.calculateFinalResult();

        session.setResult(
                finalResult
        );

        CobbleTide.LOGGER.info(
                "Fishing challenge fully completed | " +
                        "Player={} | " +
                        "Challenge={} | " +
                        "Rounds={} | " +
                        "FinalResult={}",
                player.getName().getString(),
                session.challengeType(),
                session.totalRounds(),
                finalResult
        );

        /*
         * Only after ALL rounds have succeeded do we
         * allow Cobblemon's real retrieval.
         */
        completeCatch(
                player
        );

        /*
         * Pokémon catches normally clean the session in
         * BOBBER_SPAWN_POKEMON_POST.
         *
         * This is also cleanup for item catches.
         */
        PendingFishingSessions.finish(
                player.getUUID()
        );

        ci.cancel();
    }

    /*
     * =========================================================
     * ROUND COMPLETE VISUAL / AUDIO CUE
     * =========================================================
     */

    private static void playRoundCompleteCue(
            ServerPlayer player,
            PokeRodFishingBobberEntity bobber,
            FishingSession session,
            FishingResult roundResult
    ) {

        ServerLevel level =
                player.serverLevel();

        double x =
                bobber.getX();

        double y =
                bobber.getY() + 0.15;

        double z =
                bobber.getZ();

        /*
         * =====================================================
         * BASE SPLASH
         * =====================================================
         *
         * Every successful round creates a visible
         * splash around the bobber.
         */
        level.sendParticles(
                ParticleTypes.SPLASH,
                x,
                y,
                z,
                14,
                0.30,
                0.12,
                0.30,
                0.08
        );

        /*
         * Fishing-style confirmation sound.
         *
         * null = broadcast from the server,
         * including the player who caught it.
         */
        level.playSound(
                null,
                x,
                y,
                z,
                SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.PLAYERS,
                0.9F,
                roundResult == FishingResult.PERFECT
                        ? 1.20F
                        : 1.05F
        );

        /*
         * =====================================================
         * PERFECT
         * =====================================================
         *
         * Give PERFECT a noticeably different cue.
         */
        if (roundResult
                == FishingResult.PERFECT) {

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x,
                    y + 0.15,
                    z,
                    12,
                    0.25,
                    0.20,
                    0.25,
                    0.025
            );

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.75F,
                    1.35F
            );

            player.displayClientMessage(
                    Component
                            .literal(
                                    "PERFECT!  •  Round "
                                            + session.currentRound()
                                            + "/"
                                            + session.totalRounds()
                                            + " cleared"
                            )
                            .withStyle(
                                    ChatFormatting.GOLD
                            ),
                    true
            );

            return;
        }

        /*
         * =====================================================
         * NORMAL SUCCESS
         * =====================================================
         */

        player.displayClientMessage(
                Component
                        .literal(
                                "Round "
                                        + session.currentRound()
                                        + "/"
                                        + session.totalRounds()
                                        + " cleared!"
                        )
                        .withStyle(
                                ChatFormatting.GREEN
                        ),
                true
        );
    }

    /*
     * =========================================================
     * COMPLETE COBBLEMON CATCH
     * =========================================================
     */

    private static void completeCatch(
            Player player
    ) {

        if (!(player.fishing
                instanceof PokeRodFishingBobberEntity bobber)) {

            CobbleTide.LOGGER.warn(
                    "Challenge completed but Cobblemon bobber was missing for {}",
                    player.getName().getString()
            );

            return;
        }

        ItemStack rod;

        EquipmentSlot slot;

        /*
         * Main hand Poké Rod.
         */
        if (player
                .getMainHandItem()
                .getItem()
                instanceof PokerodItem) {

            rod =
                    player.getMainHandItem();

            slot =
                    EquipmentSlot.MAINHAND;
        }

        /*
         * Offhand Poké Rod.
         */
        else if (player
                .getOffhandItem()
                .getItem()
                instanceof PokerodItem) {

            rod =
                    player.getOffhandItem();

            slot =
                    EquipmentSlot.OFFHAND;
        }

        else {

            CobbleTide.LOGGER.warn(
                    "{} stopped holding a Poké Rod during the fishing challenge",
                    player.getName().getString()
            );

            bobber.discard();

            player.fishing =
                    null;

            return;
        }

        /*
         * Cobblemon's real retrieval.
         *
         * This happens exactly once, after ALL
         * required challenge rounds.
         */
        int durabilityDamage =
                bobber.retrieve(
                        rod
                );

        rod.hurtAndBreak(
                durabilityDamage,
                player,
                slot
        );
    }

    /*
     * =========================================================
     * FAIL ENTIRE CATCH
     * =========================================================
     */

    private static void failCatch(
            Player player
    ) {

        CobbleTide.LOGGER.info(
                "Fishing catch escaped | Player={}",
                player.getName().getString()
        );

        if (player.fishing
                instanceof PokeRodFishingBobberEntity bobber) {

            bobber.discard();
        }

        player.fishing =
                null;
    }
}