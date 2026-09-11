package com.auy.cobbletide.fishing;

import com.auy.cobbletide.CobbleTide;
import com.auy.cobbletide.mixin.PokeBobberAccessor;

import com.cobblemon.mod.common.Cobblemon;
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

    private static final float ALPHA_AREA_MULTIPLIER = 0.85F;

    private static final float TINY_AREA_MULTIPLIER = 1.10F;
    private static final float SMALL_AREA_MULTIPLIER = 1.05F;
    private static final float NORMAL_AREA_MULTIPLIER = 1.00F;
    private static final float LARGE_AREA_MULTIPLIER = 0.95F;
    private static final float HUGE_AREA_MULTIPLIER = 0.90F;

    private static boolean registered = false;

    private CobblemonFishingBridge() {
    }

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

        CobbleTide.LOGGER.debug(
                "Poké Rod cast | Player={} | Charge={}x",
                player.getName().getString(),
                multiplier
        );
    }

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

        if (!hasCatch) {
            return;
        }

        event.cancel();

        if (PendingFishingSessions.isActive(
                player.getUUID()
        )) {
            return;
        }

        SpawnAction<?> plannedSpawn =
                bobber.getPlannedSpawnAction();

        String rarity =
                getRarity(
                        plannedSpawn
                );

        FishingChallengeType challengeType =
                determineChallengeType(
                        plannedSpawn,
                        rarity
                );

        SizeDifficulty sizeDifficulty =
                prepareSizeDifficulty(
                        plannedSpawn,
                        bobber
                );

        FishingSession session =
                PendingFishingSessions.start(
                        player.getUUID(),
                        bobber.getUUID(),
                        rarity,
                        challengeType,
                        sizeDifficulty.scale(),
                        sizeDifficulty.areaMultiplier(),
                        sizeDifficulty.alpha()
                );

        CobbleTide.LOGGER.debug(
                "Fishing challenge created | " +
                        "Player={} | " +
                        "Pokemon={} | " +
                        "Rarity={} | " +
                        "Challenge={} | " +
                        "Rounds={} | " +
                        "Scale={} | " +
                        "SizeCategory={} | " +
                        "AreaMultiplier={} | " +
                        "Alpha={}",
                player.getName().getString(),
                getPlannedSpeciesName(plannedSpawn),
                rarity,
                challengeType,
                session.totalRounds(),
                sizeDifficulty.scale(),
                sizeDifficulty.category(),
                sizeDifficulty.areaMultiplier(),
                sizeDifficulty.alpha()
        );

        if (player instanceof ServerPlayer serverPlayer) {
            FishingChallengeManager.startCurrentRound(
                    serverPlayer,
                    bobber,
                    session
            );
        }
    }

    private static SizeDifficulty prepareSizeDifficulty(
            SpawnAction<?> plannedSpawn,
            PokeRodFishingBobberEntity bobber
    ) {
        if (!(plannedSpawn
                instanceof PokemonSpawnAction pokemonSpawn)) {

            return SizeDifficulty.NORMAL;
        }

        Boolean alphaValue =
                pokemonSpawn
                        .getProps()
                        .isAlpha();

        boolean alpha =
                Boolean.TRUE.equals(
                        alphaValue
                );

        float minimum =
                Cobblemon.INSTANCE
                        .getConfig()
                        .getPokemonIntrinsicSizeMin();

        float maximum =
                Cobblemon.INSTANCE
                        .getConfig()
                        .getPokemonIntrinsicSizeMax();

        Float scale =
                pokemonSpawn
                        .getProps()
                        .getScaleModifier();

        /*
         * Cobblemon normally generates intrinsic scale later when
         * creating the Pokémon.
         *
         * We roll it now so the minigame difficulty can use the exact
         * size that the resulting Pokémon will have.
         */
        if (scale == null) {
            float rawScale =
                    minimum
                            + bobber.getRandom().nextFloat()
                            * (maximum - minimum);

            scale =
                    roundIntrinsicScale(
                            rawScale
                    );

            pokemonSpawn
                    .getProps()
                    .setScaleModifier(
                            scale
                    );
        }

        SizeCategory sizeCategory =
                determineSizeCategory(
                        scale,
                        minimum,
                        maximum
                );

        float sizeMultiplier =
                getAreaMultiplierForCategory(
                        sizeCategory
                );

        /*
         * Alpha is an additional difficulty modifier.
         *
         * It no longer replaces intrinsic size difficulty, meaning:
         *
         * tiny alpha  -> easier size modifier, then alpha penalty
         * huge alpha  -> harder size modifier, then alpha penalty
         */
        float finalMultiplier =
                alpha
                        ? sizeMultiplier * ALPHA_AREA_MULTIPLIER
                        : sizeMultiplier;

        return new SizeDifficulty(
                scale,
                finalMultiplier,
                alpha,
                sizeCategory
        );
    }

    private static float roundIntrinsicScale(
            float scale
    ) {
        float deltaPercent =
                (scale - 1.0F) * 100.0F;

        float roundedPercent =
                Math.round(deltaPercent * 10.0F)
                        / 10.0F;

        return 1.0F
                + roundedPercent / 100.0F;
    }

    private static SizeCategory determineSizeCategory(
            float scale,
            float minimum,
            float maximum
    ) {
        /*
         * Protect against an invalid/custom config where the
         * intrinsic size range has no usable width.
         */
        if (maximum <= minimum) {
            return SizeCategory.NORMAL;
        }

        float normalized =
                (scale - minimum)
                        / (maximum - minimum);

        normalized =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                normalized
                        )
                );

        /*
         * Split whatever intrinsic size range Cobblemon is configured
         * to use into five equal relative categories.
         *
         * 0% - 20%   = tiny
         * 20% - 40%  = small
         * 40% - 60%  = normal
         * 60% - 80%  = large
         * 80% - 100% = huge
         */
        if (normalized < 0.20F) {
            return SizeCategory.TINY;
        }

        if (normalized < 0.40F) {
            return SizeCategory.SMALL;
        }

        if (normalized < 0.60F) {
            return SizeCategory.NORMAL;
        }

        if (normalized < 0.80F) {
            return SizeCategory.LARGE;
        }

        return SizeCategory.HUGE;
    }

    private static float getAreaMultiplierForCategory(
            SizeCategory category
    ) {
        return switch (category) {

            case TINY ->
                    TINY_AREA_MULTIPLIER;

            case SMALL ->
                    SMALL_AREA_MULTIPLIER;

            case NORMAL ->
                    NORMAL_AREA_MULTIPLIER;

            case LARGE ->
                    LARGE_AREA_MULTIPLIER;

            case HUGE ->
                    HUGE_AREA_MULTIPLIER;
        };
    }

    private static FishingChallengeType determineChallengeType(
            SpawnAction<?> plannedSpawn,
            String rarity
    ) {
        /*
         * Mythical and legendary labels override the normal
         * fishing rarity.
         */
        Species species =
                getPlannedSpecies(
                        plannedSpawn
                );

        if (species != null) {
            if (species
                    .getLabels()
                    .contains("mythical")) {

                return FishingChallengeType.MYTHICAL;
            }

            if (species
                    .getLabels()
                    .contains("legendary")) {

                return FishingChallengeType.LEGENDARY;
            }
        }

        if ("ultra-rare".equals(rarity)) {
            return FishingChallengeType.ULTRA_RARE;
        }

        if ("rare".equals(rarity)) {
            return FishingChallengeType.RARE;
        }

        return FishingChallengeType.NORMAL;
    }

    private static Species getPlannedSpecies(
            SpawnAction<?> plannedSpawn
    ) {
        if (!(plannedSpawn
                instanceof PokemonSpawnAction pokemonSpawn)) {

            return null;
        }

        String speciesName =
                pokemonSpawn
                        .getProps()
                        .getSpecies();

        if (speciesName == null
                || speciesName.isBlank()
                || speciesName.equalsIgnoreCase("random")) {

            return null;
        }

        if (!speciesName.contains(":")) {
            return PokemonSpecies.getByName(
                    speciesName
            );
        }

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

    private static String getRarity(
            SpawnAction<?> plannedSpawn
    ) {
        if (plannedSpawn == null) {
            return "item";
        }

        String rarity =
                plannedSpawn.getBucket();

        if (rarity == null
                || rarity.isBlank()) {

            return "custom";
        }

        return rarity.toLowerCase(
                Locale.ROOT
        );
    }

    private static void onPokemonSpawned(
            BobberSpawnPokemonEvent.Post event
    ) {
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
            var pokemon =
                    event
                            .getPokemon()
                            .getPokemon();

            CobbleTide.LOGGER.debug(
                    "Fishing Pokémon spawned | " +
                            "Pokemon={} | " +
                            "Scale={} | " +
                            "ExpectedScale={} | " +
                            "Alpha={}",
                    pokemon.getSpecies().getName(),
                    pokemon.getScaleModifier(),
                    session.pokemonScale(),
                    pokemon.isAlpha()
            );

            if (session.result()
                    == FishingResult.SUCCESS) {

                return;
            }

            if (session.result()
                    == FishingResult.PERFECT) {

                PerfectCatchBonus.apply(
                        player,
                        pokemon,
                        session.rarity()
                );
            }

        } finally {
            PendingFishingSessions.finish(
                    player.getUUID()
            );
        }
    }

    private enum SizeCategory {
        TINY,
        SMALL,
        NORMAL,
        LARGE,
        HUGE
    }

    private record SizeDifficulty(
            float scale,
            float areaMultiplier,
            boolean alpha,
            SizeCategory category
    ) {
        private static final SizeDifficulty NORMAL =
                new SizeDifficulty(
                        1.0F,
                        1.0F,
                        false,
                        SizeCategory.NORMAL
                );
    }
}