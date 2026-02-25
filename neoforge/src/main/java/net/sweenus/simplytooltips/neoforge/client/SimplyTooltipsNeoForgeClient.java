package net.sweenus.simplytooltips.neoforge.client;

import net.minecraft.resource.SynchronousResourceReloader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;

@EventBusSubscriber(modid = SimplyTooltips.MOD_ID, value = Dist.CLIENT)
public final class SimplyTooltipsNeoForgeClient {

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((SynchronousResourceReloader) ThemeRegistry::loadAll);
        event.registerReloadListener((SynchronousResourceReloader) ItemThemeRegistry::loadAll);
    }

    private SimplyTooltipsNeoForgeClient() {}
}
