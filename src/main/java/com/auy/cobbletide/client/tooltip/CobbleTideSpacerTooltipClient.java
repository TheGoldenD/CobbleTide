package com.auy.cobbletide.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class CobbleTideSpacerTooltipClient
        implements ClientTooltipComponent {

    private final int height;

    public CobbleTideSpacerTooltipClient(
            CobbleTideSpacerTooltip tooltip
    ) {
        this.height = tooltip.height();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(Font font) {
        return 0;
    }
}