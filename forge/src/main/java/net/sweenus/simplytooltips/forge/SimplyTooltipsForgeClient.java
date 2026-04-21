package net.sweenus.simplytooltips.forge;

import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.sweenus.simplytooltips.client.TooltipKeybinds;
import net.sweenus.simplytooltips.client.TooltipNavigationEvents;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;

public final class SimplyTooltipsForgeClient {
    private SimplyTooltipsForgeClient() {
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(TooltipNavigationEvents::register);
    }

    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((SynchronousResourceReloader) ThemeRegistry::loadAll);
        event.registerReloadListener((SynchronousResourceReloader) ItemThemeRegistry::loadAll);
    }

    public static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(TooltipKeybinds.CYCLE_TAB);
        event.register(TooltipKeybinds.CAPTURE_GIF);
    }
}
