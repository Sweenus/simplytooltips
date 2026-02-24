package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class BeeMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(10, maxX - minX - 2);
        int spanY = Math.max(10, maxY - minY - 2);
        for (int i = 0; i < 7; i++) {
            int baseX = minX + ((i * 47 + i * i * 7) % spanX);
            int baseY = minY + ((i * 29 + i * i * 5) % spanY);
            int px = baseX + (int) (Math.sin((timeMs * 0.0030) + i * 1.8) * 5.0);
            int py = baseY + (int) (Math.sin((timeMs * 0.0024) + i * 1.2) * 3.0);
            px = Math.max(minX, Math.min(maxX - 2, px));
            py = Math.max(minY, Math.min(maxY - 2, py));
            context.fill(px, py, px + 1, py + 1, 0x20F2C74A);
            context.fill(px + 1, py, px + 2, py + 1, 0x167A5A22);
        }
    }
}
