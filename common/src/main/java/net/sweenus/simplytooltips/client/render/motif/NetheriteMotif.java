package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class NetheriteMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6;
        int maxX = x + w - 6;
        int minY = y + 6;
        int maxY = y + h - 6;
        int travel = Math.max(12, maxY - minY - 2);

        for (int i = 0; i < 14; i++) {
            int px = minX + ((i * 33 + i * i * 9) % Math.max(10, maxX - minX - 2));
            int py = minY + (int) ((timeMs * (0.005 + i * 0.00035) + i * 19) % travel);
            int ash = (i & 1) == 0 ? 0x0D4F4762 : 0x0A6A5E82;
            context.fill(px, py, px + 1, py + 1, ash);
            if ((i % 5) == 0) context.fill(px, py, px + 1, py + 2, 0x0FB08DDC);
        }
    }
}
