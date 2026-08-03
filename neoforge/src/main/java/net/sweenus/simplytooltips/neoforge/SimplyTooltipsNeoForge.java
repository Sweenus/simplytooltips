package net.sweenus.simplytooltips.neoforge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.sweenus.simplytooltips.SimplyTooltips;

@Mod(SimplyTooltips.MOD_ID)
public final class SimplyTooltipsNeoForge {
    public SimplyTooltipsNeoForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(SimplyTooltips.MOD_ID, modEventBus);
        SimplyTooltips.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(SimplyTooltipsNeoForgeClient::onClientSetup);
            modEventBus.addListener(SimplyTooltipsNeoForgeClient::onRegisterReloadListeners);
            modEventBus.addListener(SimplyTooltipsNeoForgeClient::onRegisterKeyMappings);
        }
    }
}
