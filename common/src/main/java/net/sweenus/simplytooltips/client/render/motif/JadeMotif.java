package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class JadeMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;

        int minX = x + 6;
        int maxX = x + w - 6;
        int minY = y + 6;
        int maxY = y + h - 6;
        int spanX = Math.max(14, maxX - minX - 2);
        int spanY = Math.max(14, maxY - minY - 2);

        // Slow drifting jade motes.
        for (int i = 0; i < 14; i++) {
            int laneX = minX + ((i * 37 + i * i * 11) % spanX);
            int laneY = minY + ((i * 29 + i * i * 7) % spanY);

            int px = laneX + (int) (Math.sin((timeMs * 0.0012) + i * 1.4) * 3.0);
            int py = laneY + (int) (Math.sin((timeMs * 0.0009) + i * 1.9) * 2.0);

            px = Math.max(minX + 1, Math.min(maxX - 2, px));
            py = Math.max(minY + 1, Math.min(maxY - 2, py));

            double pulse = (Math.sin((timeMs * 0.0033) + i * 1.6) + 1.0) * 0.5;
            int outerA = 0x05 + (int) (pulse * 0x06);
            int coreA = 0x10 + (int) (pulse * 0x10);

            int outer = (outerA << 24) | 0x004BA67B;
            int core = (coreA << 24) | 0x0085D6AF;
            context.fill(px - 1, py, px + 2, py + 1, outer);
            context.fill(px, py - 1, px + 1, py + 2, outer);
            context.fill(px, py, px + 1, py + 1, core);
        }

        // Gold glints that occasionally brighten.
        for (int i = 0; i < 6; i++) {
            int gx = minX + ((i * 53 + i * i * 13) % Math.max(16, spanX));
            int gy = minY + ((i * 41 + i * i * 9) % Math.max(16, spanY));

            gx += (int) (Math.sin((timeMs * 0.0011) + i * 2.0) * 2.0);
            gy += (int) (Math.sin((timeMs * 0.0015) + i * 1.3) * 1.0);
            gx = Math.max(minX + 1, Math.min(maxX - 2, gx));
            gy = Math.max(minY + 1, Math.min(maxY - 2, gy));

            double twinkle = (Math.sin((timeMs * 0.0045) + i * 2.8) + 1.0) * 0.5;
            int glintA = 0x06 + (int) (twinkle * 0x0C);
            int coreA = 0x0A + (int) (twinkle * 0x12);

            int glint = (glintA << 24) | 0x00E6BE5B;
            int core = (coreA << 24) | 0x00FFE59A;

            context.fill(gx - 1, gy, gx + 2, gy + 1, glint);
            context.fill(gx, gy - 1, gx + 1, gy + 2, glint);
            context.fill(gx, gy, gx + 1, gy + 1, core);
        }
    }
}
