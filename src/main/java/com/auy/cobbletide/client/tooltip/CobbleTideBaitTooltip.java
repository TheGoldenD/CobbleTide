package com.auy.cobbletide.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record CobbleTideBaitTooltip(
        ItemStack bait
) implements TooltipComponent {

    public CobbleTideBaitTooltip {
        bait = bait.copy();
    }
}