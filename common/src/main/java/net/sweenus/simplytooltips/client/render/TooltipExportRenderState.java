package net.sweenus.simplytooltips.client.render;

/** Thread-local render overrides used only while measuring or drawing exported tooltips. */
final class TooltipExportRenderState {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    static State current() {
        return CURRENT.get();
    }

    static void run(State state, Runnable action) {
        State previous = CURRENT.get();
        CURRENT.set(state);
        try {
            action.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    static final class State {
        final boolean measureOnly;
        final long elapsedMs;
        final long absoluteTimeMs;
        final int margin;
        int canvasWidth;
        int canvasHeight;

        State(boolean measureOnly, long elapsedMs, long absoluteTimeMs, int margin) {
            this.measureOnly = measureOnly;
            this.elapsedMs = elapsedMs;
            this.absoluteTimeMs = absoluteTimeMs;
            this.margin = margin;
        }

        void recordPanel(int panelWidth, int panelHeight) {
            canvasWidth = panelWidth + margin * 2;
            canvasHeight = panelHeight + margin * 2;
        }
    }

    private TooltipExportRenderState() {}
}
