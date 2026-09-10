package com.auy.cobbletide.fishing;

import com.auy.cobbletide.CobbleTide;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;

import com.li64.tide.Tide;
import com.li64.tide.data.fishing.MinigameBehavior;
import com.li64.tide.network.messages.MinigameClientMsg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FishingChallengeManager {

    private FishingChallengeManager() {
    }

    private static final List<FishingChallengePreset> ULTRA_EARLY =
            List.of(
                    new FishingChallengePreset("ultra_sweep", MinigameBehavior.SINE, 0.20f, 0.085f),
                    new FishingChallengePreset("ultra_linear", MinigameBehavior.LINEAR, 0.19f, 0.090f),
                    new FishingChallengePreset("ultra_plateau", MinigameBehavior.PLATEAU, 0.18f, 0.090f),
                    new FishingChallengePreset("ultra_jitter", MinigameBehavior.JITTER, 0.19f, 0.085f)
            );

    private static final List<FishingChallengePreset> ULTRA_FINAL =
            List.of(
                    new FishingChallengePreset("ultra_darts", MinigameBehavior.DARTS, 0.15f, 0.105f),
                    new FishingChallengePreset("ultra_tight_jitter", MinigameBehavior.JITTER, 0.15f, 0.100f),
                    new FishingChallengePreset("ultra_wrap", MinigameBehavior.LINEAR_WRAP, 0.16f, 0.100f),
                    new FishingChallengePreset("ultra_tight_plateau", MinigameBehavior.PLATEAU, 0.14f, 0.100f)
            );

    private static final List<FishingChallengePreset> BOSS_EARLY =
            List.of(
                    new FishingChallengePreset("sweeping_pull", MinigameBehavior.SINE, 0.20f, 0.090f),
                    new FishingChallengePreset("steady_pull", MinigameBehavior.LINEAR, 0.19f, 0.095f),
                    new FishingChallengePreset("heavy_drag", MinigameBehavior.PLATEAU, 0.19f, 0.090f),
                    new FishingChallengePreset("light_jitter", MinigameBehavior.JITTER, 0.20f, 0.085f)
            );

    private static final List<FishingChallengePreset> BOSS_MID =
            List.of(
                    new FishingChallengePreset("jitter_pulse", MinigameBehavior.JITTER, 0.17f, 0.100f),
                    new FishingChallengePreset("darting_pull", MinigameBehavior.DARTS, 0.17f, 0.105f),
                    new FishingChallengePreset("plateau_surge", MinigameBehavior.PLATEAU, 0.16f, 0.105f),
                    new FishingChallengePreset("wrapping_pull", MinigameBehavior.LINEAR_WRAP, 0.18f, 0.100f)
            );

    private static final List<FishingChallengePreset> BOSS_HARD =
            List.of(
                    new FishingChallengePreset("tight_jitter", MinigameBehavior.JITTER, 0.14f, 0.115f),
                    new FishingChallengePreset("fast_darts", MinigameBehavior.DARTS, 0.14f, 0.120f),
                    new FishingChallengePreset("wrap_strike", MinigameBehavior.LINEAR_WRAP, 0.15f, 0.115f),
                    new FishingChallengePreset("tight_plateau", MinigameBehavior.PLATEAU, 0.13f, 0.110f)
            );

    private static final List<FishingChallengePreset> BOSS_FINAL =
            List.of(
                    new FishingChallengePreset("final_jitter", MinigameBehavior.JITTER, 0.11f, 0.125f),
                    new FishingChallengePreset("final_darts", MinigameBehavior.DARTS, 0.11f, 0.135f),
                    new FishingChallengePreset("final_wrap", MinigameBehavior.LINEAR_WRAP, 0.12f, 0.130f),
                    new FishingChallengePreset("final_plateau", MinigameBehavior.PLATEAU, 0.10f, 0.120f)
            );

    private static final List<FishingChallengePreset> MYTHICAL_FINAL =
            List.of(
                    new FishingChallengePreset("mythical_jitter", MinigameBehavior.JITTER, 0.090f, 0.140f),
                    new FishingChallengePreset("mythical_darts", MinigameBehavior.DARTS, 0.090f, 0.150f),
                    new FishingChallengePreset("mythical_wrap", MinigameBehavior.LINEAR_WRAP, 0.100f, 0.145f),
                    new FishingChallengePreset("mythical_plateau", MinigameBehavior.PLATEAU, 0.085f, 0.135f)
            );

    public static void startCurrentRound(
            ServerPlayer player,
            PokeRodFishingBobberEntity bobber,
            FishingSession session
    ) {
        FishingChallengePreset preset =
                selectPreset(
                        session,
                        bobber.getRandom()
                );

        session.setLastBehaviorName(
                preset.behavior().name()
        );

        float adjustedArea =
                clampArea(
                        preset.area()
                                * session.sizeAreaMultiplier()
                );

        Tide.NETWORK.sendToPlayer(
                new MinigameClientMsg(
                        (byte) 0,
                        (byte) 0,
                        (byte) preset
                                .behavior()
                                .ordinal(),
                        adjustedArea,
                        preset.speed()
                ),
                player
        );

        if (session.challengeType().isMultiRound()) {
            player.displayClientMessage(
                    buildRoundMessage(
                            session
                    ),
                    true
            );
        }

        CobbleTide.LOGGER.debug(
                "Fishing round started | " +
                        "Player={} | " +
                        "Type={} | " +
                        "Round={}/{} | " +
                        "Preset={} | " +
                        "Pattern={} | " +
                        "BaseArea={} | " +
                        "AdjustedArea={} | " +
                        "Scale={} | " +
                        "Speed={}",
                player.getName().getString(),
                session.challengeType(),
                session.currentRound(),
                session.totalRounds(),
                preset.name(),
                preset.behavior(),
                preset.area(),
                adjustedArea,
                session.pokemonScale(),
                preset.speed()
        );
    }

    private static float clampArea(
            float area
    ) {
        return Math.max(
                0.05F,
                Math.min(
                        0.95F,
                        area
                )
        );
    }

    private static FishingChallengePreset selectPreset(
            FishingSession session,
            RandomSource random
    ) {
        if (session.challengeType()
                == FishingChallengeType.NORMAL) {

            return selectNormalPreset(
                    session.rarity(),
                    random
            );
        }

        List<FishingChallengePreset> pool =
                getChallengePool(
                        session
                );

        return pickWithoutRepeatingBehavior(
                pool,
                session.lastBehaviorName(),
                random
        );
    }

    private static FishingChallengePreset selectNormalPreset(
            String rarity,
            RandomSource random
    ) {
        String normalized =
                rarity == null
                        ? "item"
                        : rarity.toLowerCase(
                        Locale.ROOT
                );

        return switch (normalized) {

            case "common" ->
                    new FishingChallengePreset(
                            "common_sine",
                            MinigameBehavior.SINE,
                            0.55f,
                            0.045f
                    );

            case "uncommon" ->
                    new FishingChallengePreset(
                            "uncommon",
                            random.nextBoolean()
                                    ? MinigameBehavior.SINE
                                    : MinigameBehavior.LINEAR,
                            0.42f,
                            0.060f
                    );

            case "rare" ->
                    new FishingChallengePreset(
                            "rare_fallback",
                            MinigameBehavior.DARTS,
                            0.28f,
                            0.080f
                    );

            case "item" ->
                    new FishingChallengePreset(
                            "item",
                            MinigameBehavior.SINE,
                            0.55f,
                            0.045f
                    );

            default ->
                    new FishingChallengePreset(
                            "custom",
                            MinigameBehavior.LINEAR,
                            0.40f,
                            0.065f
                    );
        };
    }

    private static List<FishingChallengePreset> getChallengePool(
            FishingSession session
    ) {
        int round =
                session.currentRound();

        return switch (
                session.challengeType()
                ) {

            case RARE -> {
                if (round == 1) {
                    yield ULTRA_EARLY;
                }

                yield ULTRA_FINAL;
            }

            case ULTRA_RARE -> {
                if (round == 1) {
                    yield ULTRA_EARLY;
                }

                if (round == 2) {
                    yield BOSS_MID;
                }

                yield ULTRA_FINAL;
            }

            case LEGENDARY ->
                    switch (round) {

                        case 1 ->
                                BOSS_EARLY;

                        case 2 ->
                                BOSS_MID;

                        case 3 ->
                                BOSS_HARD;

                        default ->
                                BOSS_FINAL;
                    };

            case MYTHICAL ->
                    switch (round) {

                        case 1 ->
                                BOSS_EARLY;

                        case 2 ->
                                BOSS_MID;

                        case 3 ->
                                BOSS_HARD;

                        case 4 ->
                                BOSS_FINAL;

                        default ->
                                MYTHICAL_FINAL;
                    };

            case NORMAL ->
                    BOSS_EARLY;
        };
    }

    private static FishingChallengePreset pickWithoutRepeatingBehavior(
            List<FishingChallengePreset> pool,
            String previousBehavior,
            RandomSource random
    ) {
        if (previousBehavior == null) {
            return pool.get(
                    random.nextInt(
                            pool.size()
                    )
            );
        }

        List<FishingChallengePreset> candidates =
                new ArrayList<>();

        for (FishingChallengePreset preset : pool) {
            if (!preset
                    .behavior()
                    .name()
                    .equals(
                            previousBehavior
                    )) {

                candidates.add(
                        preset
                );
            }
        }

        if (candidates.isEmpty()) {
            candidates.addAll(
                    pool
            );
        }

        return candidates.get(
                random.nextInt(
                        candidates.size()
                )
        );
    }

    private static Component buildRoundMessage(
            FishingSession session
    ) {
        ChatFormatting color =
                switch (
                        session.challengeType()
                        ) {

                    case RARE ->
                            ChatFormatting.BLUE;

                    case ULTRA_RARE ->
                            ChatFormatting.AQUA;

                    case LEGENDARY ->
                            ChatFormatting.GOLD;

                    case MYTHICAL ->
                            ChatFormatting.LIGHT_PURPLE;

                    default ->
                            ChatFormatting.WHITE;
                };

        String challengeName =
                switch (
                        session.challengeType()
                        ) {

                    case RARE ->
                            "Strong catch";

                    case ULTRA_RARE ->
                            "Powerful catch";

                    case LEGENDARY ->
                            "Legendary struggle";

                    case MYTHICAL ->
                            "Mythical struggle";

                    default ->
                            "Fishing";
                };

        return Component
                .literal(
                        challengeName
                                + "  •  Round "
                                + session.currentRound()
                                + "/"
                                + session.totalRounds()
                )
                .withStyle(
                        color
                );
    }
}