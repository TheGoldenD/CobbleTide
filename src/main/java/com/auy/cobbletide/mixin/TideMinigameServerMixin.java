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
    private static void cobbletide$handleResult(MinigameServerMsg message, Player player, CallbackInfo ci) {
        FishingSession session = PendingFishingSessions.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.waitingForNextRound()) {
            CobbleTide.LOGGER.debug(
                    "Ignoring Tide result during fishing round transition | Player={}",
                    player.getName().getString()
            );
            ci.cancel();
            return;
        }
        // Tide result codes: 0 timeout, 1 miss, 2 success, 3 perfect.
        byte result = message.event();
        if (result == 0 || result == 1) {
            String resultName = result == 0 ? "TIMEOUT" : "MISS";
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
            FishingRoundTransitionManager.cancel(player.getUUID());
            PendingFishingSessions.finish(player.getUUID());
            failCatch(player);
            ci.cancel();
            return;
        }
        FishingResult roundResult;
        if (result == 3) {
            roundResult = FishingResult.PERFECT;
        } else if (result == 2) {
            roundResult = FishingResult.SUCCESS;
        } else {
            CobbleTide.LOGGER.warn("Unknown Tide result {} during CobbleTide challenge", result);
            FishingRoundTransitionManager.cancel(player.getUUID());
            PendingFishingSessions.finish(player.getUUID());
            failCatch(player);
            ci.cancel();
            return;
        }
        session.recordRoundResult(roundResult);
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
        if (session.hasMoreRounds()) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                FishingRoundTransitionManager.cancel(player.getUUID());
                PendingFishingSessions.finish(player.getUUID());
                failCatch(player);
                ci.cancel();
                return;
            }
            if (!(player.fishing instanceof PokeRodFishingBobberEntity bobber)) {
                FishingRoundTransitionManager.cancel(player.getUUID());
                PendingFishingSessions.finish(player.getUUID());
                failCatch(player);
                ci.cancel();
                return;
            }
            if (!bobber.getUUID().equals(session.bobberId())) {
                FishingRoundTransitionManager.cancel(player.getUUID());
                PendingFishingSessions.finish(player.getUUID());
                failCatch(player);
                ci.cancel();
                return;
            }
            playRoundCompleteCue(serverPlayer, bobber, session, roundResult);
            FishingRoundTransitionManager.scheduleNextRound(serverPlayer, session);
            ci.cancel();
            return;
        }
        FishingResult finalResult = session.calculateFinalResult();
        session.setResult(finalResult);
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
        completeCatch(player);
        PendingFishingSessions.finish(player.getUUID());
        ci.cancel();
    }

    private static void playRoundCompleteCue(
            ServerPlayer player,
            PokeRodFishingBobberEntity bobber,
            FishingSession session,
            FishingResult roundResult
    ) {
        ServerLevel level = player.serverLevel();
        double x = bobber.getX();
        double y = bobber.getY() + 0.15;
        double z = bobber.getZ();
        level.sendParticles(ParticleTypes.SPLASH, x, y, z, 14, 0.30, 0.12, 0.30, 0.08);
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
        if (roundResult == FishingResult.PERFECT) {
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.15, z, 12, 0.25, 0.20, 0.25, 0.025);
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
                    Component.literal(
                                    "PERFECT!  •  Round "
                                            + session.currentRound()
                                            + "/"
                                            + session.totalRounds()
                                            + " cleared"
                            ).withStyle(ChatFormatting.GOLD),
                    true
            );
            return;
        }
        player.displayClientMessage(
                Component.literal("Round " + session.currentRound() + "/" + session.totalRounds() + " cleared!").withStyle(ChatFormatting.GREEN),
                true
        );
    }

    private static void completeCatch(Player player) {
        if (!(player.fishing instanceof PokeRodFishingBobberEntity bobber)) {
            CobbleTide.LOGGER.warn(
                    "Challenge completed but Cobblemon bobber was missing for {}",
                    player.getName().getString()
            );
            return;
        }
        ItemStack rod;
        EquipmentSlot slot;
        if (player.getMainHandItem().getItem() instanceof PokerodItem) {
            rod = player.getMainHandItem();
            slot = EquipmentSlot.MAINHAND;
        }
        else if (player.getOffhandItem().getItem() instanceof PokerodItem) {
            rod = player.getOffhandItem();
            slot = EquipmentSlot.OFFHAND;
        }
        else {
            CobbleTide.LOGGER.warn(
                    "{} stopped holding a Poké Rod during the fishing challenge",
                    player.getName().getString()
            );
            bobber.discard();
            player.fishing = null;
            return;
        }
        int durabilityDamage = bobber.retrieve(rod);
        rod.hurtAndBreak(durabilityDamage, player, slot);
    }

    private static void failCatch(Player player) {
        CobbleTide.LOGGER.info("Fishing catch escaped | Player={}", player.getName().getString());
        if (player.fishing instanceof PokeRodFishingBobberEntity bobber) {
            bobber.discard();
        }
        player.fishing = null;
    }
}
