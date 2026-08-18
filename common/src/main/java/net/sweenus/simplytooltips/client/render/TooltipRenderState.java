package net.sweenus.simplytooltips.client.render;

import net.sweenus.simplytooltips.api.ThemeDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-local render overrides applied while drawing a tooltip somewhere other than the player's
 * cursor — the GIF/PNG exporter, or the Theme Studio's live preview.
 *
 * <p>Both callers need the same thing: a tooltip drawn at a position they choose, without the
 * global hover state (scroll offset, active tab, GIF capture rect) being rewritten underneath them.
 * The preview additionally forces a theme, so a player can see any theme on any item without
 * changing what that item is actually mapped to.
 */
public final class TooltipRenderState {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    public static @Nullable State current() {
        return CURRENT.get();
    }

    public static void run(State state, Runnable action) {
        State previous = CURRENT.get();
        CURRENT.set(state);
        try {
            action.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    /** Which caller is driving the render, and therefore which behaviour differs from a hover. */
    public enum Mode {
        /** Offline render to a framebuffer: deterministic animation clock, ALT forced off. */
        EXPORT,
        /** On-screen preview inside a Screen: live animation clock, explicit panel origin. */
        PREVIEW
    }

    public static final class State {
        final Mode mode;
        final boolean measureOnly;
        final long elapsedMs;
        final long absoluteTimeMs;
        final int margin;

        /** When set, replaces the whole theme-resolution chain and the item's border override. */
        final @Nullable ThemeDefinition forcedDef;
        /** When set, replaces the badges the item would otherwise resolve to. */
        final @Nullable java.util.List<String> forcedBadges;
        /** When set, the panel's top-left corner, replacing the cursor-relative offset and clamp. */
        final @Nullable Integer originX;
        final @Nullable Integer originY;

        int canvasWidth;
        int canvasHeight;
        int panelWidth;
        int panelHeight;

        /** Export state: the original four-argument shape, unchanged. */
        public State(boolean measureOnly, long elapsedMs, long absoluteTimeMs, int margin) {
            this(Mode.EXPORT, measureOnly, elapsedMs, absoluteTimeMs, margin, null, null, null, null);
        }

        public State(Mode mode, boolean measureOnly, long elapsedMs, long absoluteTimeMs, int margin,
                     @Nullable ThemeDefinition forcedDef,
                     @Nullable java.util.List<String> forcedBadges,
                     @Nullable Integer originX, @Nullable Integer originY) {
            this.mode = mode;
            this.measureOnly = measureOnly;
            this.elapsedMs = elapsedMs;
            this.absoluteTimeMs = absoluteTimeMs;
            this.margin = margin;
            this.forcedDef = forcedDef;
            this.forcedBadges = forcedBadges;
            this.originX = originX;
            this.originY = originY;
        }

        /**
         * Builds the state the Theme Studio preview uses.
         *
         * @param elapsedMs milliseconds since the preview began showing this item, which drives the
         *                  entry and title animations exactly as a fresh hover would
         */
        public static State preview(boolean measureOnly, @Nullable ThemeDefinition forcedDef,
                                    @Nullable java.util.List<String> forcedBadges,
                                    int originX, int originY, long elapsedMs) {
            return new State(Mode.PREVIEW, measureOnly, elapsedMs, System.currentTimeMillis(), 0,
                    forcedDef, forcedBadges, originX, originY);
        }

        void recordPanel(int panelWidth, int panelHeight) {
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
            canvasWidth = panelWidth + margin * 2;
            canvasHeight = panelHeight + margin * 2;
        }

        public boolean isPreview() {
            return mode == Mode.PREVIEW;
        }

        public int canvasWidth() {
            return canvasWidth;
        }

        public int canvasHeight() {
            return canvasHeight;
        }

        /** Panel size recorded by the last render, without the export margin. */
        public int panelWidth() {
            return panelWidth;
        }

        public int panelHeight() {
            return panelHeight;
        }
    }

    private TooltipRenderState() {}
}
