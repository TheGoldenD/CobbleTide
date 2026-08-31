package com.auy.cobbletide.mixin;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
        value = PokeRodFishingBobberEntity.class,
        remap = false
)
public interface PokeBobberAccessor {

    @Accessor("caughtFish")
    boolean cobbletide$hasCaughtFish();
}