package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class TomeMotif implements BackgroundMotif {

    private static final int INK   = 0x3A2A16;
    private static final int SEPIA = 0x6B4E2A;
    private static final int GOLD  = 0xB8862B;
    private static final int FOX   = 0x8A6B3C;

    private static final int  FOX_COUNT      = 7;
    private static final int  LEAF_STEP      = 5;
    private static final long GLINT_CYCLE_MS = 7200L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int topY = y + 3;
        int bottomY = y + h - 3;
        int minX = x + 3;
        int maxX = x + w - 3;
        int span = Math.max(1, bottomY - topY);
        int topReach = clamp(span / 8, 5, 12);
        int botReach = clamp(span / 4, 12, 30);

        double glint = Math.floorMod(timeMs, GLINT_CYCLE_MS) / (double) GLINT_CYCLE_MS;

        drawFibre(context, minX, maxX, topY, bottomY);
        drawFoxing(context, minX, maxX, topY, bottomY);
        drawDeckle(context, minX, maxX, topY, bottomY);
        drawHeadRule(context, minX, maxX, topY, topReach);
        drawFootFlourish(context, minX, maxX, bottomY, botReach, glint);
    }

    private static void drawFibre(DrawContext context, int minX, int maxX, int topY, int bottomY) {
        for (int py = topY; py < bottomY; py++) {
            boolean laid = ((py - topY) % 4) == 0;
            for (int px = minX; px < maxX; px++) {
                if (laid && hash01(px * 7L + py * 131L) < 0.10) {
                    context.fill(px, py, px + 1, py + 1, rgba(SEPIA, 7));
                }
                if (hash01(px * 31L + py * 17L + 5L) < 0.020) {
                    context.fill(px, py, px + 1, py + 1, rgba(INK, 9));
                }
            }
        }
    }

    private static void drawFoxing(DrawContext context, int minX, int maxX, int topY, int bottomY) {
        int spanX = Math.max(1, maxX - minX);
        int spanY = Math.max(1, bottomY - topY);

        for (int i = 0; i < FOX_COUNT; i++) {
            int cx = minX + (int) Math.round(hash01(i * 19L + 3L) * spanX);
            int inset = (int) Math.round(hash01(i * 29L + 7L) * spanY * 0.22);
            int cy = topY + (hash01(i * 23L + 11L) < 0.5 ? inset : spanY - inset);
            int r = 4 + (int) Math.round(hash01(i * 37L + 13L) * 6.0);

            for (int dy = -r; dy <= r; dy++) {
                int py = cy + dy;
                if (py < topY || py >= bottomY) continue;

                for (int dx = -r; dx <= r; dx++) {
                    int px = cx + dx;
                    if (px < minX || px >= maxX) continue;

                    double d = Math.sqrt(dx * dx + dy * dy) / (double) r;
                    if (d >= 1.0) continue;

                    int a = (int) Math.round(9 * (1.0 - d) * (1.0 - d)
                            * (0.6 + hash01(px * 3L + py * 5L) * 0.4));
                    if (a <= 0) continue;
                    context.fill(px, py, px + 1, py + 1, rgba(FOX, a));
                }
            }
        }
    }

    private static void drawDeckle(DrawContext context, int minX, int maxX, int topY, int bottomY) {
        for (int py = topY; py < bottomY; py++) {
            int j = (int) Math.round(wobble(py - topY, 6, 41L) * 2.0);
            int jitter = j > 0 ? 1 + j : 0;

            int lx = minX + jitter;
            context.fill(lx, py, lx + 1, py + 1, rgba(SEPIA, 22));
            context.fill(lx + 1, py, lx + 2, py + 1, rgba(SEPIA, 10));

            int rx = maxX - 1 - jitter;
            context.fill(rx, py, rx + 1, py + 1, rgba(SEPIA, 22));
            context.fill(rx - 1, py, rx, py + 1, rgba(SEPIA, 10));
        }
    }

    private static void drawHeadRule(DrawContext context, int minX, int maxX, int topY, int reach) {
        int y0 = topY + 1;
        context.fill(minX + 6, y0, maxX - 6, y0 + 1, rgba(SEPIA, 62));
        context.fill(minX + 10, y0 + 2, maxX - 10, y0 + 3, rgba(SEPIA, 26));

        for (int n = 0; n < 3; n++) {
            context.fill(minX + 6 + n, y0 + n, minX + 7 + n, y0 + n + 1, rgba(INK, 78));
            context.fill(maxX - 7 - n, y0 + n, maxX - 6 - n, y0 + n + 1, rgba(INK, 78));
        }

        int cx = (minX + maxX) / 2;
        context.fill(cx, y0, cx + 1, y0 + 1, rgba(GOLD, 74));
        context.fill(cx - 1, y0 + 1, cx + 2, y0 + 2, rgba(GOLD, 74));
        context.fill(cx, y0 + 2, cx + 1, y0 + 3, rgba(GOLD, 74));
    }

    private static void drawFootFlourish(DrawContext context, int minX, int maxX,
                                         int bottomY, int reach, double glint) {
        int cx = (minX + maxX) / 2;
        int baseY = bottomY - 2;
        int topLimit = bottomY - reach;
        int r = clamp(Math.min((maxX - minX) / 7, (reach * 2) / 3), 5, 16);
        double glintX = minX - 20.0 + glint * ((maxX - minX) + 40.0);

        drawStem(context, minX, maxX, cx, baseY, r, topLimit, glintX);

        for (int side = -1; side <= 1; side += 2) {
            drawVolute(context, minX, maxX, topLimit, bottomY,
                    cx + side * (r + 4), baseY - (int) Math.round(r * 0.75), r, side, glintX);
        }
    }

    private static void drawVolute(DrawContext context, int minX, int maxX, int topLimit,
                                   int bottomY, int ax, int ay, int r, int side, double glintX) {
        int steps = r * 10;

        for (int i = 0; i < steps; i++) {
            double u = i / (double) (steps - 1);
            double th = u * Math.PI * 2.2;
            double rad = r * (1.0 - u * 0.92);

            int px = ax - side * (int) Math.round(Math.cos(th) * rad);
            int py = ay + (int) Math.round(Math.sin(th) * rad * 0.78);
            if (px < minX || px >= maxX || py < topLimit || py >= bottomY) continue;

            double p = presence(bottomY - 1 - py, bottomY - topLimit);
            int a = (int) Math.round(30 + 40 * p);
            if (a <= 0) continue;
            context.fill(px, py, px + 1, py + 1, rgba(INK, a));

            if ((i % LEAF_STEP) == 0 && u < 0.6) {
                int lx = px - side * (int) Math.round(Math.cos(th + Math.PI / 2) * 2.0);
                int ly = py + (int) Math.round(Math.sin(th + Math.PI / 2) * 1.8);
                if (lx >= minX && lx < maxX && ly >= topLimit && ly < bottomY) {
                    context.fill(lx, ly, lx + 1, ly + 1, rgba(SEPIA, (int) Math.round(a * 0.65)));
                }
            }

            if ((i % (LEAF_STEP * 4)) == 0) {
                double g = Math.max(0.0, 1.0 - Math.abs(px - glintX) / 26.0);
                context.fill(px, py, px + 1, py + 1, rgba(GOLD, (int) Math.round(34 + 44 * g)));
            }
        }
    }

    private static void drawStem(DrawContext context, int minX, int maxX, int cx, int baseY,
                                 int r, int topLimit, double glintX) {
        int half = r + 4;

        for (int dx = -half; dx <= half; dx++) {
            int px = cx + dx;
            if (px < minX || px >= maxX) continue;

            double u = Math.abs(dx) / (double) Math.max(1, half);
            int py = baseY - (int) Math.round((1.0 - u * u) * 4.0);
            if (py < topLimit) continue;

            context.fill(px, py, px + 1, py + 1, rgba(INK, (int) Math.round(60 - 22 * u)));
            if (u < 0.25) {
                context.fill(px, py + 1, px + 1, py + 2, rgba(SEPIA, 26));
            }
        }

        double g = Math.max(0.0, 1.0 - Math.abs(cx - glintX) / 30.0);
        int pipA = (int) Math.round(60 + 40 * g);
        pip(context, cx, baseY - 7, topLimit, pipA);
        pip(context, cx - 1, baseY - 6, topLimit, pipA);
        pip(context, cx + 1, baseY - 6, topLimit, pipA);
        pip(context, cx, baseY - 5, topLimit, pipA);
    }

    private static void pip(DrawContext context, int px, int py, int topLimit, int alpha) {
        if (py < topLimit) return;
        context.fill(px, py, px + 1, py + 1, rgba(GOLD, alpha));
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

    private static int rgba(int rgb, int alpha) {
        int a = clamp(alpha, 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
