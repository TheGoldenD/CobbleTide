package com.auy.cobbletide.client.tooltip;

import com.auy.cobbletide.CobbleTide;

import com.cobblemon.mod.common.item.components.RodBaitComponent;
import com.cobblemon.mod.common.item.interactive.PokerodItem;

import com.mojang.datafixers.util.Either;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(
        modid = CobbleTide.MOD_ID,
        value = Dist.CLIENT
)
public final class CobbleTideRodTooltipHandler {
    private static final String COBBLEMON_BOBBER = "cobblemon.pokerod.bobber";
    private static final String COBBLEMON_BAIT = "cobblemon.pokerod.bait";
    private static final String COBBLEMON_APPLY_BAIT = "cobblemon.pokerod.apply";
    private static final String COBBLEMON_REMOVE_BAIT = "cobblemon.pokerod.remove";
    private static final String COBBLETIDE_BAIT_SLOTS = "cobbletide.tooltip.bait_slots";
    private static final String COBBLETIDE_ACCESSORIES = "cobbletide.tooltip.accessories";

    private CobbleTideRodTooltipHandler() {
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack rod = event.getItemStack();
        if (!(rod.getItem() instanceof PokerodItem)) {
            return;
        }
        ItemStack bait = getBaitStack(rod);
        List<Either<FormattedText, TooltipComponent>> original = event.getTooltipElements();
        List<Either<FormattedText, TooltipComponent>> rebuilt = new ArrayList<>();
        String expectedBaitLine =
                bait.isEmpty()
                        ? null
                        : Component.translatable(COBBLEMON_BAIT, bait.getHoverName(), bait.getCount()).getString();
        String applyBaitLine = Component.translatable(COBBLEMON_APPLY_BAIT).getString();
        String removeBaitLine = Component.translatable(COBBLEMON_REMOVE_BAIT).getString();
        boolean firstTextLineFound = false;
        boolean baitSectionInserted = false;
        boolean accessoriesInserted = false;
        for (Either<FormattedText, TooltipComponent> element : original) {
            if (element.right().isPresent()) {
                rebuilt.add(element);
                continue;
            }
            FormattedText text = element.left().orElse(null);
            if (text == null) {
                rebuilt.add(element);
                continue;
            }
            String rendered = text.getString();
            if (!firstTextLineFound) {
                firstTextLineFound = true;
                rebuilt.add(element);
                addBaitSection(rebuilt, bait);
                baitSectionInserted = true;
                continue;
            }
            String bobberName = extractTranslatedArgument(rendered, COBBLEMON_BOBBER);
            if (bobberName != null) {
                rebuilt.add(Either.right(new CobbleTideSpacerTooltip(3)));
                addAccessoriesSection(rebuilt, bobberName);
                accessoriesInserted = true;
                continue;
            }
            if (expectedBaitLine != null && rendered.equals(expectedBaitLine)) {
                continue;
            }
            if (rendered.equals(applyBaitLine) || rendered.equals(removeBaitLine)) {
                continue;
            }
            rebuilt.add(element);
        }
        if (!baitSectionInserted) {
            addBaitSection(rebuilt, bait);
        }
        if (!accessoriesInserted) {
            rebuilt.add(Either.right(new CobbleTideSpacerTooltip(3)));
            rebuilt.add(Either.left(Component.translatable(COBBLETIDE_ACCESSORIES).withStyle(ChatFormatting.GRAY)));
        }
        original.clear();
        original.addAll(rebuilt);
    }

    private static void addBaitSection(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack bait) {
        tooltip.add(Either.left(Component.translatable(COBBLETIDE_BAIT_SLOTS).withStyle(ChatFormatting.GRAY)));
        tooltip.add(Either.right(new CobbleTideBaitTooltip(bait)));
        tooltip.add(Either.right(new CobbleTideSpacerTooltip(3)));
    }

    private static void addAccessoriesSection(
            List<Either<FormattedText, TooltipComponent>> tooltip,
            String bobberName
    ) {
        tooltip.add(Either.left(Component.translatable(COBBLETIDE_ACCESSORIES).withStyle(ChatFormatting.GRAY)));
        if (bobberName != null && !bobberName.isBlank()) {
            tooltip.add(Either.left(Component.literal(bobberName).withStyle(ChatFormatting.BLUE)));
        }
        tooltip.add(Either.right(new CobbleTideSpacerTooltip(2)));
    }

    private static ItemStack getBaitStack(ItemStack rod) {
        for (TypedDataComponent<?> component : rod.getComponents()) {
            Object value = component.value();
            if (!(value instanceof RodBaitComponent baitComponent)) {
                continue;
            }
            ItemStack bait = baitComponent.getStack();
            if (bait == null || bait.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return bait.copy();
        }
        return ItemStack.EMPTY;
    }

    private static String extractTranslatedArgument(String renderedText, String translationKey) {
        String marker = "__COBBLETIDE_VALUE__";
        String template = Component.translatable(translationKey, marker).getString();
        int markerPosition = template.indexOf(marker);
        if (markerPosition < 0) {
            return null;
        }
        String prefix = template.substring(0, markerPosition);
        String suffix = template.substring(markerPosition + marker.length());
        if (!renderedText.startsWith(prefix)) {
            return null;
        }
        if (!renderedText.endsWith(suffix)) {
            return null;
        }
        int start = prefix.length();
        int end = renderedText.length() - suffix.length();
        if (end < start) {
            return null;
        }
        return renderedText.substring(start, end);
    }
}
