package net.sweenus.simplytooltips;

import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import net.sweenus.simplytooltips.client.tooltip.GenericTooltipProvider;
import net.sweenus.simplytooltips.config.SimplyTooltipsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimplyTooltips {
    public static final String MOD_ID = "simplytooltips";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized = false;

    private SimplyTooltips() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("SimplyTooltips config loaded: {}", SimplyTooltipsConfig.INSTANCE.getId());
        TooltipProviderRegistry.register(new GenericTooltipProvider(), 0);
    }
}
