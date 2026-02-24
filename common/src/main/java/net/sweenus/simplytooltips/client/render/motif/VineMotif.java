package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class VineMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spawnRange = Math.max(12, (maxX - minX) - 6);
        for (int i = 0; i < 6; i++) {
            double fallSpeed = 0.009 + i * 0.0011;
            int travel = Math.max(10, maxY - minY);
            double fallProgress = (timeMs * fallSpeed) + i * 27;
            int py = minY + (int) (fallProgress % travel);
            int lane = (i * 43 + i * i * 13) % spawnRange;
            double swayA = Math.sin((timeMs * 0.0011) + i * 2.0) * (w * 0.20);
            double swayB = Math.sin((timeMs * 0.0024) + i * 1.3) * 7.0;
            int baseX = minX + lane + (int) (swayA + swayB);
            int px = Math.max(minX, Math.min(maxX - 5, baseX));
            int rotation = Math.floorMod(i * 2 + (int) (fallProgress / 30.0), 4);
            drawRotatedVineLeaf(context, px, py, rotation);
        }
    }

    private static void drawRotatedVineLeaf(DrawContext context, int x, int y, int rotation) {
        int leafA = 0x1A84C47E, leafB = 0x1470B16A, vein = 0x1A3F7E45;
        switch (rotation) {
            case 1 -> {
                context.fill(x + 1, y, x + 3, y + 4, leafA);
                context.fill(x, y + 1, x + 2, y + 5, leafB);
                context.fill(x + 1, y + 1, x + 2, y + 4, vein);
            }
            case 2 -> {
                context.fill(x + 1, y, x + 5, y + 2, leafA);
                context.fill(x, y + 1, x + 4, y + 3, leafB);
                context.fill(x + 1, y + 1, x + 4, y + 2, vein);
            }
            case 3 -> {
                context.fill(x + 2, y, x + 4, y + 4, leafA);
                context.fill(x + 1, y + 1, x + 3, y + 5, leafB);
                context.fill(x + 2, y + 1, x + 3, y + 4, vein);
            }
            default -> {
                context.fill(x, y, x + 4, y + 2, leafA);
                context.fill(x + 1, y + 1, x + 5, y + 3, leafB);
                context.fill(x + 1, y + 1, x + 4, y + 2, vein);
            }
        }
    }
}
