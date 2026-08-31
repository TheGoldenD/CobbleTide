package com.auy.cobbletide.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CobbleTideBaitTooltipClient
        implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 24;

    // Reuse Tide's own bait-slot sprite.
    private static final ResourceLocation SLOT_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("tide", "bait/slot_background");

    private final ItemStack bait;

    public CobbleTideBaitTooltipClient(CobbleTideBaitTooltip tooltip) {
        this.bait = tooltip.bait();
    }

    @Override
    public int getHeight() {
        return SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.blitSprite(SLOT_BACKGROUND, x, y, SLOT_SIZE, SLOT_SIZE);
        if (bait.isEmpty()) {
            return;
        }
        graphics.renderItem(bait, x + 4, y + 4);
        graphics.renderItemDecorations(font, bait, x + 4, y + 4);
    }
}
