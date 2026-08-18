package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class AmethystMotif implements BackgroundMotif {

    private static final int CORE  = 0xF4ECFF;
    private static final int FACET = 0xD9CFFF;
    private static final int BODY  = 0xC4ADFF;
    private static final int DEEP  = 0x4A2C9E;
    private static final int CALC  = 0xCFC6D8;
    private static final int SPARK = 0xE070FF;

    private static final int  CLUSTER_COUNT  = 9;
    private static final int  HANG_COUNT     = 9;
    private static final int  MOTE_COUNT     = 8;
    private static final long MOTE_CYCLE_MS  = 6800L;
    private static final long CHIME_CYCLE_MS = 5400L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int topY = y + 4;
        int bottomY = y + h - 4;
        int span = Math.max(1, bottomY - topY);
        int topReach = clamp(span / 6, 8, 18);
        int bottomReach = clamp(span / 4, 12, 28);

        double chime = Math.floorMod(timeMs, CHIME_CYCLE_MS) / (double) CHIME_CYCLE_MS;

        drawMotes(context, x, w, topY, bottomY, timeMs);
        drawShell(context, x, w, topY, topReach);
        drawHangers(context, x, w, topY, topReach);
        drawFloor(context, x, w, bottomY, bottomReach);
        drawClusters(context, x, w, bottomY, bottomReach, chime);
    }

    private static void drawFloor(DrawContext context, int panelX, int panelW, int bottomY, int reach) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int baseH = clamp(reach / 3, 4, 9);

        for (int px = minX; px < maxX; px++) {
            int crest = floorCrest(px - minX, baseH);
            for (int n = 0; n < crest; n++) {
                int row = bottomY - 1 - n;
                if (row <= bottomY - reach) break;

                if (n == crest - 1) {
                    context.fill(px, row, px + 1, row + 1, rgba(FACET, 34));
                    continue;
                }

                int a = (int) Math.round(20 * presence(n, crest + 2));
                if (a <= 0) continue;
                context.fill(px, row, px + 1, row + 1, rgba(DEEP, a));

                if (n < crest / 2) {
                    context.fill(px, row, px + 1, row + 1,
                            rgba(BODY, (int) Math.round(14 * presence(n, crest))));
                }
            }
        }
    }

    private static void drawClusters(DrawContext context, int panelX, int panelW,
                                     int bottomY, int reach, double chime) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int spanX = Math.max(1, maxX - minX);
        int baseH = clamp(reach / 3, 4, 9);
        int limitY = bottomY - reach;
        double slot = spanX / (double) CLUSTER_COUNT;
        double chimeX = minX - 24.0 + chime * (spanX + 48.0);

        for (int c = 0; c < CLUSTER_COUNT; c++) {
            int anchor = minX + (int) Math.round(slot * (c + 0.5)
                    + (hash01(c * 17L + 2L) - 0.5) * slot * 0.8);
            int shards = 3 + (int) Math.floorMod((long) Math.round(hash01(c * 31L + 9L) * 2.0), 2L);

            for (int s = 0; s < shards; s++) {
                long seed = c * 101L + s * 7L;
                int baseX = clamp(anchor + (int) Math.round((hash01(seed) - 0.5) * 13.0), minX, maxX - 1);
                int baseHalf = 2 + (int) Math.round(hash01(seed + 5L) * 2.0);
                int height = clamp((int) Math.round(baseHalf * (2.8 + hash01(seed + 3L) * 2.2)), 6, reach - 3);
                double lean = (hash01(seed + 11L) - 0.5) * 0.7;
                boolean lightLeft = hash01(seed + 19L) < 0.5;

                double boost = Math.max(0.0, 1.0 - Math.abs(baseX - chimeX) / 22.0);
                int baseY = bottomY - floorCrest(baseX - minX, baseH);

                drawSpire(context, minX, maxX, baseX, baseY, -1, limitY,
                        height, baseHalf, lean, lightLeft, boost, 14, 7, 46, 24);
            }
        }
    }

    private static void drawHangers(DrawContext context, int panelX, int panelW, int topY, int reach) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int spanX = Math.max(1, maxX - minX);
        int iconClear = panelX + 34;
        int limitY = topY + reach;
        double slot = spanX / (double) HANG_COUNT;

        for (int i = 0; i < HANG_COUNT; i++) {
            int baseX = minX + (int) Math.round(slot * (i + 0.5)
                    + (hash01(i * 23L + 41L) - 0.5) * slot * 0.8);
            if (baseX < iconClear) continue;

            int baseHalf = 1 + (int) Math.floorMod((long) Math.round(hash01(i * 53L + 11L) * 1.4), 2L);
            int height = clamp((int) Math.round(baseHalf * (2.0 + hash01(i * 13L + 7L) * 1.4)),
                    3, Math.max(3, reach / 2));
            double lean = (hash01(i * 37L + 5L) - 0.5) * 0.5;
            boolean lightLeft = hash01(i * 43L + 3L) < 0.5;
            int baseY = topY + shellDepth(baseX - minX, reach) - 1;

            drawSpire(context, minX, maxX, baseX, baseY, 1, limitY,
                    height, baseHalf, lean, lightLeft, 0.0, 12, 6, 38, 0);
        }
    }

    private static void drawSpire(DrawContext context, int minX, int maxX, int baseX, int baseY,
                                  int step, int boundY, int height, int baseHalf, double lean,
                                  boolean lightLeft, double boost,
                                  int bodyBase, int deepBase, int coreBase, int coreBoost) {
        int lit = lightLeft ? -1 : 1;

        for (int n = 0; n < height; n++) {
            int row = baseY + step * n;
            if (step < 0 ? row < boundY : row >= boundY) break;

            double up = n / (double) height;
            double fade = 1.0 - up * 0.45;
            int halfW = (int) Math.round(baseHalf * (1.0 - Math.max(0.0, (up - 0.25) / 0.75)));
            int cx = baseX + (int) Math.round(lean * n);
            boolean tip = n >= height - 2;

            int bodyA = (int) Math.round(bodyBase * fade);
            if (bodyA > 0 && halfW > 0) {
                int x0 = clamp(cx - halfW, minX, maxX - 1);
                int x1 = clamp(cx + halfW + 1, x0 + 1, maxX);
                context.fill(x0, row, x1, row + 1, rgba(BODY, bodyA));
            }

            int shadeA = (int) Math.round(deepBase * fade);
            if (shadeA > 0 && halfW > 0) {
                int sx = clamp(cx - lit * halfW, minX, maxX - 1);
                context.fill(sx, row, sx + 1, row + 1, rgba(DEEP, shadeA));
            }

            int coreA = (int) Math.round((coreBase + boost * coreBoost) * fade);
            if (coreA <= 0) continue;

            int lx = clamp(cx + lit * halfW, minX, maxX - 1);
            context.fill(lx, row, lx + 1, row + 1, rgba(tip ? CORE : FACET, coreA));

            if (tip && halfW > 0) {
                int tx = clamp(cx, minX, maxX - 1);
                context.fill(tx, row, tx + 1, row + 1, rgba(CORE, coreA));
            }
        }
    }

    private static void drawShell(DrawContext context, int panelX, int panelW, int topY, int reach) {
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;
        int iconClear = panelX + 34;
        int ramp = Math.max(1, iconClear - minX);

        for (int px = minX; px < maxX; px++) {
            int depth = shellDepth(px - minX, reach);
            double fade = clamp01(0.15 + ((px - minX) / (double) ramp) * 0.85);

            for (int n = 0; n < depth - 1; n++) {
                int a = (int) Math.round(14 * presence(n, depth) * fade);
                if (a <= 0) continue;
                context.fill(px, topY + n, px + 1, topY + n + 1, rgba(DEEP, a));
            }

            int rimA = (int) Math.round(16 * fade);
            if (rimA <= 0) continue;
            context.fill(px, topY + depth - 1, px + 1, topY + depth, rgba(CALC, rimA));
        }
    }

    private static void drawMotes(DrawContext context, int panelX, int panelW,
                                  int topY, int bottomY, long timeMs) {
        int minX = panelX + 5;
        int maxX = panelX + panelW - 5;
        int spanX = Math.max(1, maxX - minX);
        int travel = Math.max(1, (bottomY - topY) / 2);

        for (int i = 0; i < MOTE_COUNT; i++) {
            long cycle = MOTE_CYCLE_MS + i * 431L;
            float progress = Math.floorMod(timeMs + i * 577L, cycle) / (float) cycle;

            int py = bottomY - 2 - Math.round(travel * progress);
            if (py < topY) continue;

            int alpha = Math.round(13 * Math.min(1.0F, progress * 3.0F) * (1.0F - progress));
            if (alpha <= 0) continue;

            int px = minX + Math.floorMod(i * 53 + i * i * 29, spanX);
            px += (int) Math.round(Math.sin(timeMs * 0.0009 + i * 2.11) * 3.0);
            px = clamp(px, minX, maxX - 1);

            context.fill(px, py, px + 1, py + 1, rgba(SPARK, alpha));
        }
    }

    private static int floorCrest(int dx, int baseH) {
        return Math.max(1, baseH
                + (int) Math.round(wobble(dx, 13, 5L) * 5.0)
                + (int) Math.round(wobble(dx, 5, 71L) * 2.0));
    }

    private static int shellDepth(int dx, int reach) {
        return clamp(reach / 3 + 2
                + (int) Math.round(wobble(dx, 7, 23L) * 5.0)
                + (hash01(Math.floorDiv(dx, 2) + 97L) < 0.35 ? 1 : 0)
                + (int) Math.round(wobble(dx, 3, 61L) * 2.0), 2, reach);
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
