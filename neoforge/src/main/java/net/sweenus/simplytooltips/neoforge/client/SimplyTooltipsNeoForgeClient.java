package net.sweenus.simplytooltips.neoforge.client;

import net.minecraft.resource.SynchronousResourceReloader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.TooltipKeybinds;
import net.sweenus.simplytooltips.client.TooltipNavigationEvents;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;

@EventBusSubscriber(modid = SimplyTooltips.MOD_ID, value = Dist.CLIENT)
public final class SimplyTooltipsNeoForgeClient {

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((SynchronousResourceReloader) ThemeRegistry::loadAll);
        event.registerReloadListener((SynchronousResourceReloader) ItemThemeRegistry::loadAll);
        TooltipNavigationEvents.register();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TooltipKeybinds.CYCLE_TAB);
        event.register(TooltipKeybinds.CAPTURE_GIF);
    }

    private SimplyTooltipsNeoForgeClient() {}
}
