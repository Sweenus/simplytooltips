package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class SoulMotif implements BackgroundMotif {

    private static final int FISSURE_COUNT = 4;
    private static final int SPARK_COUNT = 14;
    private static final long SPARK_CYCLE_MS = 3200L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;

        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;

        for (int i = 0; i < FISSURE_COUNT; i++) {
            drawFissure(context, minX, maxX, minY, maxY, i, timeMs);
        }

        for (int i = 0; i < SPARK_COUNT; i++) {
            drawSpark(context, minX, maxX, minY, maxY, i, timeMs);
        }
    }

    private static void drawFissure(DrawContext context, int minX, int maxX, int minY, int maxY, int idx, long timeMs) {
        int spanX = Math.max(12, maxX - minX - 8);
        int baseX = minX + 4 + positiveMod(hash(idx * 31, 17), spanX);
        int height = Math.max(12, maxY - minY - 6);
        int y0 = minY + 2;

        for (int step = 0; step < height; step += 2) {
            double p = step / (double) Math.max(1, height - 1);
            double sway = Math.sin((timeMs * 0.0017) + idx * 1.9 + p * 8.0) * (1.8 + p * 2.8);
            int x = (int) Math.round(baseX + sway);
            int y = y0 + step;

            int alpha = (int) Math.round(8.0 * (1.0 - p));
            int body = rgba(0x58C8BD, alpha);
            context.fill(x, y, x + 1, y + 2, body);

            if ((step / 2 + idx) % 3 == 0) {
                int branchA = (int) Math.round(6.0 * (1.0 - p));
                int branch = rgba(0x76DFD5, branchA);
                context.fill(x - 1, y + 1, x, y + 2, branch);
                context.fill(x + 1, y + 1, x + 2, y + 2, branch);
            }
        }

        int halo = rgba(0x9AEDE5, 7);
        context.fill(baseX - 2, y0 - 1, baseX + 3, y0, halo);
        context.fill(baseX - 1, y0 - 2, baseX + 2, y0 - 1, halo);
    }

    private static void drawSpark(DrawContext context, int minX, int maxX, int minY, int maxY, int idx, long timeMs) {
        long tLocal = timeMs + idx * 197L;
        long cycle = Math.floorDiv(tLocal, SPARK_CYCLE_MS);
        long phase = Math.floorMod(tLocal, SPARK_CYCLE_MS);

        int seed = hash(idx, cycle);
        int delay = 90 + positiveMod(seed, 760);
        int life = 1100 + positiveMod(seed >>> 8, 1450);
        if (phase < delay || phase >= delay + life) return;

        double age = phase - delay;
        double p = age / life;

        double fadeIn = Math.min(1.0, age / (130.0 + positiveMod(seed >>> 16, 160)));
        double fadeOutStart = 0.60 + positiveMod(seed >>> 24, 24) / 100.0;
        double fadeOut = p < fadeOutStart ? 1.0 : Math.max(0.0, 1.0 - ((p - fadeOutStart) / (1.0 - fadeOutStart)));
        double alpha = fadeIn * fadeOut;
        if (alpha <= 0.01) return;

        int spanX = Math.max(8, maxX - minX - 10);
        int spanY = Math.max(10, maxY - minY - 14);
        int startX = minX + 5 + positiveMod(seed >>> 3, spanX);
        int startY = minY + 4 + positiveMod(seed >>> 11, 8);
        int endY = minY + 10 + positiveMod(seed >>> 19, spanY);

        double drift = ((positiveMod(seed >>> 27, 200) / 100.0) - 1.0) * 4.0;
        double phaseA = positiveMod(seed >>> 7, 628) / 100.0;
        double phaseB = positiveMod(seed >>> 13, 628) / 100.0;
        double x = startX + drift * p
                + Math.sin(age * 0.010 + phaseA) * 2.0
                + Math.sin(age * 0.021 + phaseB) * 1.0;
        double y = startY + (endY - startY) * p;

        int px = (int) Math.round(Math.max(minX + 1, Math.min(maxX - 4, x)));
        int py = (int) Math.round(Math.max(minY + 1, Math.min(maxY - 4, y)));

        int glow = rgba(0x5FCFC4, (int) Math.round(7 * alpha));
        int body = rgba(0x86E8DF, (int) Math.round(13 * alpha));
        int core = rgba(0xD8FFFB, (int) Math.round(18 * alpha));

        context.fill(px - 1, py, px + 2, py + 1, glow);
        context.fill(px, py - 1, px + 1, py + 2, glow);
        context.fill(px, py, px + 1, py + 1, body);
        context.fill(px, py, px + 1, py + 1, core);

        int wake = rgba(0x6ED9CE, (int) Math.round(8 * alpha));
        context.fill(px, py + 1, px + 1, py + 2, wake);
        if ((idx & 1) == 0) {
            context.fill(px - 1, py + 1, px, py + 2, wake);
        } else {
            context.fill(px + 1, py + 1, px + 2, py + 2, wake);
        }
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
