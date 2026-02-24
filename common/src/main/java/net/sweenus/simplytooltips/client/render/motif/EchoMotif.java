package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class EchoMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(10, maxX - minX - 3);
        int spanY = Math.max(10, maxY - minY - 3);
        for (int i = 0; i < 7; i++) {
            int laneX = minX + ((i * 37 + i * i * 9) % spanX);
            int laneY = minY + ((i * 23 + i * i * 11) % spanY);
            int px = laneX + (int) (Math.sin((timeMs * 0.0019) + i * 1.5) * 7.0);
            int py = laneY + (int) (Math.sin((timeMs * 0.0022) + i * 1.1) * 4.0);
            px = Math.max(minX, Math.min(maxX - 3, px));
            py = Math.max(minY, Math.min(maxY - 3, py));
            int pulse = (((int) (timeMs / 240L) + i) & 1);
            int runeA = pulse == 0 ? 0x18B59AFF : 0x12B59AFF;
            int runeB = pulse == 0 ? 0x167C67D9 : 0x107C67D9;
            context.fill(px, py + 1, px + 3, py + 2, runeA);
            context.fill(px + 1, py, px + 2, py + 3, runeB);
        }
    }
}
