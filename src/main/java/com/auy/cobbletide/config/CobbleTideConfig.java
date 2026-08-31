package com.auy.cobbletide.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

public final class CobbleTideConfig {

    public static final CobbleTideConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    /*
     * =========================================================
     * PERFECT SHINY BONUS
     * =========================================================
     */

    public final ModConfigSpec.IntValue commonShinyDenominator;
    public final ModConfigSpec.IntValue uncommonShinyDenominator;
    public final ModConfigSpec.IntValue rareShinyDenominator;
    public final ModConfigSpec.IntValue ultraRareShinyDenominator;

    /*
     * =========================================================
     * PERFECT IV BONUS
     * =========================================================
     */

    public final ModConfigSpec.DoubleValue onePerfectIvChance;
    public final ModConfigSpec.DoubleValue twoPerfectIvChance;
    public final ModConfigSpec.DoubleValue threePerfectIvChance;
    public final ModConfigSpec.DoubleValue fourPerfectIvChance;
    public final ModConfigSpec.DoubleValue fivePerfectIvChance;
    public final ModConfigSpec.DoubleValue sixPerfectIvChance;

    static {

        ModConfigSpec.Builder builder =
                new ModConfigSpec.Builder();

        INSTANCE =
                new CobbleTideConfig(builder);

        SPEC =
                builder.build();
    }

    private CobbleTideConfig(
            ModConfigSpec.Builder builder
    ) {

        /*
         * =====================================================
         * SHINY
         * =====================================================
         */

        builder.comment(
                "Extra shiny roll awarded by a PERFECT fishing catch.",
                "",
                "These numbers are denominators:",
                "1 = guaranteed",
                "2 = 1 in 2",
                "256 = 1 in 256",
                "1024 = 1 in 1024",
                "",
                "Cobblemon's normal shiny chance and bait effects",
                "are processed before this bonus."
        );

        builder.push("perfect_shiny_bonus");

        commonShinyDenominator =
                builder.defineInRange(
                        "common_denominator",
                        1024,
                        1,
                        10_000_000
                );

        uncommonShinyDenominator =
                builder.defineInRange(
                        "uncommon_denominator",
                        768,
                        1,
                        10_000_000
                );

        rareShinyDenominator =
                builder.defineInRange(
                        "rare_denominator",
                        512,
                        1,
                        10_000_000
                );

        ultraRareShinyDenominator =
                builder.defineInRange(
                        "ultra_rare_denominator",
                        256,
                        1,
                        10_000_000
                );

        builder.pop();

        /*
         * =====================================================
         * IV
         * =====================================================
         */

        builder.comment(
                "Perfect-IV reward chances for PERFECT catches.",
                "",
                "These are exclusive percentage chances.",
                "The strongest reward is checked first.",
                "",
                "Default:",
                "1 IV  = 6%",
                "2 IVs = 3%",
                "3 IVs = 1.5%",
                "4 IVs = 0.5%",
                "5 IVs = 0.1%",
                "6 IVs = 0.02%"
        );

        builder.push("perfect_iv_bonus");

        onePerfectIvChance =
                builder.defineInRange(
                        "one_iv_percent",
                        6.0,
                        0.0,
                        100.0
                );

        twoPerfectIvChance =
                builder.defineInRange(
                        "two_iv_percent",
                        3.0,
                        0.0,
                        100.0
                );

        threePerfectIvChance =
                builder.defineInRange(
                        "three_iv_percent",
                        1.5,
                        0.0,
                        100.0
                );

        fourPerfectIvChance =
                builder.defineInRange(
                        "four_iv_percent",
                        0.5,
                        0.0,
                        100.0
                );

        fivePerfectIvChance =
                builder.defineInRange(
                        "five_iv_percent",
                        0.1,
                        0.0,
                        100.0
                );

        sixPerfectIvChance =
                builder.defineInRange(
                        "six_iv_percent",
                        0.02,
                        0.0,
                        100.0
                );

        builder.pop();
    }

    /*
     * =========================================================
     * SHINY HELPER
     * =========================================================
     */

    public int getShinyDenominator(
            String rarity
    ) {

        String normalized =
                rarity == null
                        ? "common"
                        : rarity
                        .trim()
                        .toLowerCase(Locale.ROOT);

        return switch (normalized) {

            case "uncommon" ->
                    uncommonShinyDenominator.get();

            case "rare" ->
                    rareShinyDenominator.get();

            case "ultra-rare" ->
                    ultraRareShinyDenominator.get();

            default ->
                    commonShinyDenominator.get();
        };
    }
}