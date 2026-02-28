package net.sweenus.simplytooltips.client;

import net.sweenus.simplytooltips.config.SimplyTooltipsConfig;

/**
 * Accessor facade for tooltip navigation feature flags.
 * Values are read live from {@link SimplyTooltipsConfig} so in-game config changes take effect immediately.
 */
public class TooltipNavigationConfig {

    /** @return {@code true} if mouse-wheel scrolling is enabled for tall tooltips. */
    public static boolean scrollableTooltip() {
        return SimplyTooltipsConfig.INSTANCE.general.scrollableTooltip.get();
    }

    /** @return {@code true} if content tabs (LORE / FORGE / STATS) are enabled. */
    public static boolean tooltipTabs() {
        return SimplyTooltipsConfig.INSTANCE.general.tooltipTabs.get();
    }

    /** @return {@code true} if vanilla (minecraft namespace) items should use Simply Tooltips. */
    public static boolean applyTooltipsToVanillaItems() {
        return SimplyTooltipsConfig.INSTANCE.general.applyTooltipsToVanillaItems.get();
    }

    /** @return {@code true} if all modded items should use Simply Tooltips. */
    public static boolean applyTooltipsToModItems() {
        return SimplyTooltipsConfig.INSTANCE.general.applyTooltipsToModItems.get();
    }

    private TooltipNavigationConfig() {}
}
