package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class StoneMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6;
        int maxX = x + w - 6;
        int minY = y + 6;
        int maxY = y + h - 6;
        int spanX = Math.max(12, maxX - minX - 2);
        int spanY = Math.max(12, maxY - minY - 2);

        for (int i = 0; i < 20; i++) {
            int px = minX + ((i * 29 + i * i * 7) % spanX);
            int py = minY + ((i * 23 + i * i * 11) % spanY);
            px += (int) (Math.sin(timeMs * 0.0007 + i) * 1.0);
            py += (int) (Math.sin(timeMs * 0.0006 + i * 1.4) * 1.0);
            int chip = (i & 1) == 0 ? 0x126A717F : 0x0F8790A0;
            context.fill(px, py, px + 1, py + 1, chip);
            if ((i % 5) == 0) context.fill(px, py, px + 2, py + 1, 0x0AA5ADBD);
        }
    }
}
