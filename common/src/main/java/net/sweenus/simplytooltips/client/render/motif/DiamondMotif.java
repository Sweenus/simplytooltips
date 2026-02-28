package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class DiamondMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6;
        int maxX = x + w - 6;
        int minY = y + 6;
        int maxY = y + h - 6;
        final long windowMs = 920L;
        long window = timeMs / windowMs;
        float chance = hash01(window * 43L + 19L);
        if (chance > 0.22f) return;

        float progress = (float) (timeMs % windowMs) / windowMs;
        int span = Math.max(1, maxX - minX);
        float centerBase = (minX - 12.0f) + progress * (span + 24.0f);

        for (int py = minY; py < maxY; py++) {
            float center = centerBase + (py - minY) * 0.29f;
            for (int dx = -24; dx <= 24; dx++) {
                int px = (int) Math.floor(center + dx);
                if (px < minX || px >= maxX) continue;
                int adx = Math.abs(dx);
                int alpha = Math.max(1, 6 - (adx / 4));
                int color = (alpha << 24) | 0x00BFEAF2;
                context.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static float hash01(long n) {
        long x = n;
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        long masked = x & 0x7FFFFFFFL;
        return masked / (float) 0x7FFFFFFF;
    }
}
