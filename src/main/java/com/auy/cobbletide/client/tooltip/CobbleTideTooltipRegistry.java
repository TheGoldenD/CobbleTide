package com.auy.cobbletide.client.tooltip;

import com.auy.cobbletide.CobbleTide;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(
        modid = CobbleTide.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class CobbleTideTooltipRegistry {
    private CobbleTideTooltipRegistry() {
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CobbleTideBaitTooltip.class, CobbleTideBaitTooltipClient::new);
        event.register(CobbleTideSpacerTooltip.class, CobbleTideSpacerTooltipClient::new);
    }
}
