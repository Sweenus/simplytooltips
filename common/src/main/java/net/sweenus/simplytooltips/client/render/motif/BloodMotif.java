package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class BloodMotif implements BackgroundMotif {

    private static final int DRIP_COUNT = 7;
    private static final long CYCLE_MS = 4200L;
    private static final int TRAIL_SAMPLES = 12;
    private static final double TRAIL_STEP_MS = 78.0;
    private static final double FALL_EXPONENT = 2.4;

    private static final int CLOT = 0x5E0A10;
    private static final int CRIMSON = 0xB01821;
    private static final int ARTERIAL = 0xE8535A;
    private static final int SPLAT = 0x8E1119;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 38 || h < 34) return;

        int minX = x + 6, maxX = x + w - 6;
        int minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(12, maxX - minX - 8);

        for (int i = 0; i < 10; i++) {
            int rx = minX + ((i * 17 + i * i * 9) % Math.max(1, spanX));
            int len = 1 + (i % 3);
            int residue = 0x127A1B22;
            context.fill(rx, minY, rx + 1, minY + len, residue);
        }

        for (int i = 0; i < DRIP_COUNT; i++) {
            drawPool(context, i, timeMs, minX, spanX, maxY);
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
            double fallP = Math.pow(p, FALL_EXPONENT);
            int py = (int) Math.round(yStart + (yEnd - yStart) * fallP);

            drawDrip(context, px, py, p, alpha, seed, minY, maxY);

            for (int s = 1; s <= TRAIL_SAMPLES; s++) {
                double pastAge = age - (s * TRAIL_STEP_MS);
                if (pastAge <= 0.0) break;

                double pastP = pastAge / life;
                long pastTime = (long) (timeMs - (s * TRAIL_STEP_MS));
                double pastSway = Math.sin((pastTime * 0.0021) + i * 1.3) * 1.2;
                int pastX = (int) Math.round(Math.max(minX + 2, Math.min(maxX - 4, xBase + pastSway)));
                double pastFallP = Math.pow(pastP, FALL_EXPONENT);
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

        int trail = rgba(CLOT, trailA);
        int body = rgba(CRIMSON, bodyA);
        int core = rgba(ARTERIAL, coreA);

        context.fill(x, Math.max(minY, y - 1), x + 1, y, trail);

        context.fill(x - 1, y, x + 2, y + 3, body);
        context.fill(x, y + 3, x + 1, y + 5, body);
        context.fill(x, y + 1, x + 1, y + 3, core);

        if (y > maxY - 7 && ((seed >>> 5) & 3) == 0) {
            int splat = rgba(SPLAT, Math.max(4, (int) Math.round(10 * alpha)));
            context.fill(x - 2, y + 3, x + 3, y + 4, splat);
            context.fill(x - 1, y + 4, x + 2, y + 5, splat);
        }
    }

    private static void drawPool(DrawContext context, int i, long timeMs, int minX, int spanX, int maxY) {
        long tLocal = timeMs + i * 211L;
        long cycle = Math.floorDiv(tLocal, CYCLE_MS);
        long phase = Math.floorMod(tLocal, CYCLE_MS);

        int seed = hash(i, cycle);
        int spawnDelay = 90 + positiveMod(seed, 620);
        int life = 1300 + positiveMod(seed >>> 7, 1300);

        double landAt = spawnDelay + life * 0.86;
        double poolLife = CYCLE_MS - landAt;
        if (phase < landAt || poolLife <= 1.0) return;

        double q = (phase - landAt) / poolLife;
        double grow = Math.min(1.0, q / 0.25);
        double fade = q < 0.55 ? 1.0 : Math.max(0.0, 1.0 - ((q - 0.55) / 0.45));
        double alpha = grow * fade;
        if (alpha <= 0.02) return;

        int px = minX + 2 + positiveMod(seed >>> 3, spanX);
        int halfW = 1 + (int) Math.round(grow * 1.5);
        int poolY = Math.min(maxY - 2, maxY - 3 - positiveMod(seed >>> 17, 4) + 4);

        int spread = rgba(CRIMSON, Math.max(1, (int) Math.round(14 * alpha)));
        int edge = rgba(CLOT, Math.max(1, (int) Math.round(9 * alpha)));

        context.fill(px - halfW, poolY, px + halfW + 1, poolY + 1, spread);
        context.fill(px - halfW + 1, poolY + 1, px + halfW, poolY + 2, edge);
    }

    private static void drawTrailBlob(DrawContext context, int x, int y, double alpha, int seed) {
        int glowA = Math.max(1, (int) Math.round(6 * alpha));
        int bodyA = Math.max(1, (int) Math.round(10 * alpha));
        int glow = rgba(CLOT, glowA);
        int body = rgba(CRIMSON, bodyA);

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
