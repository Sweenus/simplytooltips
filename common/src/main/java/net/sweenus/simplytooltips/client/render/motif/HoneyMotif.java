package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;


public class HoneyMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 36 || h < 36) return;

        int minX = x + 5;
        int maxX = x + w - 5;
        int minY = y + 5;
        int maxY = y + h - 5;

        int cellW = 8;
        int cellH = 6;
        int stepX = 9;
        int stepY = 6;

        for (int row = 0, py = minY; py <= maxY - cellH; row++, py += stepY) {
            int rowOffset = ((row & 1) == 0) ? 0 : 4;
            for (int col = 0, px = minX + rowOffset; px <= maxX - cellW; col++, px += stepX) {
                double pulse = Math.sin(timeMs * 0.0017 + row * 0.75 + col * 0.55);
                int edge = pulse > 0.4 ? 0x2ADCAA3A : 0x22B57E1F;
                int shade = pulse < -0.35 ? 0x1A744B16 : 0x164B320F;
                int fill = 0x0ED29B35;

                drawHexCell(context, px, py, edge, shade, fill);
            }
        }
    }

    private static void drawHexCell(DrawContext context, int x, int y, int edge, int shade, int fill) {
        context.fill(x + 2, y, x + 5, y + 1, edge);
        context.fill(x + 1, y + 1, x + 2, y + 2, edge);
        context.fill(x + 5, y + 1, x + 6, y + 2, edge);
        context.fill(x, y + 2, x + 1, y + 4, edge);
        context.fill(x + 6, y + 2, x + 7, y + 4, edge);
        context.fill(x + 1, y + 4, x + 2, y + 5, shade);
        context.fill(x + 5, y + 4, x + 6, y + 5, shade);
        context.fill(x + 2, y + 5, x + 5, y + 6, shade);

        context.fill(x + 2, y + 2, x + 5, y + 4, fill);
        context.fill(x + 3, y + 1, x + 4, y + 2, 0x12FFE18A);
    }
}
