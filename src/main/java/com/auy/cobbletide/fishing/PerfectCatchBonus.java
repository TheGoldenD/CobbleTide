package com.auy.cobbletide.fishing;

import com.auy.cobbletide.CobbleTide;
import com.auy.cobbletide.config.CobbleTideConfig;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class PerfectCatchBonus {

    private static final int MAX_IV = 31;

    private static final List<Stat> IV_STATS =
            List.of(
                    Stats.HP,
                    Stats.ATTACK,
                    Stats.DEFENCE,
                    Stats.SPECIAL_ATTACK,
                    Stats.SPECIAL_DEFENCE,
                    Stats.SPEED
            );

    private PerfectCatchBonus() {
    }

    /*
     * =========================================================
     * APPLY PERFECT REWARD
     * =========================================================
     */
    public static void apply(
            ServerPlayer player,
            Pokemon pokemon,
            String rarity
    ) {

        int perfectIVBonus =
                rollPerfectIVCount(
                        player.getRandom()
                );

        int ivsAdded =
                addPerfectIVs(
                        pokemon,
                        perfectIVBonus,
                        player.getRandom()
                );

        /*
         * Cobblemon/bait has already performed
         * the normal shiny roll.
         */
        boolean alreadyShiny =
                pokemon.getShiny();

        boolean bonusShiny =
                false;

        int shinyDenominator =
                CobbleTideConfig.INSTANCE
                        .getShinyDenominator(
                                rarity
                        );

        if (!alreadyShiny) {

            bonusShiny =
                    player.getRandom()
                            .nextInt(
                                    shinyDenominator
                            ) == 0;

            if (bonusShiny) {

                pokemon.setShiny(true);
            }
        }

        CobbleTide.LOGGER.info(
                "PERFECT fishing reward | " +
                        "Player={} | " +
                        "Pokemon={} | " +
                        "Rarity={} | " +
                        "IVBonusRolled={} | " +
                        "PerfectIVsAdded={} | " +
                        "AlreadyShiny={} | " +
                        "BonusShiny={} | " +
                        "BonusShinyChance=1/{}",
                player.getName().getString(),
                pokemon.getSpecies().getName(),
                rarity,
                perfectIVBonus,
                ivsAdded,
                alreadyShiny,
                bonusShiny,
                shinyDenominator
        );
    }

    /*
     * =========================================================
     * IV REWARD ROLL
     * =========================================================
     */
    private static int rollPerfectIVCount(
            RandomSource random
    ) {

        CobbleTideConfig config =
                CobbleTideConfig.INSTANCE;

        double roll =
                random.nextDouble()
                        * 100.0;

        double cumulative = 0.0;

        /*
         * 6 perfect IVs.
         */
        cumulative +=
                config.sixPerfectIvChance.get();

        if (roll < cumulative) {
            return 6;
        }

        /*
         * 5 perfect IVs.
         */
        cumulative +=
                config.fivePerfectIvChance.get();

        if (roll < cumulative) {
            return 5;
        }

        /*
         * 4 perfect IVs.
         */
        cumulative +=
                config.fourPerfectIvChance.get();

        if (roll < cumulative) {
            return 4;
        }

        /*
         * 3 perfect IVs.
         */
        cumulative +=
                config.threePerfectIvChance.get();

        if (roll < cumulative) {
            return 3;
        }

        /*
         * 2 perfect IVs.
         */
        cumulative +=
                config.twoPerfectIvChance.get();

        if (roll < cumulative) {
            return 2;
        }

        /*
         * 1 perfect IV.
         */
        cumulative +=
                config.onePerfectIvChance.get();

        if (roll < cumulative) {
            return 1;
        }

        return 0;
    }

    /*
     * =========================================================
     * APPLY IVs
     * =========================================================
     */
    private static int addPerfectIVs(
            Pokemon pokemon,
            int amount,
            RandomSource random
    ) {

        List<Stat> candidates =
                new ArrayList<>();

        /*
         * Existing 31s don't waste the reward.
         */
        for (Stat stat : IV_STATS) {

            int currentIV =
                    pokemon
                            .getIvs()
                            .getOrDefault(stat);

            if (currentIV < MAX_IV) {
                candidates.add(stat);
            }
        }

        int added = 0;

        while (added < amount
                && !candidates.isEmpty()) {

            int index =
                    random.nextInt(
                            candidates.size()
                    );

            Stat selected =
                    candidates.remove(index);

            pokemon.setIV(
                    selected,
                    MAX_IV
            );

            CobbleTide.LOGGER.debug(
                    "Perfect fishing IV upgraded | Pokemon={} | Stat={}",
                    pokemon.getSpecies().getName(),
                    selected.getShowdownId()
            );

            added++;
        }

        return added;
    }
}