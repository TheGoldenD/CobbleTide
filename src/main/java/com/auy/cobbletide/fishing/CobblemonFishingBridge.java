package com.auy.cobbletide.fishing;

import com.auy.cobbletide.CobbleTide;
import com.auy.cobbletide.mixin.PokeBobberAccessor;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.fishing.BobberSpawnPokemonEvent;
import com.cobblemon.mod.common.api.events.fishing.PokerodCastEvent;
import com.cobblemon.mod.common.api.events.fishing.PokerodReelEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;
import com.cobblemon.mod.common.pokemon.Species;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class CobblemonFishingBridge {

    private static boolean registered = false;

    private CobblemonFishingBridge() {
    }

    /*
     * =========================================================
     * REGISTER
     * =========================================================
     */
    public static void register() {

        if (registered) {
            return;
        }

        registered = true;

        CobblemonEvents.POKEROD_CAST_PRE.subscribe(
                Priority.HIGHEST,
                CobblemonFishingBridge::onPokerodCast
        );

        CobblemonEvents.POKEROD_REEL.subscribe(
                Priority.HIGHEST,
                CobblemonFishingBridge::onPokerodReel
        );

        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(
                Priority.NORMAL,
                CobblemonFishingBridge::onPokemonSpawned
        );

        CobbleTide.LOGGER.info(
                "Registered Cobblemon fishing compatibility"
        );
    }

    /*
     * =========================================================
     * CHARGED CAST
     * =========================================================
     */
    private static void onPokerodCast(
            PokerodCastEvent.Pre event
    ) {

        PokeRodFishingBobberEntity bobber =
                event.getBobber();

        if (!(bobber.getOwner() instanceof Player player)) {
            return;
        }

        float multiplier =
                PendingCastCharge.consume(
                        player.getUUID()
                );

        Vec3 originalVelocity =
                bobber.getDeltaMovement();

        Vec3 chargedVelocity =
                originalVelocity.scale(
                        multiplier
                );

        bobber.setDeltaMovement(
                chargedVelocity
        );

        CobbleTide.LOGGER.info(
                "Poké Rod cast | Player={} | Charge={}x | Speed={} -> {}",
                player.getName().getString(),
                String.format(
                        "%.2f",
                        multiplier
                ),
                String.format(
                        "%.3f",
                        originalVelocity.length()
                ),
                String.format(
                        "%.3f",
                        chargedVelocity.length()
                )
        );
    }

    /*
     * =========================================================
     * REEL
     * =========================================================
     */
    private static void onPokerodReel(
            PokerodReelEvent event
    ) {

        Player player =
                event.getPlayer();

        if (player.level().isClientSide()) {
            return;
        }

        if (!(player.fishing
                instanceof PokeRodFishingBobberEntity bobber)) {

            return;
        }

        boolean hasCatch =
                ((PokeBobberAccessor) (Object) bobber)
                        .cobbletide$hasCaughtFish();

        /*
         * Nothing has bitten.
         * Let Cobblemon retract normally.
         */
        if (!hasCatch) {

            CobbleTide.LOGGER.info(
                    "Normal Poké Rod reel - nothing has bitten"
            );

            return;
        }

        /*
         * Tide now controls whether the catch succeeds.
         */
        event.cancel();

        /*
         * Prevent multiple overlapping challenges.
         */
        if (PendingFishingSessions.isActive(
                player.getUUID()
        )) {

            CobbleTide.LOGGER.info(
                    "Ignoring extra reel - fishing challenge already active for {}",
                    player.getName().getString()
            );

            return;
        }

        /*
         * Cobblemon already knows what this bobber
         * intends to spawn.
         */
        SpawnAction<?> plannedSpawn =
                bobber.getPlannedSpawnAction();

        String rarity =
                getRarity(
                        plannedSpawn
                );

        /*
         * Determine:
         *
         * NORMAL      = 1 round
         * RARE        = 2 rounds
         * ULTRA_RARE  = 3 rounds
         * LEGENDARY   = 4 rounds
         * MYTHICAL    = 5 rounds
         */
        FishingChallengeType challengeType =
                determineChallengeType(
                        plannedSpawn,
                        rarity
                );

        FishingSession session =
                PendingFishingSessions.start(
                        player.getUUID(),
                        bobber.getUUID(),
                        rarity,
                        challengeType
                );

        String plannedSpecies =
                getPlannedSpeciesName(
                        plannedSpawn
                );

        CobbleTide.LOGGER.info(
                "Fishing challenge created | " +
                        "Player={} | " +
                        "Pokemon={} | " +
                        "Rarity={} | " +
                        "Challenge={} | " +
                        "Rounds={}",
                player.getName().getString(),
                plannedSpecies,
                rarity,
                challengeType,
                session.totalRounds()
        );

        /*
         * Start round 1.
         */
        if (player instanceof ServerPlayer serverPlayer) {

            FishingChallengeManager.startCurrentRound(
                    serverPlayer,
                    bobber,
                    session
            );
        }
    }

    /*
     * =========================================================
     * CHALLENGE CLASSIFICATION
     * =========================================================
     */
    private static FishingChallengeType determineChallengeType(
            SpawnAction<?> plannedSpawn,
            String rarity
    ) {

        /*
         * Try to identify the planned Pokémon.
         *
         * Legendary / Mythical overrides its ordinary
         * Cobblemon fishing rarity.
         */
        Species species =
                getPlannedSpecies(
                        plannedSpawn
                );

        if (species != null) {

            /*
             * =================================================
             * MYTHICAL
             *
             * 5 rounds
             * =================================================
             *
             * Check this first in case a custom species
             * happens to contain multiple labels.
             */
            if (species
                    .getLabels()
                    .contains(
                            "mythical"
                    )) {

                return FishingChallengeType.MYTHICAL;
            }

            /*
             * =================================================
             * LEGENDARY
             *
             * 4 rounds
             * =================================================
             */
            if (species
                    .getLabels()
                    .contains(
                            "legendary"
                    )) {

                return FishingChallengeType.LEGENDARY;
            }
        }

        /*
         * =====================================================
         * ULTRA RARE
         *
         * 3 rounds
         * =====================================================
         */
        if ("ultra-rare".equals(
                rarity
        )) {

            return FishingChallengeType.ULTRA_RARE;
        }

        /*
         * =====================================================
         * RARE
         *
         * 2 rounds
         * =====================================================
         */
        if ("rare".equals(
                rarity
        )) {

            return FishingChallengeType.RARE;
        }

        /*
         * =====================================================
         * NORMAL
         *
         * Common
         * Uncommon
         * Items
         * Custom buckets
         *
         * 1 round
         * =====================================================
         */
        return FishingChallengeType.NORMAL;
    }

    /*
     * =========================================================
     * GET PLANNED SPECIES
     * =========================================================
     */
    private static Species getPlannedSpecies(
            SpawnAction<?> plannedSpawn
    ) {

        /*
         * Item catches do not use PokemonSpawnAction.
         */
        if (!(plannedSpawn
                instanceof PokemonSpawnAction pokemonSpawn)) {

            return null;
        }

        /*
         * Example:
         *
         * magikarp
         * kyogre
         * mew
         */
        String speciesName =
                pokemonSpawn
                        .getProps()
                        .getSpecies();

        if (speciesName == null
                || speciesName.isBlank()
                || speciesName.equalsIgnoreCase(
                "random"
        )) {

            return null;
        }

        /*
         * =====================================================
         * STANDARD COBBLEMON SPECIES
         * =====================================================
         *
         * Usually something like:
         *
         * kyogre
         * magikarp
         */
        if (!speciesName.contains(":")) {

            return PokemonSpecies.getByName(
                    speciesName
            );
        }

        /*
         * =====================================================
         * NAMESPACED / ADDON SPECIES
         * =====================================================
         *
         * Example:
         *
         * someaddon:custompokemon
         */
        ResourceLocation identifier =
                ResourceLocation.tryParse(
                        speciesName
                );

        if (identifier == null) {

            CobbleTide.LOGGER.warn(
                    "Could not parse planned Pokémon identifier: {}",
                    speciesName
            );

            return null;
        }

        return PokemonSpecies.getByIdentifier(
                identifier
        );
    }

    /*
     * =========================================================
     * PLANNED SPECIES NAME
     * =========================================================
     *
     * Used mainly for logging.
     */
    private static String getPlannedSpeciesName(
            SpawnAction<?> plannedSpawn
    ) {

        if (!(plannedSpawn
                instanceof PokemonSpawnAction pokemonSpawn)) {

            return "item";
        }

        String species =
                pokemonSpawn
                        .getProps()
                        .getSpecies();

        if (species == null
                || species.isBlank()) {

            return "unknown";
        }

        return species;
    }

    /*
     * =========================================================
     * RARITY
     * =========================================================
     */
    private static String getRarity(
            SpawnAction<?> plannedSpawn
    ) {

        /*
         * No Pokémon SpawnAction normally means
         * fishing loot / item.
         */
        if (plannedSpawn == null) {

            return "item";
        }

        String rarity =
                plannedSpawn
                        .getBucket()
                        .getName();

        if (rarity == null) {

            return "custom";
        }

        return rarity.toLowerCase(
                Locale.ROOT
        );
    }

    /*
     * =========================================================
     * ACTUAL POKÉMON SPAWN
     * =========================================================
     */
    private static void onPokemonSpawned(
            BobberSpawnPokemonEvent.Post event
    ) {

        /*
         * Rewards are server-side.
         */
        if (!(event
                .getBobber()
                .getOwner()
                instanceof ServerPlayer player)) {

            return;
        }

        FishingSession session =
                PendingFishingSessions.get(
                        player.getUUID()
                );

        if (session == null) {
            return;
        }

        /*
         * Make sure this Pokémon came from the exact
         * bobber used for the current challenge.
         */
        if (!session
                .bobberId()
                .equals(
                        event
                                .getBobber()
                                .getUUID()
                )) {

            return;
        }

        try {

            /*
             * =================================================
             * SUCCESS
             * =================================================
             *
             * At least one round was ordinary SUCCESS.
             *
             * The Pokémon is caught normally.
             */
            if (session.result()
                    == FishingResult.SUCCESS) {

                CobbleTide.LOGGER.info(
                        "Fishing challenge completed | " +
                                "Player={} | " +
                                "Pokemon={} | " +
                                "Rarity={} | " +
                                "Challenge={} | " +
                                "Rounds={} | " +
                                "Result=SUCCESS",
                        player.getName().getString(),
                        event.getPokemon()
                                .getPokemon()
                                .getSpecies()
                                .getName(),
                        session.rarity(),
                        session.challengeType(),
                        session.totalRounds()
                );

                return;
            }

            /*
             * =================================================
             * PERFECT
             * =================================================
             *
             * Every single challenge round was PERFECT.
             *
             * Apply the configurable:
             *
             * perfect IV bonus
             * perfect shiny bonus
             */
            if (session.result()
                    == FishingResult.PERFECT) {

                CobbleTide.LOGGER.info(
                        "Fishing challenge PERFECT | " +
                                "Player={} | " +
                                "Pokemon={} | " +
                                "Rarity={} | " +
                                "Challenge={} | " +
                                "Rounds={}",
                        player.getName().getString(),
                        event.getPokemon()
                                .getPokemon()
                                .getSpecies()
                                .getName(),
                        session.rarity(),
                        session.challengeType(),
                        session.totalRounds()
                );

                PerfectCatchBonus.apply(
                        player,
                        event.getPokemon()
                                .getPokemon(),
                        session.rarity()
                );
            }

        } finally {

            /*
             * Always clean the completed session.
             */
            PendingFishingSessions.finish(
                    player.getUUID()
            );
        }
    }
}