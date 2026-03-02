package net.sweenus.simplytooltips.client;

import net.sweenus.simplytooltips.config.SimplyTooltipsConfig;

/**
 * Accessor facade for tooltip navigation feature flags.
 * Values are read live from {@link SimplyTooltipsConfig} so in-game config changes take effect immediately.
 */
public class TooltipNavigationConfig {

    /** @return {@code true} if Simply Tooltips custom tooltip rendering is globally enabled. */
    public static boolean tooltipRenderingEnabled() {
        return SimplyTooltipsConfig.INSTANCE.enableTooltipRendering.get();
    }

    /** @return {@code true} if mouse-wheel scrolling is enabled for tall tooltips. */
    public static boolean scrollableTooltip() {
        return tooltipRenderingEnabled() && SimplyTooltipsConfig.INSTANCE.general.scrollableTooltip.get();
    }

    /** @return {@code true} if content tabs (LORE / FORGE / STATS) are enabled. */
    public static boolean tooltipTabs() {
        return tooltipRenderingEnabled() && SimplyTooltipsConfig.INSTANCE.general.tooltipTabs.get();
    }

    /** @return {@code true} if vanilla (minecraft namespace) items should use Simply Tooltips. */
    public static boolean applyTooltipsToVanillaItems() {
        return tooltipRenderingEnabled()
                && SimplyTooltipsConfig.INSTANCE.general.applyTooltipsToVanillaItems.get();
    }

    /** @return {@code true} if all modded items should use Simply Tooltips. */
    public static boolean applyTooltipsToModItems() {
        return tooltipRenderingEnabled()
                && SimplyTooltipsConfig.INSTANCE.general.applyTooltipsToModItems.get();
    }

    private TooltipNavigationConfig() {}
}
