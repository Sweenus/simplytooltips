package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class EnchantedMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;

        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(14, maxX - minX - 3);
        int spanY = Math.max(14, maxY - minY - 3);

        // Starfield layer: slow twinkling stars
        for (int i = 0; i < 11; i++) {
            int baseX = minX + ((i * 41 + i * i * 9) % spanX);
            int baseY = minY + ((i * 23 + i * i * 7) % spanY);

            int px = baseX + (int) (Math.sin((timeMs * 0.0012) + i * 1.4) * 3.0);
            int py = baseY + (int) (Math.sin((timeMs * 0.0010) + i * 1.9) * 2.0);

            px = Math.max(minX + 1, Math.min(maxX - 2, px));
            py = Math.max(minY + 1, Math.min(maxY - 2, py));

            double twinkle = (Math.sin((timeMs * 0.0036) + i * 2.2) + 1.0) * 0.5;
            int hazeA = 0x03 + (int) (twinkle * 0x06);
            int starA = 0x0B + (int) (twinkle * 0x0C);
            int coreA = 0x12 + (int) (twinkle * 0x14);

            int haze = (hazeA << 24) | 0x006246C8;
            int star = (starA << 24) | 0x00B894FF;
            int core = (coreA << 24) | 0x00F2EAFF;

            context.fill(px - 1, py, px + 2, py + 1, haze);
            context.fill(px, py - 1, px + 1, py + 2, haze);
            context.fill(px, py, px + 1, py + 1, star);
            if ((i % 3) == 0) {
                context.fill(px, py, px + 1, py + 1, core);
            }
        }

        // Nebula wisps: broader drifting purple clouds
        for (int i = 0; i < 4; i++) {
            int laneX = minX + ((i * 57 + i * i * 17) % Math.max(18, spanX));
            int laneY = minY + ((i * 31 + i * i * 11) % Math.max(16, spanY));

            int nx = laneX + (int) (Math.sin((timeMs * 0.0008) + i * 1.5) * (w * 0.10));
            int ny = laneY + (int) (Math.sin((timeMs * 0.0011) + i * 2.1) * 4.0);
            nx = Math.max(minX + 2, Math.min(maxX - 4, nx));
            ny = Math.max(minY + 2, Math.min(maxY - 3, ny));

            double phase = (Math.sin((timeMs * 0.0015) + i * 1.8) + 1.0) * 0.5;
            int nebulaA = 0x03 + (int) (phase * 0x06);
            int nebula = (nebulaA << 24) | 0x007A4DCC;
            int nebulaBright = ((nebulaA + 2) << 24) | 0x00A06CFF;

            context.fill(nx - 2, ny, nx + 3, ny + 1, nebula);
            context.fill(nx - 1, ny - 1, nx + 2, ny, nebula);
            context.fill(nx - 1, ny + 1, nx + 2, ny + 2, nebula);
            context.fill(nx, ny, nx + 1, ny + 1, nebulaBright);
        }

        // Arcane comets: diagonal streaks for cosmic motion
        for (int i = 0; i < 3; i++) {
            int travel = Math.max(18, (maxY - minY) + 10);
            double prog = (timeMs * (0.0052 + i * 0.0008)) + i * 31;
            int py = minY + (int) (prog % travel);
            int px = minX + Math.floorMod((int) (prog * 1.35) + i * 43, Math.max(16, spanX));
            px = Math.max(minX + 2, Math.min(maxX - 3, px));
            py = Math.max(minY + 2, Math.min(maxY - 3, py));

            int tail = 0x0A9A76F0;
            int head = 0x16ECDDFF;
            context.fill(px - 2, py - 1, px - 1, py, tail);
            context.fill(px - 1, py, px, py + 1, tail);
            context.fill(px, py, px + 1, py + 1, head);
            context.fill(px + 1, py + 1, px + 2, py + 2, 0x0EC7AEFF);
        }
    }
}