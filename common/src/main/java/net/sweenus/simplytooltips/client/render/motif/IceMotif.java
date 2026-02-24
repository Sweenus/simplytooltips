package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class IceMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spawnRange = Math.max(12, (maxX - minX) - 6);
        for (int i = 0; i < 8; i++) {
            double fallSpeed = 0.010 + i * 0.0010;
            int travel = Math.max(10, maxY - minY);
            double fallProgress = (timeMs * fallSpeed) + i * 23;
            int py = minY + (int) (fallProgress % travel);
            int lane = (i * 41 + i * i * 7) % spawnRange;
            double swayA = Math.sin((timeMs * 0.0010) + i * 1.6) * (w * 0.08);
            double swayB = Math.sin((timeMs * 0.0021) + i * 0.8) * 2.0;
            int baseX = minX + lane + (int) (swayA + swayB);
            int px = Math.max(minX, Math.min(maxX - 4, baseX));
            int rotation = Math.floorMod(i + (int) (fallProgress / 36.0), 4);
            int sizeVariant = ((i * 17) + 5) % 3;
            drawRotatedSnowflake(context, px, py, rotation, sizeVariant);
        }
    }

    private static void drawRotatedSnowflake(DrawContext context, int x, int y, int rotation, int sizeVariant) {
        int outer = 0x12D8F2FF, inner = 0x18E7F8FF, core = 0x20FFFFFF;
        if ((sizeVariant & 1) == 0) {
            context.fill(x + 1, y, x + 2, y + 1, outer);
            context.fill(x, y + 1, x + 3, y + 2, inner);
            context.fill(x + 1, y + 2, x + 2, y + 3, outer);
            context.fill(x + 1, y + 1, x + 2, y + 2, (rotation & 1) == 0 ? core : 0x18FFFFFF);
        } else {
            context.fill(x + 1, y, x + 3, y + 1, outer);
            context.fill(x, y + 1, x + 4, y + 3, inner);
            context.fill(x + 1, y + 3, x + 3, y + 4, outer);
            context.fill(x + 1, y + 1, x + 3, y + 3, (rotation & 1) == 0 ? core : 0x18FFFFFF);
        }
    }
}
