package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class RunicMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6;
        int maxX = x + w - 6;
        int minY = y + 6;
        int maxY = y + h - 6;
        int spanX = Math.max(14, maxX - minX - 3);
        int spanY = Math.max(14, maxY - minY - 3);

        for (int i = 0; i < 8; i++) {
            int px = minX + ((i * 59 + i * i * 7) % spanX);
            int py = minY + ((i * 37 + i * i * 13) % spanY);
            px += (int) (Math.sin(timeMs * 0.0013 + i * 1.2) * 2.0);
            py += (int) (Math.sin(timeMs * 0.001 + i * 1.7) * 2.0);

            double pulse = (Math.sin(timeMs * 0.004 + i * 2.7) + 1.0) * 0.5;
            int a = 0x07 + (int) (pulse * 0x0F);
            int rune = (a << 24) | 0x00A88DFF;
            int core = ((a + 5) << 24) | 0x00E4D9FF;

            if ((i & 1) == 0) {
                context.fill(px, py - 1, px + 1, py + 2, rune);
                context.fill(px - 1, py, px + 2, py + 1, rune);
            } else {
                context.fill(px - 1, py - 1, px, py + 2, rune);
                context.fill(px + 1, py - 1, px + 2, py + 2, rune);
                context.fill(px, py, px + 1, py + 1, rune);
            }
            context.fill(px, py, px + 1, py + 1, core);
        }
    }
}
