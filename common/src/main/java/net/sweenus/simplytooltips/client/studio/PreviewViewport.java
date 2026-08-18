package net.sweenus.simplytooltips.client.studio;

/**
 * Zoom and pan state for the Theme Studio's preview stage.
 *
 * <p>Deliberately free of Minecraft types: this is where all the fiddly arithmetic lives - snapping,
 * zoom-at-cursor, pan clamping - and keeping it independent means it can be exercised without a
 * running client.
 *
 * <p>Zoom has two modes. <em>Fit</em> is free-form: whatever scale makes the panel fill the stage,
 * which is what you want when the Studio opens. As soon as the player zoom deliberately it snaps to
 * one of {@link #STEPS}, because the preview is pixel art and a fractional scale resamples it into
 * mush.
 */
public final class PreviewViewport {

    /** Crisp scales: quarters and thirds below 1:1, whole multiples above it. */
    public static final float[] STEPS = {0.25f, 1.0f / 3.0f, 0.5f, 1.0f, 2.0f, 3.0f, 4.0f};

    /** Sentinel for "no explicit zoom - fill the stage". */
    private static final float FIT = 0.0f;

    private float zoom = FIT;
    private float panX;
    private float panY;

    private int panelW;
    private int panelH;
    private int viewW;
    private int viewH;

    /** Records the sizes every later query depends on. Call once per frame before anything else. */
    public void setBounds(int panelWidth, int panelHeight, int viewWidth, int viewHeight) {
        this.panelW = Math.max(1, panelWidth);
        this.panelH = Math.max(1, panelHeight);
        this.viewW = Math.max(1, viewWidth);
        this.viewH = Math.max(1, viewHeight);
        clampPan();
    }

    /** The free-form scale that makes the panel fill the stage without cropping or upscaling. */
    public float fitScale() {
        return Math.min(1.0f, Math.min(viewW / (float) panelW, viewH / (float) panelH));
    }

    public float scale() {
        return zoom == FIT ? fitScale() : zoom;
    }

    public boolean isFit() {
        return zoom == FIT;
    }

    public float panX() {
        return panX;
    }

    public float panY() {
        return panY;
    }

    /** Back to filling the stage, centred. */
    public void reset() {
        zoom = FIT;
        panX = 0.0f;
        panY = 0.0f;
    }

    /** Top-left corner of the drawn panel, in stage-local coordinates. */
    public float offsetX() {
        return (viewW - panelW * scale()) / 2.0f + panX;
    }

    public float offsetY() {
        return (viewH - panelH * scale()) / 2.0f + panY;
    }

    public void pan(double dx, double dy) {
        panX += (float) dx;
        panY += (float) dy;
        clampPan();
    }

    /**
     * Steps one stop along {@link #STEPS}, keeping the panel pixel under the cursor put.
     *
     * @param direction +1 to zoom in, -1 to zoom out
     * @param cursorX   cursor position in stage-local coordinates
     */
    public void zoomAt(int direction, double cursorX, double cursorY) {
        float before = scale();
        float after = nextStep(before, direction);
        if (after == before) return;

        // The panel coordinate under the cursor has to land back under the cursor afterwards.
        double panelPointX = (cursorX - offsetX()) / before;
        double panelPointY = (cursorY - offsetY()) / before;

        zoom = after;

        // offset = (view - panel*scale)/2 + pan, and we need offset + panelPoint*scale == cursor.
        panX = (float) (cursorX - panelPointX * after - (viewW - panelW * after) / 2.0);
        panY = (float) (cursorY - panelPointY * after - (viewH - panelH * after) / 2.0);
        clampPan();
    }

    /** The next crisp step from {@code current}, or {@code current} if already at the end. */
    public static float nextStep(float current, int direction) {
        if (direction > 0) {
            for (float step : STEPS) {
                if (step > current + 1.0e-4f) return step;
            }
            return STEPS[STEPS.length - 1];
        }
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (STEPS[i] < current - 1.0e-4f) return STEPS[i];
        }
        return STEPS[0];
    }

    /**
     * Keeps the panel overlapping the stage. When the panel is smaller than the stage it stays
     * centred; when it is larger, panning is bounded by the overhang, so an edge can reach the
     * matching stage edge but no further.
     */
    private void clampPan() {
        float drawW = panelW * scale();
        float drawH = panelH * scale();

        float slackX = Math.max(0.0f, (drawW - viewW) / 2.0f);
        float slackY = Math.max(0.0f, (drawH - viewH) / 2.0f);

        panX = Math.max(-slackX, Math.min(slackX, panX));
        panY = Math.max(-slackY, Math.min(slackY, panY));
    }

    /** Percentage shown in the stage corner. */
    public int percent() {
        return Math.round(scale() * 100.0f);
    }
}
