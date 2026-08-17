package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class RadiantMotif implements BackgroundMotif {

    private static final int CORE  = 0xFFFFF1;
    private static final int PALE  = 0xFFE588;
    private static final int GOLD  = 0xFFC233;
    private static final int AMBER = 0xFFAD27;
    private static final int DUSK  = 0x3A2E6B;

    private static final int  SUN_BANDS     = 4;
    private static final int  RAY_COUNT     = 9;
    private static final int  SPOKE_COUNT   = 9;
    private static final int  MOTE_COUNT    = 10;
    private static final long MOTE_CYCLE_MS = 6400L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int topY = y + 4;
        int bottomY = y + h - 4;
        int centerX = x + w / 2;
        int span = Math.max(1, bottomY - topY);
        int topReach = clamp(span / 5, 10, 22);
        int bottomReach = clamp(span / 4, 10, 26);
        double breath = (Math.sin(timeMs * 0.0009) + 1.0) * 0.5;

        drawMotes(context, x, w, topY, bottomY, timeMs);
        drawRays(context, x, w, topY, bottomY, centerX, bottomReach, breath, timeMs);
        drawSunrise(context, x, w, topY, bottomY, centerX, bottomReach, breath);
        drawHalo(context, x, w, topY, centerX, topReach, breath);
    }

    private static void drawSunrise(DrawContext context, int panelX, int panelW,
                                    int topY, int bottomY, int centerX, int reach, double breath) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;

        for (int row = bottomY - 1; row >= topY; row--) {
            int d = bottomY - 1 - row;
            if (d >= reach) break;

            double presence = presence(d, reach);
            double rise = d / (double) reach;
            double dome = Math.sqrt(Math.max(0.0, 1.0 - rise * rise));
            int halfW = (int) Math.round(panelW * 0.52 * dome);
            if (halfW < 1) continue;

            int bandA = (int) Math.round((4 + breath * 3) * presence);
            if (bandA <= 0) continue;

            for (int band = 0; band < SUN_BANDS; band++) {
                int bw = (int) Math.round(halfW * (1.0 - band * 0.24));
                if (bw < 1) continue;
                context.fill(clamp(centerX - bw, minX, maxX), row,
                        clamp(centerX + bw + 1, minX, maxX), row + 1,
                        rgba(band < 2 ? AMBER : GOLD, bandA));
            }

            if (d >= 2) continue;

            int coreW = Math.max(1, halfW / 4);
            context.fill(clamp(centerX - coreW, minX, maxX), row,
                    clamp(centerX + coreW + 1, minX, maxX), row + 1,
                    rgba(PALE, (int) Math.round((16 + breath * 8) * presence)));
        }
    }

    private static void drawRays(DrawContext context, int panelX, int panelW,
                                 int topY, int bottomY, int centerX, int reach,
                                 double breath, long timeMs) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int originY = bottomY - 1;
        int length = reach * 2;
        double sweep = Math.sin(timeMs * 0.0004) * 0.10;

        for (int i = 0; i < RAY_COUNT; i++) {
            double angle = -1.31 + (2.62 * i) / (RAY_COUNT - 1.0) + sweep;
            double stepX = Math.sin(angle);
            double stepY = -Math.cos(angle);
            double peak = (11 + breath * 7) * (((i & 1) == 0) ? 1.0 : 0.62);

            for (int step = 4; step <= length; step++) {
                int py = originY + (int) Math.round(stepY * step);
                if (py < topY) break;

                double falloff = 1.0 - step / (double) length;
                int alpha = (int) Math.round(peak * falloff * falloff);
                if (alpha <= 0) continue;

                int px = centerX + (int) Math.round(stepX * step);
                context.fill(clamp(px, minX, maxX), py, clamp(px + 1, minX, maxX), py + 1,
                        rgba(GOLD, alpha));
            }
        }
    }

    private static void drawHalo(DrawContext context, int panelX, int panelW,
                                 int topY, int centerX, int reach, double breath) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int radius = clamp(Math.min(panelW * 5 / 12, reach * 3), 14, 56);
        int centerY = topY + radius + 4;
        int steps = Math.max(24, (int) (radius * 2.5));
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;

        for (int i = 0; i <= steps; i++) {
            double angle = -1.25 + (2.5 * i) / steps;
            int py = centerY - (int) Math.round(Math.cos(angle) * radius);
            if (py < topY) continue;

            int d = py - topY;
            if (d >= reach) continue;

            int px = centerX + (int) Math.round(Math.sin(angle) * radius);
            if (px == lastX && py == lastY) continue;
            lastX = px;
            lastY = py;

            double presence = presence(d, reach);
            int ringA = (int) Math.round((34 + breath * 26) * presence);
            if (ringA <= 0) continue;

            int glowA = (int) Math.round((9 + breath * 7) * presence);
            context.fill(clamp(px - 1, minX, maxX), py, clamp(px + 2, minX, maxX), py + 1, rgba(GOLD, glowA));
            context.fill(clamp(px, minX, maxX), py + 1, clamp(px + 1, minX, maxX), py + 2, rgba(DUSK, glowA));
            context.fill(clamp(px, minX, maxX), py, clamp(px + 1, minX, maxX), py + 1, rgba(PALE, ringA));

            if (Math.abs(angle) < 0.05) {
                context.fill(clamp(px, minX, maxX), py, clamp(px + 1, minX, maxX), py + 1,
                        rgba(CORE, (int) Math.round(28 + breath * 22)));
            }
        }

        drawSpokes(context, centerX, centerY, radius, topY, reach, minX, maxX, breath);
    }

    private static void drawSpokes(DrawContext context, int centerX, int centerY, int radius,
                                   int topY, int reach, int minX, int maxX, double breath) {
        int maxStep = clamp(reach / 4, 0, 5);
        if (maxStep < 2) return;

        for (int i = 0; i < SPOKE_COUNT; i++) {
            double angle = -0.85 + (1.7 * i) / (SPOKE_COUNT - 1.0);
            double stepX = Math.sin(angle);
            double stepY = -Math.cos(angle);

            int baseY = centerY + (int) Math.round(stepY * radius);
            if (baseY < topY || baseY - topY >= reach) continue;

            for (int step = 2; step <= maxStep; step++) {
                int py = centerY + (int) Math.round(stepY * (radius + step));
                if (py < topY) continue;

                int d = py - topY;
                if (d >= reach) continue;

                double taper = 1.0 - (step - 2) / (double) (maxStep + 1);
                int alpha = (int) Math.round((18 + breath * 10) * presence(d, reach) * taper);
                if (alpha <= 0) continue;

                int px = centerX + (int) Math.round(stepX * (radius + step));
                context.fill(clamp(px, minX, maxX), py, clamp(px + 1, minX, maxX), py + 1,
                        rgba(PALE, alpha));
            }
        }
    }

    private static void drawMotes(DrawContext context, int panelX, int panelW,
                                  int topY, int bottomY, long timeMs) {
        int minX = panelX + 5;
        int maxX = panelX + panelW - 5;
        int spanX = Math.max(1, maxX - minX);
        int spanY = Math.max(1, bottomY - topY);

        for (int i = 0; i < MOTE_COUNT; i++) {
            long cycle = MOTE_CYCLE_MS + i * 431L;
            float progress = Math.floorMod(timeMs + i * 733L, cycle) / (float) cycle;

            int py = bottomY - 1 - Math.round(spanY * progress);
            if (py < topY) continue;

            int alpha = Math.round(20 * Math.min(1.0F, progress * 5.0F) * (1.0F - progress));
            if (alpha <= 0) continue;

            int px = minX + Math.floorMod(i * 53 + i * i * 19, spanX);
            px += (int) Math.round(Math.sin(timeMs * 0.0008 + i * 1.61) * 3.0);
            px = clamp(px, minX, maxX - 1);

            context.fill(px, py, px + 1, py + 1, rgba(GOLD, alpha));
            if (i % 3 == 0) {
                context.fill(px, py, px + 1, py + 1, rgba(PALE, Math.max(1, alpha / 2)));
            }
        }
    }

    private static double presence(int d, int reach) {
        if (d >= reach) return 0.0;
        double fall = 1.0 - d / (double) reach;
        return fall * fall;
    }

    private static int rgba(int rgb, int alpha) {
        int a = clamp(alpha, 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
