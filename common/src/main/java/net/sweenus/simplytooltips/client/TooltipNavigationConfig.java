package net.sweenus.simplytooltips.client;

/**
 * Boolean flags controlling tooltip navigation features.
 * A proper config system (FzzyConfig or similar) will be wired up in a future update.
 */
public class TooltipNavigationConfig {

    /**
     * When {@code true}: if the tooltip body content is taller than the available screen space,
     * the panel height is clamped and a scroll bar is drawn on the inner-right edge.
     * The player can scroll the tooltip content with the mouse wheel.
     */
    public static boolean SCROLLABLE_TOOLTIP = true;

    /**
     * When {@code true}: content is split into up to three tabs (LORE, FORGE, STATS).
     * The footer dots become tab indicators; the active tab's dot is larger and brighter.
     * Press {@code G} (default) to cycle between tabs.
     */
    public static boolean TOOLTIP_TABS = true;

    private TooltipNavigationConfig() {}
}
