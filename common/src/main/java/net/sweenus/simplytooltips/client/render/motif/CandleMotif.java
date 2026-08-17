package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class CandleMotif implements BackgroundMotif {

    private static final int CORE   = 0xFFFFF1;
    private static final int FLAME  = 0xFFFF2A;
    private static final int GLOW   = 0xFFD86A;
    private static final int TALLOW = 0xF2E3C0;
    private static final int WAX    = 0xCCC3A8;
    private static final int BRASS  = 0xC49D1C;
    private static final int WICK   = 0x3C1A1A;

    private static final int  FLAME_H       = 16;
    private static final int  HALO_BANDS    = 4;
    private static final int  POOL_BANDS    = 3;
    private static final int  DRIP_COUNT    = 8;
    private static final int  MOTE_COUNT    = 8;
    private static final long MOTE_CYCLE_MS = 7100L;
    private static final long FALL_CYCLE_MS = 5200L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int topY = y + 4;
        int bottomY = y + h - 4;
        int centerX = x + w / 2;
        int span = Math.max(1, bottomY - topY);
        int topReach = clamp(span / 5, 12, 24);
        int bottomReach = clamp(span / 4, 10, 26);

        double flicker = clamp01(0.55
                + Math.sin(timeMs * 0.0031) * 0.22
                + Math.sin(timeMs * 0.0073 + 1.7) * 0.14
                + (hash01(timeMs / 90L) - 0.5) * 0.18);
        double lean = Math.sin(timeMs * 0.0013) * 0.6
                + Math.sin(timeMs * 0.0047 + 0.9) * 0.4;

        int flameBase = topY + topReach - 3;

        drawMotes(context, x, w, topY, bottomY, timeMs);
        drawDrips(context, x, w, topY, bottomY, flicker, timeMs);
        drawHalo(context, x, w, topY, topReach, centerX, flameBase, flicker);
        drawFlame(context, x, w, topY, centerX, flameBase, flicker, lean);
        drawPool(context, x, w, topY, bottomY, centerX, bottomReach, flicker);
    }

    private static void drawFlame(DrawContext context, int panelX, int panelW, int topY,
                                  int centerX, int baseY, double flicker, double lean) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;

        context.fill(clamp(centerX, minX, maxX), baseY + 1,
                clamp(centerX + 1, minX, maxX), baseY + 3, rgba(WICK, 120));

        for (int n = 0; n < FLAME_H; n++) {
            int row = baseY - n;
            if (row < topY) break;

            double up = n / (double) (FLAME_H - 1);
            double taper = Math.pow(Math.sin((1.0 - up) * Math.PI * 0.86), 1.6);
            int halfW = (int) Math.round(taper * (2.4 + flicker * 1.0));
            int cx = centerX + (int) Math.round(lean * up * up * 3.0);

            int glowA  = (int) Math.round((18 + flicker * 12) * (0.4 + taper * 0.6));
            int flameA = (int) Math.round((26 + flicker * 18) * taper);
            if (flameA <= 0) continue;

            context.fill(clamp(cx - halfW - 1, minX, maxX), row,
                    clamp(cx + halfW + 2, minX, maxX), row + 1, rgba(GLOW, glowA));
            context.fill(clamp(cx - halfW, minX, maxX), row,
                    clamp(cx + halfW + 1, minX, maxX), row + 1, rgba(FLAME, flameA));

            if (up > 0.14 && up < 0.62) {
                context.fill(clamp(cx, minX, maxX), row, clamp(cx + 1, minX, maxX), row + 1,
                        rgba(CORE, (int) Math.round(40 + flicker * 38)));
            }
        }
    }

    private static void drawHalo(DrawContext context, int panelX, int panelW, int topY, int reach,
                                 int centerX, int flameBase, double flicker) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int radius = clamp(Math.min(panelW / 4, reach + 4), 12, 30);
        int haloY = flameBase - FLAME_H / 2;

        for (int row = topY; row < topY + reach; row++) {
            int dy = row - haloY;
            double inside = 1.0 - (dy * dy) / (double) (radius * radius);
            if (inside <= 0.0) continue;

            double edgeFade = presence(row - topY, reach);
            int halfW = (int) Math.round(radius * Math.sqrt(inside));
            if (halfW < 1) continue;

            int bandA = (int) Math.round((5 + flicker * 4) * (0.45 + edgeFade * 0.55));
            if (bandA <= 0) continue;

            for (int band = 0; band < HALO_BANDS; band++) {
                int bw = (int) Math.round(halfW * (1.0 - band * 0.23));
                if (bw < 1) continue;
                context.fill(clamp(centerX - bw, minX, maxX), row,
                        clamp(centerX + bw + 1, minX, maxX), row + 1, rgba(GLOW, bandA));
            }
        }
    }

    private static void drawPool(DrawContext context, int panelX, int panelW,
                                 int topY, int bottomY, int centerX, int reach, double flicker) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int maxHalf = (int) Math.round(panelW * 0.52);

        for (int row = bottomY - 1; row >= topY; row--) {
            int d = bottomY - 1 - row;
            if (d >= reach) break;

            double presence = presence(d, reach);
            double rise = d / (double) reach;
            double dome = Math.sqrt(Math.max(0.0, 1.0 - rise * rise));
            int halfW = (int) Math.round(maxHalf * dome + wobble(d, 7, 11L) * 7.0);
            if (halfW < 2) continue;

            int bandA = (int) Math.round((10 + flicker * 5) * presence);
            if (bandA <= 0) continue;

            for (int band = 0; band < POOL_BANDS; band++) {
                int bw = (int) Math.round(halfW * (1.0 - band * 0.26));
                if (bw < 1) continue;
                context.fill(clamp(centerX - bw, minX, maxX), row,
                        clamp(centerX + bw + 1, minX, maxX), row + 1,
                        rgba(band == 0 ? WAX : TALLOW, bandA));
            }

            if (d >= 2) continue;

            int brassW = Math.max(2, halfW / 3);
            context.fill(clamp(centerX - brassW, minX, maxX), row,
                    clamp(centerX + brassW + 1, minX, maxX), row + 1,
                    rgba(BRASS, (int) Math.round(20 + flicker * 12)));
        }
    }

    private static void drawDrips(DrawContext context, int panelX, int panelW,
                                  int topY, int bottomY, double flicker, long timeMs) {
        int minX = panelX + 5;
        int maxX = panelX + panelW - 5;
        int spanX = Math.max(1, maxX - minX);

        for (int i = 0; i < DRIP_COUNT; i++) {
            int px = minX + (int) Math.floorMod((long) Math.round(hash01(i * 13L + 5L) * spanX), spanX);
            int len = 5 + (int) Math.floorMod((long) Math.round(hash01(i * 29L + 3L) * 10.0), 10L);

            long cycle = FALL_CYCLE_MS + i * 617L;
            float progress = Math.floorMod(timeMs + i * 811L, cycle) / (float) cycle;
            int grow = Math.round(len * Math.min(1.0F, progress * 1.7F));
            int alpha = (int) Math.round(9 + flicker * 5);

            for (int n = 0; n < grow; n++) {
                int row = topY + n;
                if (row >= bottomY) break;
                int a = Math.round(alpha * (1.0F - n / (float) (len + 3)));
                if (a <= 0) continue;
                context.fill(px, row, px + 1, row + 1, rgba(TALLOW, a));
            }

            int tipY = topY + grow;
            if (grow > 1 && tipY < bottomY - 2) {
                context.fill(px, tipY, px + 1, tipY + 2, rgba(WAX, alpha + 2));
            }

            if (progress < 0.66F) continue;

            float fall = (progress - 0.66F) / 0.34F;
            int beadY = tipY + Math.round(fall * fall * (bottomY - topY));
            if (beadY >= bottomY - 2) continue;

            int beadA = Math.round(14 * (1.0F - fall * 0.7F));
            if (beadA <= 0) continue;
            context.fill(px, beadY, px + 1, beadY + 2, rgba(TALLOW, beadA));
        }
    }

    private static void drawMotes(DrawContext context, int panelX, int panelW,
                                  int topY, int bottomY, long timeMs) {
        int minX = panelX + 5;
        int maxX = panelX + panelW - 5;
        int spanX = Math.max(1, maxX - minX);
        int spanY = Math.max(1, bottomY - topY);

        for (int i = 0; i < MOTE_COUNT; i++) {
            long cycle = MOTE_CYCLE_MS + i * 397L;
            float progress = Math.floorMod(timeMs + i * 653L, cycle) / (float) cycle;

            int py = bottomY - 1 - Math.round(spanY * progress);
            if (py < topY) continue;

            int alpha = Math.round(16 * Math.min(1.0F, progress * 4.0F) * (1.0F - progress));
            if (alpha <= 0) continue;

            int px = minX + Math.floorMod(i * 47 + i * i * 23, spanX);
            px += (int) Math.round(Math.sin(timeMs * 0.0011 + i * 1.87) * 3.0);
            px = clamp(px, minX, maxX - 1);

            context.fill(px, py, px + 1, py + 1, rgba(TALLOW, alpha));
        }
    }

    private static double presence(int d, int reach) {
        if (d >= reach) return 0.0;
        double fall = 1.0 - d / (double) reach;
        return fall * fall;
    }

    private static double wobble(int d, int period, long salt) {
        int node = Math.floorDiv(d, period);
        double frac = Math.floorMod(d, period) / (double) period;
        double a = hash01(node + salt) - 0.5;
        double b = hash01(node + 1L + salt) - 0.5;
        return a + (b - a) * frac;
    }

    private static double hash01(long n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 33;
        x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int rgba(int rgb, int alpha) {
        int a = clamp(alpha, 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
