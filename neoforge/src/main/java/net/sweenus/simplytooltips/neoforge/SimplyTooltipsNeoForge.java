package net.sweenus.simplytooltips.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.sweenus.simplytooltips.SimplyTooltips;

@Mod(SimplyTooltips.MOD_ID)
public final class SimplyTooltipsNeoForge {
    public SimplyTooltipsNeoForge(IEventBus modEventBus) {
        SimplyTooltips.init();
    }
}
