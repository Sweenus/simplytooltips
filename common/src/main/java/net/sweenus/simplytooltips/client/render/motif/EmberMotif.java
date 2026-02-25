package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class EmberMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;

        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(12, maxX - minX - 6);
        int travel = Math.max(12, maxY - minY);
        final long cycleMs = 2600L;
        final int fadeMs = 750;

        for (int i = 0; i < 9; i++) {
            long offset = i * 173L;
            long localTime = timeMs + offset;
            long cycleIndex = localTime / cycleMs;
            int seed = (int) (cycleIndex * 1103515245L + i * 12345L + 0x4D2B79F3);
            int delayMs = 220 + Math.floorMod(seed ^ (seed >>> 16), 1150);
            long phase = Math.floorMod(localTime, cycleMs);
            if (phase >= delayMs + fadeMs) continue;

            double fadeByLifetime = phase <= delayMs
                    ? 1.0
                    : Math.max(0.0, 1.0 - ((phase - delayMs) / (double) fadeMs));

            double riseSpeed = 0.008 + i * 0.00105;
            double riseProgress = (localTime * riseSpeed) + i * 31;
            int py = maxY - (int) (riseProgress % travel);

            int lane = (i * 37 + i * i * 11) % spanX;
            double swayA = Math.sin((localTime * 0.0012) + i * 1.7) * (w * 0.10);
            double swayB = Math.sin((localTime * 0.0027) + i * 0.9) * 2.8;
            int baseX = minX + lane + (int) (swayA + swayB);
            int px = Math.max(minX + 1, Math.min(maxX - 3, baseX));

            double flicker = (Math.sin((localTime * 0.0054) + i * 2.3) + 1.0) * 0.5;
            int glowAlpha = (int) ((0x06 + (flicker * 0x08)) * fadeByLifetime);
            int emberAlpha = (int) ((0x13 + (flicker * 0x0F)) * fadeByLifetime);
            int hotAlpha = (int) ((0x20 + (flicker * 0x12)) * fadeByLifetime);
            if (hotAlpha <= 0) continue;

            int glow = (glowAlpha << 24) | 0x00FF7A2E;
            int ember = (emberAlpha << 24) | 0x00FF9A3F;
            int hot = (hotAlpha << 24) | 0x00FFD87A;
            int trail = ((Math.max(0x07, emberAlpha - 0x06)) << 24) | 0x00E05A20;

            // Soft glow around particle
            context.fill(px - 1, py, px + 2, py + 1, glow);
            context.fill(px, py - 1, px + 1, py + 2, glow);

            // Ember body and hot core
            if ((i & 1) == 0) {
                context.fill(px, py, px + 2, py + 1, ember);
                context.fill(px, py, px + 1, py + 1, hot);
            } else {
                context.fill(px, py, px + 1, py + 1, ember);
                context.fill(px, py, px + 1, py + 1, hot);
            }

            // Small upward trail spark
            if ((i % 3) != 0 && py > minY + 1) {
                context.fill(px, py - 1, px + 1, py, trail);
            }
        }
    }
}