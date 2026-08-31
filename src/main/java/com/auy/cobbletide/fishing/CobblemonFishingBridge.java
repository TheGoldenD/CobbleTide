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

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CobblemonEvents.POKEROD_CAST_PRE.subscribe(Priority.HIGHEST, CobblemonFishingBridge::onPokerodCast);
        CobblemonEvents.POKEROD_REEL.subscribe(Priority.HIGHEST, CobblemonFishingBridge::onPokerodReel);
        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(Priority.NORMAL, CobblemonFishingBridge::onPokemonSpawned);
        CobbleTide.LOGGER.info("Registered Cobblemon fishing compatibility");
    }

    private static void onPokerodCast(PokerodCastEvent.Pre event) {
        PokeRodFishingBobberEntity bobber = event.getBobber();
        if (!(bobber.getOwner() instanceof Player player)) {
            return;
        }
        float multiplier = PendingCastCharge.consume(player.getUUID());
        Vec3 originalVelocity = bobber.getDeltaMovement();
        Vec3 chargedVelocity = originalVelocity.scale(multiplier);
        bobber.setDeltaMovement(chargedVelocity);
        CobbleTide.LOGGER.info(
                "Poké Rod cast | Player={} | Charge={}x | Speed={} -> {}",
                player.getName().getString(),
                String.format("%.2f", multiplier),
                String.format("%.3f", originalVelocity.length()),
                String.format("%.3f", chargedVelocity.length())
        );
    }

    private static void onPokerodReel(PokerodReelEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) {
            return;
        }
        if (!(player.fishing instanceof PokeRodFishingBobberEntity bobber)) {
            return;
        }
        boolean hasCatch = ((PokeBobberAccessor) (Object) bobber).cobbletide$hasCaughtFish();
        if (!hasCatch) {
            CobbleTide.LOGGER.info("Normal Poké Rod reel - nothing has bitten");
            return;
        }
        event.cancel();
        if (PendingFishingSessions.isActive(player.getUUID())) {
            CobbleTide.LOGGER.info(
                    "Ignoring extra reel - fishing challenge already active for {}",
                    player.getName().getString()
            );
            return;
        }
        SpawnAction<?> plannedSpawn = bobber.getPlannedSpawnAction();
        String rarity = getRarity(plannedSpawn);
        FishingChallengeType challengeType = determineChallengeType(plannedSpawn, rarity);
        FishingSession session =
                PendingFishingSessions.start(player.getUUID(), bobber.getUUID(), rarity, challengeType);
        String plannedSpecies = getPlannedSpeciesName(plannedSpawn);
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
        if (player instanceof ServerPlayer serverPlayer) {
            FishingChallengeManager.startCurrentRound(serverPlayer, bobber, session);
        }
    }

    private static FishingChallengeType determineChallengeType(SpawnAction<?> plannedSpawn, String rarity) {
        // Mythical/legendary labels override the normal fishing rarity.
        Species species = getPlannedSpecies(plannedSpawn);
        if (species != null) {
            if (species.getLabels().contains("mythical")) {
                return FishingChallengeType.MYTHICAL;
            }
            if (species.getLabels().contains("legendary")) {
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

    private static Species getPlannedSpecies(SpawnAction<?> plannedSpawn) {
        if (!(plannedSpawn instanceof PokemonSpawnAction pokemonSpawn)) {
            return null;
        }
        String speciesName = pokemonSpawn.getProps().getSpecies();
        if (speciesName == null || speciesName.isBlank() || speciesName.equalsIgnoreCase("random")) {
            return null;
        }
        if (!speciesName.contains(":")) {
            return PokemonSpecies.getByName(speciesName);
        }
        ResourceLocation identifier = ResourceLocation.tryParse(speciesName);
        if (identifier == null) {
            CobbleTide.LOGGER.warn("Could not parse planned Pokémon identifier: {}", speciesName);
            return null;
        }
        return PokemonSpecies.getByIdentifier(identifier);
    }

    private static String getPlannedSpeciesName(SpawnAction<?> plannedSpawn) {
        if (!(plannedSpawn instanceof PokemonSpawnAction pokemonSpawn)) {
            return "item";
        }
        String species = pokemonSpawn.getProps().getSpecies();
        if (species == null || species.isBlank()) {
            return "unknown";
        }
        return species;
    }

    private static String getRarity(SpawnAction<?> plannedSpawn) {
        if (plannedSpawn == null) {
            return "item";
        }
        String rarity = plannedSpawn.getBucket().getName();
        if (rarity == null) {
            return "custom";
        }
        return rarity.toLowerCase(Locale.ROOT);
    }

    private static void onPokemonSpawned(BobberSpawnPokemonEvent.Post event) {
        if (!(event.getBobber().getOwner() instanceof ServerPlayer player)) {
            return;
        }
        FishingSession session = PendingFishingSessions.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!session.bobberId().equals(event.getBobber().getUUID())) {
            return;
        }
        try {
            if (session.result() == FishingResult.SUCCESS) {
                CobbleTide.LOGGER.info(
                        "Fishing challenge completed | " +
                                "Player={} | " +
                                "Pokemon={} | " +
                                "Rarity={} | " +
                                "Challenge={} | " +
                                "Rounds={} | " +
                                "Result=SUCCESS",
                        player.getName().getString(),
                        event.getPokemon().getPokemon().getSpecies().getName(),
                        session.rarity(),
                        session.challengeType(),
                        session.totalRounds()
                );
                return;
            }
            if (session.result() == FishingResult.PERFECT) {
                CobbleTide.LOGGER.info(
                        "Fishing challenge PERFECT | " +
                                "Player={} | " +
                                "Pokemon={} | " +
                                "Rarity={} | " +
                                "Challenge={} | " +
                                "Rounds={}",
                        player.getName().getString(),
                        event.getPokemon().getPokemon().getSpecies().getName(),
                        session.rarity(),
                        session.challengeType(),
                        session.totalRounds()
                );
                PerfectCatchBonus.apply(player, event.getPokemon().getPokemon(), session.rarity());
            }
        } finally {
            PendingFishingSessions.finish(player.getUUID());
        }
    }
}
