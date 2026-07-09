package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class CosmicMotif implements BackgroundMotif {

    private static final int CONSTELLATION_SLOTS = 4;
    private static final double ANIMATION_SPEED = 2.0 / 3.0;
    private static final long CONSTELLATION_LIFETIME_MS = 4200L;
    private static final long CONSTELLATION_STAGGER_MS = 1100L;
    private static final int MAX_NODES = 6;
    private static final int[][] SHAPE_A = {
            {-18, 8}, {-7, -6}, {5, -1}, {16, -13}, {22, 5}
    };
    private static final int[][] SHAPE_B = {
            {-14, -12}, {-5, 5}, {7, -3}, {15, 12}
    };
    private static final int[][] SHAPE_C = {
            {-20, 10}, {-7, -4}, {5, 8}, {18, -6}, {24, 10}, {2, -15}
    };
    private static final int[][][] SHAPES = {SHAPE_A, SHAPE_B, SHAPE_C};

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int minX = x + 8;
        int maxX = x + w - 8;
        int minY = y + 8;
        int maxY = y + h - 8;
        int spanX = Math.max(1, maxX - minX);
        int spanY = Math.max(1, maxY - minY);
        long animationTimeMs = (long) (timeMs * ANIMATION_SPEED);

        for (int i = 0; i < CONSTELLATION_SLOTS; i++) {
            drawConstellation(context, minX, minY, spanX, spanY, animationTimeMs, i);
        }

        for (int i = 0; i < 10; i++) {
            int px = minX + Math.floorMod(i * 47 + i * i * 13, spanX);
            int py = minY + Math.floorMod(i * 31 + i * i * 17, spanY);
            double twinkle = (Math.sin((animationTimeMs * 0.0032) + i * 1.73) + 1.0) * 0.5;
            int alpha = 0x18 + (int) (twinkle * 0x20);
            context.fill(px, py, px + 1, py + 1, (alpha << 24) | 0x00F4FBFF);
        }
    }

    private void drawConstellation(DrawContext context, int minX, int minY, int spanX, int spanY, long timeMs, int slot) {
        long localTime = timeMs + slot * CONSTELLATION_STAGGER_MS;
        long generation = Math.floorDiv(localTime, CONSTELLATION_LIFETIME_MS);
        long ageMs = Math.floorMod(localTime, CONSTELLATION_LIFETIME_MS);
        float alpha = constellationAlpha(ageMs);
        if (alpha <= 0.01F) return;

        int seed = mix((int) generation * 0x45D9F3B + slot * 0x119DE1F3);
        int[][] points = SHAPES[Math.floorMod(seed, SHAPES.length)];
        int nodeCount = Math.min(points.length, 3 + Math.floorMod(seed >>> 5, Math.min(MAX_NODES, points.length) - 2));
        int margin = Math.max(12, Math.min(spanX, spanY) / 6);
        int centerSpanX = Math.max(1, spanX - margin * 2);
        int centerSpanY = Math.max(1, spanY - margin * 2);
        int centerX = minX + margin + Math.floorMod(seed >>> 8, centerSpanX);
        int centerY = minY + margin + Math.floorMod(seed >>> 17, centerSpanY);
        float scale = 0.64F + (Math.floorMod(seed >>> 3, 40) / 100.0F);
        double ageRatio = ageMs / (double) CONSTELLATION_LIFETIME_MS;
        double formationAngle = Math.sin(ageRatio * Math.PI * 2.0 + (seed & 0xFF) * 0.017) * 0.04;
        double driftA = Math.sin((timeMs * 0.00055) + slot * 1.91) * 1.4;
        double driftB = Math.cos((timeMs * 0.00042) + slot * 2.37) * 1.0;
        double separation = 1.0 + ageRatio * 0.08;

        int[] xs = new int[nodeCount];
        int[] ys = new int[nodeCount];
        double[] depths = new double[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            double px = points[i][0] * scale * separation;
            double py = points[i][1] * scale * separation;
            double baseX = centerX + driftA + px * Math.cos(formationAngle) - py * Math.sin(formationAngle);
            double baseY = centerY + driftB + px * Math.sin(formationAngle) + py * Math.cos(formationAngle);
            double nodeSpeed = 0.35 + Math.floorMod(seed >>> (i + 4), 44) / 100.0;
            double orbitPhase = timeMs * 0.0026 * nodeSpeed + slot * 1.21 + i * 2.399 + (seed & 0x3F) * 0.031;
            double verticalOrbit = Math.cos(orbitPhase * 0.86 + i * 0.47) * (0.42 + nodeSpeed * 0.34);
            double depth = Math.sin(orbitPhase + i * 0.61);
            depths[i] = depth;
            xs[i] = clamp((int) Math.round(baseX), minX + 1, minX + spanX - 2);
            ys[i] = clamp((int) Math.round(baseY + verticalOrbit - depth * 0.72), minY + 1, minY + spanY - 2);
        }

        int previousX = -1;
        int previousY = -1;
        float lineAlphaFactor = constellationLineAlpha(ageMs) * 0.78F;
        int lineAlpha = (int) (0x38 * alpha * lineAlphaFactor);
        int shadowAlpha = (int) (0x16 * alpha * lineAlphaFactor);
        int lineColor = (lineAlpha << 24) | 0x0094D7FF;
        int shadowColor = (shadowAlpha << 24) | 0x006ED4FF;

        for (int i = 0; i < nodeCount; i++) {
            int px = xs[i];
            int py = ys[i];
            double depth = depths[i];
            double depthScale = 0.82 + ((depth + 1.0) * 0.14);

            if (previousX >= 0) {
                drawLine(context, previousX, previousY, px, py, lineColor);
                drawLine(context, previousX, previousY + 1, px, py + 1, shadowColor);
            }

            double twinkle = (Math.sin((timeMs * 0.0028) + (i + slot * 5) * 1.41) + 1.0) * 0.5;
            int glowAlpha = (int) ((0x18 + twinkle * 0x22) * alpha * depthScale);
            int coreAlpha = (int) ((0x82 + twinkle * 0x50) * alpha * depthScale);

            if (depth > 0.62) {
                context.fill(px - 2, py, px + 3, py + 1, (glowAlpha << 24) | 0x008EDBFF);
                context.fill(px, py - 2, px + 1, py + 3, (glowAlpha << 24) | 0x008EDBFF);
            } else {
                context.fill(px - 1, py, px + 2, py + 1, (glowAlpha << 24) | 0x008EDBFF);
                context.fill(px, py - 1, px + 1, py + 2, (glowAlpha << 24) | 0x008EDBFF);
            }
            context.fill(px, py, px + 1, py + 1, (coreAlpha << 24) | 0x00F8FDFF);
            if (depth > 0.78) {
                context.fill(px + 1, py, px + 2, py + 1, (coreAlpha << 24) | 0x00F8FDFF);
            }
            drawGlint(context, px, py, alpha, ageMs, seed, slot, i);

            previousX = px;
            previousY = py;
        }
    }

    private float constellationAlpha(long ageMs) {
        float fadeIn = Math.min(1.0F, ageMs / 700.0F);
        float fadeOut = Math.min(1.0F, Math.max(0.0F, (CONSTELLATION_LIFETIME_MS - ageMs) / 1100.0F));
        return Math.max(0.0F, Math.min(fadeIn, fadeOut));
    }

    private float constellationLineAlpha(long ageMs) {
        float delayedFadeIn = Math.min(1.0F, Math.max(0.0F, (ageMs - 420L) / 900.0F));
        float fadeOut = Math.min(1.0F, Math.max(0.0F, (CONSTELLATION_LIFETIME_MS - ageMs) / 1300.0F));
        return Math.max(0.0F, Math.min(delayedFadeIn, fadeOut));
    }

    private void drawGlint(DrawContext context, int x, int y, float constellationAlpha, long ageMs, int seed, int slot, int node) {
        int glintWindow = 180;
        int glintCycle = 2600 + Math.floorMod(seed >>> (node + 6), 900);
        int glintAge = Math.floorMod((int) ageMs + slot * 431 + node * 719, glintCycle);
        if (glintAge >= glintWindow) return;

        float glintAlpha = (float) Math.sin((glintAge / (float) glintWindow) * Math.PI) * constellationAlpha;
        int color = ((int) (0x54 * glintAlpha) << 24) | 0x00F8FDFF;
        context.fill(x - 2, y, x + 3, y + 1, color);
        context.fill(x, y - 2, x + 1, y + 3, color);
    }

    private void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;

        while (true) {
            context.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        value *= 0x846CA68B;
        value ^= value >>> 16;
        return value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
