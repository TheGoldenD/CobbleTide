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
                        "AreaMultiplier={} | " +
                        "Alpha={}",
                player.getName().getString(),
                getPlannedSpeciesName(plannedSpawn),
                rarity,
                challengeType,
                session.totalRounds(),
                sizeDifficulty.scale(),
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

        if (alpha) {
            return new SizeDifficulty(
                    1.0F,
                    0.85F,
                    true
            );
        }

        Float scale =
                pokemonSpawn
                        .getProps()
                        .getScaleModifier();

        if (scale == null) {
            float minimum =
                    Cobblemon.INSTANCE
                            .getConfig()
                            .getPokemonIntrinsicSizeMin();

            float maximum =
                    Cobblemon.INSTANCE
                            .getConfig()
                            .getPokemonIntrinsicSizeMax();

            float rawScale =
                    minimum
                            + bobber.getRandom().nextFloat()
                            * (maximum - minimum);

            scale =
                    roundIntrinsicScale(
                            rawScale
                    );

            /*
             * Setting this now is important.
             *
             * Cobblemon only rolls intrinsic scale when
             * PokemonProperties.scaleModifier is null.
             * Therefore the Pokémon we eventually reel up
             * keeps this exact size.
             */
            pokemonSpawn
                    .getProps()
                    .setScaleModifier(
                            scale
                    );
        }

        return new SizeDifficulty(
                scale,
                getAreaMultiplierForScale(scale),
                false
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

    private static float getAreaMultiplierForScale(
            float scale
    ) {
        if (scale <= 0.96F) {
            return 1.08F;
        }

        if (scale < 0.99F) {
            return 1.04F;
        }

        if (scale <= 1.01F) {
            return 1.00F;
        }

        if (scale < 1.04F) {
            return 0.96F;
        }

        return 0.92F;
    }

    private static FishingChallengeType determineChallengeType(
            SpawnAction<?> plannedSpawn,
            String rarity
    ) {
        // Mythical/legendary labels override the normal fishing rarity.
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

    private record SizeDifficulty(
            float scale,
            float areaMultiplier,
            boolean alpha
    ) {
        private static final SizeDifficulty NORMAL =
                new SizeDifficulty(
                        1.0F,
                        1.0F,
                        false
                );
    }
}