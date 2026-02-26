package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class PoisonMotif implements BackgroundMotif {

    private static final int DRIP_COUNT = 8;
    private static final long CYCLE_MS = 3000L;
    private static final int TRAIL_SAMPLES = 12;
    private static final double TRAIL_STEP_MS = 78.0;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 38 || h < 34) return;

        int minX = x + 6, maxX = x + w - 6;
        int minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(12, maxX - minX - 8);

        for (int i = 0; i < 10; i++) {
            int rx = minX + ((i * 17 + i * i * 9) % Math.max(1, spanX));
            int len = 1 + (i % 3);
            int residue = 0x1239862E;
            context.fill(rx, minY, rx + 1, minY + len, residue);
        }

        for (int i = 0; i < DRIP_COUNT; i++) {
            long tLocal = timeMs + i * 211L;
            long cycle = Math.floorDiv(tLocal, CYCLE_MS);
            long phase = Math.floorMod(tLocal, CYCLE_MS);

            int seed = hash(i, cycle);
            int spawnDelay = 90 + positiveMod(seed, 620);
            int life = 1300 + positiveMod(seed >>> 7, 1300);
            if (phase < spawnDelay || phase >= spawnDelay + life) continue;

            double age = phase - spawnDelay;
            double p = age / life;

            double fadeIn = Math.min(1.0, age / (100.0 + positiveMod(seed >>> 14, 140)));
            double fadeOutStart = 0.70 + (positiveMod(seed >>> 22, 20) / 100.0);
            double fadeOut = p < fadeOutStart ? 1.0 : Math.max(0.0, 1.0 - ((p - fadeOutStart) / (1.0 - fadeOutStart)));
            double alpha = fadeIn * fadeOut;
            if (alpha <= 0.02) continue;

            int xBase = minX + 2 + positiveMod(seed >>> 3, spanX);
            double xSway = Math.sin((timeMs * 0.0021) + i * 1.3) * 1.2;
            int px = (int) Math.round(Math.max(minX + 2, Math.min(maxX - 4, xBase + xSway)));

            int yStart = minY + positiveMod(seed >>> 10, 3);
            int yEnd = maxY - 3 - positiveMod(seed >>> 17, 4);
            double fallP = p * p;
            int py = (int) Math.round(yStart + (yEnd - yStart) * fallP);

            drawDrip(context, px, py, p, alpha, seed, minY, maxY);

            for (int s = 1; s <= TRAIL_SAMPLES; s++) {
                double pastAge = age - (s * TRAIL_STEP_MS);
                if (pastAge <= 0.0) break;

                double pastP = pastAge / life;
                long pastTime = (long) (timeMs - (s * TRAIL_STEP_MS));
                double pastSway = Math.sin((pastTime * 0.0021) + i * 1.3) * 1.2;
                int pastX = (int) Math.round(Math.max(minX + 2, Math.min(maxX - 4, xBase + pastSway)));
                double pastFallP = pastP * pastP;
                int pastY = (int) Math.round(yStart + (yEnd - yStart) * pastFallP);

                double linger = 1.0 - (s / (double) (TRAIL_SAMPLES + 2));
                double trailAlpha = alpha * linger * 0.75;
                if (trailAlpha <= 0.01) continue;

                drawTrailBlob(context, pastX, pastY, trailAlpha, seed + s);
            }
        }
    }

    private static void drawDrip(DrawContext context, int x, int y, double p, double alpha, int seed, int minY, int maxY) {
        int trailA = (int) Math.round(6 * alpha);
        int bodyA = (int) Math.round(22 * alpha);
        int coreA = (int) Math.round(34 * alpha);

        int trail = rgba(0x4CBF58, trailA);
        int body = rgba(0x79DA56, bodyA);
        int core = rgba(0xB8F27A, coreA);

        context.fill(x, Math.max(minY, y - 1), x + 1, y, trail);

        context.fill(x - 1, y, x + 2, y + 2, body);
        context.fill(x, y + 1, x + 1, y + 3, body);
        context.fill(x, y + 1, x + 1, y + 2, core);

        if (y > maxY - 7 && ((seed >>> 5) & 3) == 0) {
            int splat = rgba(0x7ED95A, Math.max(4, (int) Math.round(10 * alpha)));
            context.fill(x - 2, y + 2, x + 3, y + 3, splat);
            context.fill(x - 1, y + 3, x + 2, y + 4, splat);
        }
    }

    private static void drawTrailBlob(DrawContext context, int x, int y, double alpha, int seed) {
        int glowA = Math.max(1, (int) Math.round(6 * alpha));
        int bodyA = Math.max(1, (int) Math.round(10 * alpha));
        int glow = rgba(0x4CBF58, glowA);
        int body = rgba(0x79DA56, bodyA);

        int dx = ((seed >>> 2) & 1) == 0 ? 0 : (((seed >>> 3) & 1) == 0 ? -1 : 1);
        context.fill(x - 1 + dx, y, x + 2 + dx, y + 1, glow);
        context.fill(x + dx, y - 1, x + 1 + dx, y + 2, glow);
        context.fill(x + dx, y, x + 1 + dx, y + 1, body);
    }

    private static int hash(int a, long b) {
        long x = (b * 0x9E3779B97F4A7C15L) ^ (a * 0xBF58476DL);
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return (int) x;
    }

    private static int positiveMod(int value, int mod) {
        int r = value % mod;
        return r < 0 ? r + mod : r;
    }

    private static int rgba(int rgb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
