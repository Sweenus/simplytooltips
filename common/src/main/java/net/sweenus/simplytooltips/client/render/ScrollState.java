package net.sweenus.simplytooltips.client.render;

import net.minecraft.util.Identifier;

/**
 * Static scroll offset state for the scrollable tooltip feature.
 * State is keyed by item {@link Identifier}; automatically resets when the hovered item changes.
 *
 * <p>Call order per frame:
 * <ol>
 *   <li>{@link #notifyItem(Identifier)} — at the start of rendering, before everything else</li>
 *   <li>{@link #flushScrollDelta(int)} — after {@code scrollMax} is computed from layout</li>
 *   <li>{@link #get()} — to offset {@code cursorY} inside the scissor region</li>
 * </ol>
 */
public class ScrollState {

    private static final double SCROLL_SPEED = 8.0;

    private static int      offsetPx          = 0;
    private static double   pendingScrollDelta = 0.0;
    private static Identifier lastItemKey     = null;

    /**
     * Must be called at the start of each tooltip render.
     * Resets scroll state if the hovered item has changed since last frame.
     */
    public static void notifyItem(Identifier itemId) {
        if (!itemId.equals(lastItemKey)) {
            offsetPx          = 0;
            pendingScrollDelta = 0.0;
            lastItemKey       = itemId;
        }
    }

    /**
     * Queues a mouse-wheel scroll delta. Positive {@code amountY} = wheel forward = scroll up
     * = decrease the offset so content moves toward the top.
     *
     * <p>Called from the Architectury mouse-scroll event handler.
     */
    public static void queueScroll(double amountY) {
        pendingScrollDelta -= amountY * SCROLL_SPEED;
    }

    /**
     * Applies the accumulated scroll delta and clamps the offset to {@code [0, maxOffset]}.
     * Must be called after the layout phase has produced a valid {@code scrollMax} value.
     */
    public static void flushScrollDelta(int maxOffset) {
        if (pendingScrollDelta != 0.0) {
            offsetPx = (int) Math.round(offsetPx + pendingScrollDelta);
            pendingScrollDelta = 0.0;
        }
        offsetPx = Math.max(0, Math.min(offsetPx, maxOffset));
    }

    /** Returns the current scroll offset in pixels (0 = top, positive = scrolled down). */
    public static int get() {
        return offsetPx;
    }

    /** Resets the scroll offset to the top without touching the item key. Called by {@link TabState} on tab switch. */
    public static void reset() {
        offsetPx          = 0;
        pendingScrollDelta = 0.0;
    }

    private ScrollState() {}
}
