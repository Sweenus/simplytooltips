package net.sweenus.simplytooltips.fabric;

import net.fabricmc.api.ModInitializer;
import net.sweenus.simplytooltips.SimplyTooltips;

public final class SimplyTooltipsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimplyTooltips.init();
    }
}
