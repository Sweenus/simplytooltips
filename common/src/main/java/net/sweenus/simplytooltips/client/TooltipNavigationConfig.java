package net.sweenus.simplytooltips.client;

/**
 * Boolean flags controlling tooltip navigation features.
 * Both are {@code false} by default — full implementations are deferred to a future update.
 * A proper config system (FzzyConfig or similar) will be wired up in that update.
 */
public class TooltipNavigationConfig {

    /**
     * When {@code true}: if the tooltip panel would extend below the bottom of the screen,
     * the panel height is clamped and a scroll bar is drawn on the inner-right edge.
     * The player can scroll the tooltip content with the mouse wheel.
     */
    public static boolean SCROLLABLE_TOOLTIP = false;

    /**
     * When {@code true}: the three footer dots represent tabs.
     * The active tab's dot is rendered slightly larger and brighter.
     * A configurable keybind cycles between tabs.
     */
    public static boolean TOOLTIP_TABS = false;

    private TooltipNavigationConfig() {}
}
