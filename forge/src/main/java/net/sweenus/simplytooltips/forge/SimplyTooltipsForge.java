package net.sweenus.simplytooltips.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.sweenus.simplytooltips.SimplyTooltips;

@Mod(SimplyTooltips.MOD_ID)
public final class SimplyTooltipsForge {
    public SimplyTooltipsForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(SimplyTooltips.MOD_ID, modEventBus);
        SimplyTooltips.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(SimplyTooltipsForgeClient::onClientSetup);
            modEventBus.addListener(SimplyTooltipsForgeClient::onRegisterReloadListeners);
            modEventBus.addListener(SimplyTooltipsForgeClient::onRegisterKeyMappings);
        }
    }
}
