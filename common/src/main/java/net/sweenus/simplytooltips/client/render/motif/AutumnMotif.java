package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

/**
 * Autumn background motif:
 * - Swaying tree trunks/silhouettes in the background
 * - Soft horizontal wind gust streaks
 * - Drifting leaves carried by wind
 */
public class AutumnMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;

        int minX = x + 5, maxX = x + w - 5;
        int minY = y + 5, maxY = y + h - 5;

        drawSwayingTrees(context, minX, maxX, minY, maxY, w, h, timeMs);
        drawWindGusts(context, minX, maxX, minY, maxY, w, timeMs);
        drawLeaves(context, minX, maxX, minY, maxY, w, timeMs);
    }

    private static void drawSwayingTrees(DrawContext context, int minX, int maxX, int minY, int maxY,
                                         int panelW, int panelH, long timeMs) {
        int baseY = maxY;
        int treeCount = 5;
        int span = Math.max(12, maxX - minX - 8);

        for (int i = 0; i < treeCount; i++) {
            int lane = (i * 53 + 19) % span;
            int trunkX = minX + 3 + lane;
            int trunkH = Math.max(12, (int) (panelH * (0.26 + i * 0.03)));
            int sway = (int) Math.round(Math.sin(timeMs * 0.0014 + i * 1.3) * (1.2 + (i % 2)));

            int x0 = Math.max(minX + 1, Math.min(maxX - 2, trunkX + sway));
            int y0 = Math.max(minY, baseY - trunkH);

            int trunk = 0x223A2718;
            int bark = 0x1A2A1A10;
            int canopy = 0x184B311B;

            context.fill(x0, y0, x0 + 2, baseY, trunk);
            context.fill(x0 + 1, y0 + 1, x0 + 2, baseY, bark);

            int crownW = 7;
            int crownH = 4;
            context.fill(x0 - crownW / 2, y0 - crownH, x0 + crownW / 2 + 2, y0 - 1, canopy);
        }
    }

    private static void drawWindGusts(DrawContext context, int minX, int maxX, int minY, int maxY,
                                      int panelW, long timeMs) {
        int width = Math.max(14, maxX - minX - 10);
        int height = Math.max(14, maxY - minY - 10);

        for (int i = 0; i < 4; i++) {
            // Per-emitter pseudo-random event windows (location + timing vary each window).
            long windowMs = 3600L;
            long t = timeMs + i * 911L;
            long windowIdx = Math.floorDiv(t, windowMs);
            long inWindow = Math.floorMod(t, windowMs);

            int seed = hash32(windowIdx * 1315423911L + i * 2654435761L);

            long startDelayMs = randRange(seed ^ 0x1A2B3C4D, 80, 1500);
            long growMs       = randRange(seed ^ 0x39A17C5E, 700, 1280);
            long holdMs       = randRange(seed ^ 0x5F356495, 220, 760);
            long fadeMs       = randRange(seed ^ 0x764BD112, 320, 920);
            long activeMs = growMs + holdMs + fadeMs;

            // Random gap between gusts by delaying start and skipping when outside active span.
            long phase = inWindow - startDelayMs;
            if (phase < 0 || phase >= activeMs) continue;

            int gx = minX + 2 + randRange(seed ^ 0x2F71A8C3, 0, Math.max(8, width - 16));
            int gy = minY + 6 + randRange(seed ^ 0x4C8E2D19, 0, Math.max(8, height - 10));

            double growth;
            double fade = 1.0;
            if (phase < growMs) {
                double p = phase / (double) growMs;
                growth = p * p * (3.0 - 2.0 * p); // smoothstep
            } else {
                growth = 1.0;
                if (phase > growMs + holdMs) {
                    double fp = (phase - growMs - holdMs) / (double) Math.max(1L, fadeMs);
                    fade = Math.max(0.0, 1.0 - fp);
                }
            }

            int alpha = (int) ((0x12 + i * 2) * fade);
            if (alpha <= 1) continue;
            int baseAlpha = Math.max(0x06, Math.min(0x20, alpha));

            int maxLen = 14 + i * 3;
            int lastIdx = Math.max(1, maxLen - 1);

            // Swirl anchor at 75% of the path so the last quarter curls/spirals.
            double swirlStartT = 0.75;
            int swirlBaseX = gx + (int) Math.round(lastIdx * swirlStartT);
            int swirlBaseY = gy
                    - (int) Math.round((swirlStartT * swirlStartT) * (2.4 + 2.1 * growth))
                    + (int) Math.round(Math.sin(swirlStartT * 2.4 + i * 0.55) * (0.7 + 1.3 * growth));

            for (int s = 0; s <= lastIdx; s++) {
                double pathT = s / (double) lastIdx;

                // Shape grows from one pixel outward.
                if (pathT > growth) continue;

                int px;
                int py;
                if (pathT < swirlStartT) {
                    // Body curves upward with mild waviness.
                    px = gx + s;
                    py = gy
                            - (int) Math.round((pathT * pathT) * (2.4 + 2.1 * growth))
                            + (int) Math.round(Math.sin(pathT * 2.4 + i * 0.55) * (0.7 + 1.3 * growth));
                } else {
                    // Last quarter transitions into a spiral that curls back inward.
                    double q = (pathT - swirlStartT) / (1.0 - swirlStartT); // 0..1 in final quarter
                    double a = (Math.PI * 0.20) + (Math.PI * 2.6 * q);
                    double r = (3.2 + 2.1 * growth) * (1.0 - q);
                    px = swirlBaseX + (int) Math.round((q * (maxLen * 0.18)) + Math.cos(a) * r);
                    py = swirlBaseY - (int) Math.round(Math.sin(a) * r);
                }

                // Fade from gust start over time: older start segments decay as growth advances.
                double age = growth - pathT; // larger near the origin as gust matures
                double ageFade = Math.max(0.05, 1.0 - (age * 1.55));

                // Additional along-path fade so brightness naturally decreases toward the end.
                double tailFade = 1.0 - (pathT * 0.72);

                int segAlpha = Math.max(0x03, (int) Math.round(baseAlpha * ageFade * tailFade));
                int gust = (segAlpha << 24) | 0x00D8C2A0;

                context.fill(px, py, px + 1, py + 1, gust);
                if ((s % 3) == 0) context.fill(px, py + 1, px + 1, py + 2, gust & 0xCCFFFFFF);
            }
        }
    }

    private static int hash32(long v) {
        long x = v;
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return (int) x;
    }

    private static int randRange(int seed, int minInclusive, int maxInclusive) {
        int span = Math.max(1, (maxInclusive - minInclusive + 1));
        return minInclusive + Math.floorMod(seed, span);
    }

    private static void drawLeaves(DrawContext context, int minX, int maxX, int minY, int maxY,
                                   int panelW, long timeMs) {
        int spanX = Math.max(12, (maxX - minX) - 8);
        int spanY = Math.max(10, (maxY - minY) - 8);

        for (int i = 0; i < 11; i++) {
            double windSpeed = 0.011 + i * 0.00095;
            double adv = (timeMs * windSpeed) + i * 31;
            int px = minX + 2 + (int) (adv % spanX);

            int laneY = (i * 17 + i * i * 5) % spanY;
            double waveA = Math.sin((timeMs * 0.0031) + i * 1.2) * 2.2;
            double waveB = Math.sin((timeMs * 0.0015) + i * 0.4) * (panelW * 0.018);
            int py = minY + 2 + laneY + (int) Math.round(waveA + waveB);
            py = Math.max(minY + 1, Math.min(maxY - 4, py));

            int[] palette = {0x2AD66A2A, 0x2ACC7A29, 0x2ABF4F1E, 0x2AB88A2F};
            int leaf = palette[i % palette.length];
            int vein = 0x1A5B361A;

            int rot = Math.floorMod(i + (int) (adv / 18.0), 4);
            switch (rot) {
                case 1 -> {
                    context.fill(px, py, px + 1, py + 3, leaf);
                    context.fill(px + 1, py + 1, px + 2, py + 3, leaf);
                    context.fill(px, py + 1, px + 1, py + 2, vein);
                }
                case 2 -> {
                    context.fill(px, py, px + 3, py + 1, leaf);
                    context.fill(px + 1, py + 1, px + 3, py + 2, leaf);
                    context.fill(px + 1, py, px + 2, py + 1, vein);
                }
                case 3 -> {
                    context.fill(px + 1, py, px + 2, py + 3, leaf);
                    context.fill(px, py + 1, px + 1, py + 3, leaf);
                    context.fill(px + 1, py + 1, px + 2, py + 2, vein);
                }
                default -> {
                    context.fill(px, py + 1, px + 3, py + 2, leaf);
                    context.fill(px, py, px + 2, py + 1, leaf);
                    context.fill(px + 1, py + 1, px + 2, py + 2, vein);
                }
            }
        }
    }
}
