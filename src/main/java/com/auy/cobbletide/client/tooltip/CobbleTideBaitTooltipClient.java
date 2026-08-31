package com.auy.cobbletide.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CobbleTideBaitTooltipClient
        implements ClientTooltipComponent {

    /*
     * Tide's slot_background.png is 24x24.
     */
    private static final int SLOT_SIZE = 24;

    /*
     * If the file inside the Tide jar is:
     *
     * assets/tide/textures/gui/sprites/slot_background.png
     *
     * then this is the correct sprite ID.
     */
    private static final ResourceLocation SLOT_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(
                    "tide",
                    "bait/slot_background"
            );

    private final ItemStack bait;

    public CobbleTideBaitTooltipClient(
            CobbleTideBaitTooltip tooltip
    ) {
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
    public void renderImage(
            Font font,
            int x,
            int y,
            GuiGraphics graphics
    ) {

        /*
         * Render Tide's real 24x24 nine-sliced slot.
         *
         * Minecraft reads the accompanying .mcmeta
         * automatically.
         */
        graphics.blitSprite(
                SLOT_BACKGROUND,
                x,
                y,
                SLOT_SIZE,
                SLOT_SIZE
        );

        if (bait.isEmpty()) {
            return;
        }

        /*
         * Tide's slot has a 4px border.
         *
         * A Minecraft item is 16x16:
         *
         * 24 - 16 = 8
         * 8 / 2 = 4
         *
         * so +4 perfectly centers the item.
         */
        graphics.renderItem(
                bait,
                x + 4,
                y + 4
        );

        graphics.renderItemDecorations(
                font,
                bait,
                x + 4,
                y + 4
        );
    }
}