package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.gui.DrawContext;

/**
 * Draws upgrade pips with staggered scale-in animations.
 */
public class PipAnimator {

    public static final float START_SCALE     = 0.28F;
    public static final float OVERSHOOT_SCALE = 1.40F;
    public static final long  GROW_MS         = 70L;
    public static final long  SETTLE_MS       = 85L;
    public static final long  STAGGER_MS      = 32L;

    /**
     * Returns the display scale for a pip at {@code pipIndex} given elapsed tooltip time.
     */
    public static float getPipScale(long tooltipElapsedMs, int pipIndex) {
        long localMs = tooltipElapsedMs - (long) pipIndex * STAGGER_MS;
        long totalAnimMs = GROW_MS + SETTLE_MS;

        if (localMs <= 0L) return START_SCALE;
        if (localMs >= totalAnimMs) return 1.0F;

        if (localMs < GROW_MS) {
            float t = localMs / (float) GROW_MS;
            return lerpFloat(START_SCALE, OVERSHOOT_SCALE, easeOutCubic(t));
        }

        float t = (localMs - GROW_MS) / (float) SETTLE_MS;
        return lerpFloat(OVERSHOOT_SCALE, 1.0F, easeOutQuad(t));
    }

    /** Static (non-animated) pip with top-highlight. */
    public static void drawPip(DrawContext context, int x, int y, int color, boolean filled) {
        context.fill(x, y, x + 5, y + 5, color);
        if (filled) {
            int highlight = TooltipPainter.lerpColor(color, 0xFFFFFFFF, 0.35f);
            context.fill(x, y, x + 5, y + 1, highlight);
        } else {
            context.fill(x, y, x + 5, y + 1, 0xFF4A4030);
        }
    }

    /** Animated pip — computes the top-highlight then delegates to {@link #drawAnimatedSquarePip}. */
    public static void drawAnimatedPip(DrawContext context, int x, int y, int color,
                                       boolean filled, long tooltipElapsedMs, int pipIndex) {
        int topHighlight = filled ? TooltipPainter.lerpColor(color, 0xFFFFFFFF, 0.35f) : 0xFF4A4030;
        drawAnimatedSquarePip(context, x, y, 5, color, topHighlight, tooltipElapsedMs, pipIndex, filled);
    }

    /**
     * Draws a square pip, optionally with a scale-from-center animation.
     *
     * @param animate if false, drawn statically; if true and {@code pipIndex >= 0}, animates on entry
     */
    public static void drawAnimatedSquarePip(DrawContext context, int x, int y, int size,
                                             int bodyColor, int topHighlightColor,
                                             long tooltipElapsedMs, int pipIndex, boolean animate) {
        if (!animate) {
            context.fill(x, y, x + size, y + size, bodyColor);
            context.fill(x, y, x + size, y + 1, topHighlightColor);
            return;
        }

        float scale = getPipScale(tooltipElapsedMs, pipIndex);
        if (Math.abs(scale - 1.0F) < 0.001F) {
            context.fill(x, y, x + size, y + size, bodyColor);
            context.fill(x, y, x + size, y + 1, topHighlightColor);
            return;
        }

        context.getMatrices().push();
        float cx = x + (size / 2.0F);
        float cy = y + (size / 2.0F);
        context.getMatrices().translate(cx, cy, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.getMatrices().translate(-(size / 2.0F), -(size / 2.0F), 0.0F);
        context.fill(0, 0, size, size, bodyColor);
        context.fill(0, 0, size, 1, topHighlightColor);
        context.getMatrices().pop();
    }

    // --- Easing ---

    public static float lerpFloat(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float easeOutQuad(float t) {
        float inv = 1.0F - t;
        return 1.0F - inv * inv;
    }

    public static float easeOutCubic(float t) {
        float inv = 1.0F - t;
        return 1.0F - (inv * inv * inv);
    }

    private PipAnimator() {}
}
