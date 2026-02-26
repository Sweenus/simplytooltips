package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class OceanMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 54 || h < 46) return;

        drawFishAndBubbles(context, x, y, w, h, timeMs);

        int baseY = y + h - 2;
        int flameH = Math.max(14, Math.min(24, h - 20));
        int leftX = x + 10;
        int rightX = x + w - 10;

        int flameCount = w >= 210 ? 4 : 3;

        for (int i = 0; i < flameCount; i++) {
            double along = i / (double) Math.max(1, flameCount - 1);
            int baseCx = (int) Math.round(leftX + along * (rightX - leftX));

            double sizeJitter = 0.58 + ((i * 37) % 41) / 100.0;
            sizeJitter += Math.sin(timeMs * 0.00045 + i * 1.13) * 0.04;
            double randomScale = 1.0 + (((i * 73 + 19) % 201) / 100.0);
            double maxScaleByPanel = Math.max(1.0, (h - 14.0) / Math.max(1.0, flameH));
            double finalScale = Math.min(randomScale, maxScaleByPanel);
            int localH = Math.max(10, (int) Math.round(flameH * sizeJitter * finalScale));
            int localBaseY = baseY - ((i % 3) == 0 ? 1 : 0);
            double phase = i * 0.83;

            int flameX = baseCx;

            drawFlameStack(context, flameX, localBaseY, localH, timeMs, phase, x + 2, x + w - 3, y + 2, y + h - 3);
        }
    }

    private static void drawFlameStack(DrawContext context, int flameX, int baseY, int flameH, long timeMs, double phase,
                                       int clipMinX, int clipMaxX, int clipMinY, int clipMaxY) {
        int w0 = Math.max(1, (int) Math.round(6.0 * (flameH / 24.0)));
        int w1 = Math.max(1, (int) Math.round(5.0 * (flameH / 24.0)));
        int w2 = Math.max(1, (int) Math.round(4.0 * (flameH / 24.0)));
        int w3 = Math.max(1, (int) Math.round(3.0 * (flameH / 24.0)));
        int w4 = 1;

        drawFlameLayer(context, flameX, baseY, flameH + 2, w0, 0x031B3116, 0x04142610, 1.00, timeMs, phase + 0.0, 1.6,
                clipMinX, clipMaxX, clipMinY, clipMaxY);
        drawFlameLayer(context, flameX, baseY, flameH - 1, w1, 0x06243D1E, 0x081E3418, 0.86, timeMs, phase + 0.9, 1.9,
                clipMinX, clipMaxX, clipMinY, clipMaxY);
        drawFlameLayer(context, flameX, baseY, flameH - 4, w2, 0x082E5227, 0x0A274820, 0.73, timeMs, phase + 1.6, 2.2,
                clipMinX, clipMaxX, clipMinY, clipMaxY);
        drawFlameLayer(context, flameX, baseY, flameH - 7, w3, 0x0A3D6A33, 0x0C345C2C, 0.60, timeMs, phase + 2.3, 2.5,
                clipMinX, clipMaxX, clipMinY, clipMaxY);
        drawFlameLayer(context, flameX, baseY, flameH - 10, w4, 0x0E4D8440, 0x0F43763A, 0.52, timeMs, phase + 3.0, 2.9,
                clipMinX, clipMaxX, clipMinY, clipMaxY);

        int topY = baseY - flameH + 6;
        for (int i = 0; i < 2; i++) {
            int px = flameX + (int) Math.round(Math.sin(timeMs * 0.0011 + phase + i * 1.4) * (2.0 + i));
            int py = topY + i * 3 + (int) Math.round(Math.sin(timeMs * 0.00225 + phase + i * 0.8) * 1.0);
            int emberA = 3 + (int) Math.round((Math.sin(timeMs * 0.0035 + phase + i * 1.7) + 1.0) * 1.5);
            int ember = (emberA << 24) | 0x00456F34;
            if (px >= clipMinX && px <= clipMaxX && py >= clipMinY && py <= clipMaxY) {
                context.fill(px, py, px + 1, py + 1, ember);
            }
        }
    }

    private static void drawFishAndBubbles(DrawContext context, int x, int y, int w, int h, long timeMs) {
        int minX = x + 6, maxX = x + w - 6;
        int minY = y + 6, maxY = y + h - 6;

        for (int i = 0; i < 3; i++) {
            double speed = 0.0045 + (i * 0.0008);
            int travel = w + 26;
            int offset = (int) ((timeMs * speed) + i * 31) % travel;
            int fx = maxX + 10 - offset;
            int laneY = minY + 8 + i * Math.max(6, (h - 26) / 3);
            int fy = laneY + (int) Math.round(Math.sin(timeMs * 0.0012 + i * 1.8) * 2.0);

            int fish = 0x063B7F78;
            int fishCore = 0x0A63B2A9;
            drawFish(context, fx, fy, fish, fishCore);
        }

        final long cycleMs = 3400L;
        for (int i = 0; i < 5; i++) {
            long tLocal = timeMs + i * 271L;
            long cycle = Math.floorDiv(tLocal, cycleMs);
            long phase = Math.floorMod(tLocal, cycleMs);

            int seed = hash(i, cycle);
            int delay = 120 + positiveMod(seed, 1700);
            int life = 700 + positiveMod(seed >>> 7, 900);
            if (phase < delay || phase >= delay + life) continue;

            double age = phase - delay;
            double p = age / life;
            double fadeIn = Math.min(1.0, age / 120.0);
            double fadeOut = p < 0.72 ? 1.0 : Math.max(0.0, 1.0 - ((p - 0.72) / 0.28));
            double alpha = fadeIn * fadeOut;
            if (alpha <= 0.02) continue;

            int bxBase = minX + 6 + positiveMod(seed >>> 3, Math.max(1, w - 20));
            double sway = Math.sin((timeMs * 0.0017) + i * 1.9) * 1.2;
            int bx = (int) Math.round(bxBase + sway);

            int byStart = maxY - 2;
            int byEnd = minY + 8 + positiveMod(seed >>> 13, Math.max(1, (h / 3)));
            int by = (int) Math.round(byStart - (byStart - byEnd) * p);

            int ringA = Math.max(1, (int) Math.round(8 * alpha));
            int coreA = Math.max(1, (int) Math.round(4 * alpha));
            int ring = (ringA << 24) | 0x008CC8C2;
            int core = (coreA << 24) | 0x00B4E7E0;

            context.fill(bx, by, bx + 1, by + 1, ring);
            context.fill(bx - 1, by, bx, by + 1, ring);
            context.fill(bx + 1, by, bx + 2, by + 1, ring);
            context.fill(bx, by - 1, bx + 1, by, ring);
            context.fill(bx, by + 1, bx + 1, by + 2, ring);
            context.fill(bx, by, bx + 1, by + 1, core);
        }
    }

    private static void drawFish(DrawContext context, int x, int y, int body, int core) {
        context.fill(x, y, x + 3, y + 1, body);
        context.fill(x + 1, y - 1, x + 3, y, body);
        context.fill(x + 1, y + 1, x + 3, y + 2, body);
        context.fill(x + 3, y, x + 5, y + 1, body);
        context.fill(x + 4, y - 1, x + 5, y + 2, body);
        context.fill(x + 1, y, x + 2, y + 1, core);
    }

    private static void drawFlameLayer(DrawContext context, int cx, int baseY, int height, int halfWidth,
                                       int sideColor, int coreColor, double taperPow,
                                       long timeMs, double phase, double bendPower,
                                       int clipMinX, int clipMaxX, int clipMinY, int clipMaxY) {
        if (height <= 2 || halfWidth <= 0) return;

        for (int row = 0; row < height; row++) {
            double p = row / (double) (height - 1);
            double taper = Math.pow(1.0 - p, taperPow);
            int hw = Math.max(1, (int) Math.round(halfWidth * taper));

            double bendScale = Math.pow(p, bendPower);
            double bendA = Math.sin((timeMs * 0.0031) + phase + (p * 7.0)) * (1.8 + p * 1.8);
            double bendB = Math.sin((timeMs * 0.0064) + phase * 1.7 + (p * 13.0)) * (0.8 + p * 1.1);

            double lowerFlow = Math.sin((timeMs * 0.0022) + phase * 2.2 + row * 0.63) * (1.2 + (1.0 - p) * 2.0);
            long tick = timeMs / 340L;
            double tickFrac = (timeMs % 340L) / 340.0;
            double noiseA = rowNoise(row, phase, tick);
            double noiseB = rowNoise(row, phase, tick + 1L);
            double randomFlow = (noiseA + (noiseB - noiseA) * tickFrac) * (0.9 + (1.0 - p) * 2.4);

            int rowCx = cx + (int) Math.round(((bendA + bendB) * bendScale) + lowerFlow + randomFlow);

            if ((row % 5) == 0) hw += 1;
            if ((row % 7) == 0) hw = Math.max(1, hw - 1);

            int y = baseY - row;
            if (y < clipMinY || y > clipMaxY) continue;

            int left = Math.max(clipMinX, rowCx - hw);
            int right = Math.min(clipMaxX + 1, rowCx + hw + 1);
            if (right > left) {
                context.fill(left, y, right, y + 1, sideColor);
            }

            int coreW = Math.max(1, hw - 2);
            int coreL = Math.max(clipMinX, rowCx - coreW);
            int coreR = Math.min(clipMaxX + 1, rowCx + coreW + 1);
            if (coreR > coreL) {
                context.fill(coreL, y, coreR, y + 1, coreColor);
            }

            if (p > 0.72 && hw > 2 && (row % 2 == 0)
                    && rowCx - 1 >= clipMinX && rowCx + 1 <= clipMaxX + 1) {
                context.fill(rowCx - 1, y, rowCx + 1, y + 1, 0x08000000);
            }
        }
    }

    private static double rowNoise(int row, double phase, long tick) {
        long s = ((long) row * 0x9E3779B97F4A7C15L) ^ (tick * 0xBF58476D1CE4E5B9L) ^ (long) (phase * 10000.0);
        s ^= (s >>> 30);
        s *= 0xBF58476D1CE4E5B9L;
        s ^= (s >>> 27);
        s *= 0x94D049BB133111EBL;
        s ^= (s >>> 31);
        int bits = (int) (s & 0xFFFF);
        return (bits / 32767.5) - 1.0;
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
}
