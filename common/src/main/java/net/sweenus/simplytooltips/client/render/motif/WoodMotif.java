package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class WoodMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6;
        int maxX = x + w - 6;
        int minY = y + 6;
        int maxY = y + h - 6;

        for (int row = 0, py = minY; py < maxY - 1; row++, py += 8) {
            int sway = (int) (Math.sin(timeMs * 0.0011 + row * 0.9) * 2.0);
            int grain = (row & 1) == 0 ? 0x124B3323 : 0x0E6A4930;
            context.fill(minX + 1 + sway, py, maxX - 1 + sway, py + 1, grain);
        }

        for (int i = 0; i < 8; i++) {
            int px = minX + ((i * 37 + i * i * 5) % Math.max(10, maxX - minX - 2));
            int py = minY + (int) ((timeMs * (0.006 + i * 0.0004) + i * 15) % Math.max(10, maxY - minY - 2));
            context.fill(px, py, px + 2, py + 1, 0x129F6F44);
        }
    }
}
