package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class CorruptedEyeMotif implements BackgroundMotif {

    private static final int EYE_COUNT = 8;
    private static final long BLINK_CYCLE_MS = 4200L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int minX = x + 8;
        int maxX = x + w - 8;
        int minY = y + 8;
        int maxY = y + h - 8;
        int spanX = Math.max(1, maxX - minX - 8);
        int spanY = Math.max(1, maxY - minY - 6);

        for (int i = 0; i < 12; i++) {
            int baseX = minX + 3 + Math.floorMod(i * 37 + i * i * 11, spanX);
            int baseY = minY + 2 + Math.floorMod(i * 23 + i * i * 7, spanY);
            int sway = (int) Math.round(Math.sin(timeMs * 0.0007 + i * 1.63) * 2.0);
            int vein = rgba(0x4A1D63, 5 + (i % 3) * 2);
            int shadow = rgba(0x100617, 8 + (i % 2) * 3);

            context.fill(baseX - 2, baseY + sway, baseX + 3, baseY + sway + 1, vein);
            context.fill(baseX, baseY + sway - 1, baseX + 5, baseY + sway, shadow);
            if ((i & 1) == 0) {
                context.fill(baseX + 2, baseY + sway + 1, baseX + 3, baseY + sway + 3, vein);
            }
        }

        for (int i = 0; i < EYE_COUNT; i++) {
            int eyeW = (i % 3 == 0) ? 7 : 5;
            int px = minX + 4 + Math.floorMod(i * 43 + i * i * 13, spanX);
            int py = minY + 3 + Math.floorMod(i * 29 + i * i * 17, spanY);
            px = clamp(px, minX + eyeW / 2 + 1, maxX - eyeW / 2 - 2);
            py = clamp(py, minY + 2, maxY - 3);

            long cycle = BLINK_CYCLE_MS + i * 173L;
            long phase = Math.floorMod(timeMs + i * 719L, cycle);
            float openness = blinkOpenness(phase, cycle);
            float pulse = (float) ((Math.sin(timeMs * 0.0021 + i * 1.41) + 1.0) * 0.5);
            drawEye(context, px, py, eyeW, openness, pulse, timeMs, i);
        }
    }

    private static void drawEye(DrawContext context, int x, int y, int width, float openness,
                                float pulse, long timeMs, int index) {
        int half = width / 2;
        int shadow = rgba(0x09030D, 31 + Math.round(pulse * 12));
        int lid = rgba(0x713699, 30 + Math.round(pulse * 17));
        int iris = rgba(0xE8B62E, 38 + Math.round(pulse * 30));
        int glow = rgba(0xFFD85A, 24 + Math.round(pulse * 22));
        int pupil = rgba(0x050207, 62 + Math.round(pulse * 28));

        context.fill(x - half - 1, y, x + half + 2, y + 1, shadow);

        if (openness < 0.34F) {
            context.fill(x - half, y, x + half + 1, y + 1, lid);
            context.fill(x - half + 1, y + 1, x + half, y + 2, shadow);
            return;
        }

        context.fill(x - half, y - 1, x + half + 1, y, lid);
        context.fill(x - half - 1, y, x + half + 2, y + 1, iris);
        context.fill(x - half, y + 1, x + half + 1, y + 2, lid);
        context.fill(x - half + 1, y - 1, x + half, y, glow);

        int pupilOffset = (int) Math.round(Math.sin(timeMs * 0.0011 + index * 1.77));
        int pupilX = clamp(x + pupilOffset, x - half + 1, x + half - 1);
        context.fill(pupilX, y - 1, pupilX + 1, y + 2, pupil);
        context.fill(pupilX - 1, y, pupilX + 2, y + 1, pupil);
        context.fill(pupilX, y, pupilX + 1, y + 1, glow);
    }

    private static float blinkOpenness(long phase, long cycle) {
        long blinkStart = cycle - 320L;
        if (phase < blinkStart) return 1.0F;
        float blink = (phase - blinkStart) / 320.0F;
        return Math.abs(blink * 2.0F - 1.0F);
    }

    private static int rgba(int rgb, int alpha) {
        int a = clamp(alpha, 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
