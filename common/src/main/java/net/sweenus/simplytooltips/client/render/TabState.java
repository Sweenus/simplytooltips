package net.sweenus.simplytooltips.client.render;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * Static tab-navigation state for the tooltip tabs feature.
 * State is keyed by item {@link Identifier}; automatically resets to tab 0 when the hovered item changes.
 *
 * <p>Call order per frame:
 * <ol>
 *   <li>{@link #notifyItem(Identifier, List)} — after computing which tabs are available</li>
 *   <li>{@link #multiTab()} — to decide whether to show tab UI vs. normal footer</li>
 *   <li>{@link #activeTab()} / {@link #tabs()} — to gate content drawing</li>
 * </ol>
 */
public class TabState {

    /**
     * The content tabs, in cycle order.
     * <ul>
     *   <li>{@code LORE}    — ability / description lines</li>
     *   <li>{@code FORGE}   — upgrade section</li>
     *   <li>{@code STATS}   — body and extra lines (stat bars, attributes)</li>
     *   <li>{@code AFFIXES} — Apotheosis affix bullets and socket summary (only shown when present)</li>
     * </ul>
     */
    public enum Tab { LORE, FORGE, STATS, AFFIXES }

    private static int        activeIndex  = 0;
    private static List<Tab>  activeTabs   = Collections.emptyList();
    private static Identifier lastItemKey  = null;

    /**
     * Must be called each render after the set of available tabs has been computed.
     * Resets to tab 0 if the item has changed; clamps the active index if the tab count shrank.
     */
    public static void notifyItem(Identifier itemId, List<Tab> tabs) {
        if (!itemId.equals(lastItemKey)) {
            activeIndex = 0;
            lastItemKey = itemId;
        }
        activeTabs  = tabs.isEmpty() ? Collections.emptyList() : List.copyOf(tabs);
        activeIndex = activeTabs.isEmpty() ? 0 : Math.min(activeIndex, activeTabs.size() - 1);
    }

    /** Returns the currently active tab. Returns {@link Tab#STATS} as a safe default if state is invalid. */
    public static Tab activeTab() {
        if (activeTabs.isEmpty()) return Tab.STATS;
        return activeTabs.get(activeIndex);
    }

    /** Returns an unmodifiable view of the currently available tabs. */
    public static List<Tab> tabs() {
        return activeTabs;
    }

    /**
     * Returns {@code true} if there are 2 or more available tabs,
     * meaning the tab UI (cycling dots, content filtering) should be active.
     */
    public static boolean multiTab() {
        return activeTabs.size() >= 2;
    }

    /**
     * Advances to the next tab in cycle order, wrapping around.
     * Also resets the scroll offset to the top.
     */
    public static void cycleNext() {
        if (activeTabs.size() < 2) return;
        activeIndex = (activeIndex + 1) % activeTabs.size();
        ScrollState.reset();
    }

    private TabState() {}
}
