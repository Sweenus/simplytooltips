package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class EarthMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spawnRange = Math.max(10, maxX - minX - 1);
        for (int i = 0; i < 16; i++) {
            int speedBand = (i * 13 + 5) % 6;
            double fallSpeed = 0.009 + speedBand * 0.0011 + i * 0.00015;
            int travel = Math.max(8, maxY - minY);
            double fallProgress = (timeMs * fallSpeed) + i * 11;
            int py = minY + (int) (fallProgress % travel);
            int lane = (i * 29 + i * i * 5) % spawnRange;
            double drift = Math.sin((timeMs * 0.0016) + i * 1.1) * 2.0;
            int sizeRoll = (i * 7 + 3) % 10;
            int size = sizeRoll < 6 ? 1 : 2;
            if (sizeRoll == 9) size = 3;
            int px = Math.max(minX, Math.min(maxX - size, minX + lane + (int) drift));
            int cA = ((i + ((int) fallProgress / 24)) & 1) == 0 ? 0x1CB08C66 : 0x168A6A4B;
            int cB = 0x126E583F;
            if (size == 1) {
                context.fill(px, py, px + 1, py + 1, cA);
            } else if (size == 2) {
                context.fill(px, py, px + 2, py + 2, cA);
                context.fill(px + 1, py, px + 2, py + 1, cB);
            } else {
                context.fill(px, py, px + 3, py + 2, cA);
                context.fill(px + 1, py + 1, px + 3, py + 2, cB);
            }
        }
    }
}
