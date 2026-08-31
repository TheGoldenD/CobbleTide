package com.auy.cobbletide.fishing;

import com.li64.tide.data.fishing.MinigameBehavior;

public record FishingChallengePreset(String name, MinigameBehavior behavior, float area, float speed) {
}
