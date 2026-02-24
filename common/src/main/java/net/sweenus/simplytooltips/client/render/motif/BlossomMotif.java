package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class BlossomMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spawnRange = Math.max(12, (maxX - minX) - 6);
        for (int i = 0; i < 7; i++) {
            double fallSpeed = 0.010 + i * 0.0012;
            int travel = Math.max(10, maxY - minY);
            double fallProgress = (timeMs * fallSpeed) + i * 19;
            int py = minY + (int) (fallProgress % travel);
            int lane = (i * 37 + i * i * 11) % spawnRange;
            double swayA = Math.sin((timeMs * 0.0009) + i * 2.1) * (w * 0.22);
            double swayB = Math.sin((timeMs * 0.0017) + i * 0.9) * 8.0;
            int baseX = minX + lane + (int) (swayA + swayB);
            int px = Math.max(minX, Math.min(maxX - 5, baseX));
            int coarseSpin = (int) (fallProgress / 26.0);
            int rotation = Math.floorMod(i * 3 + coarseSpin, 4);
            drawRotatedBlossomPetal(context, px, py, rotation);
        }
    }

    private static void drawRotatedBlossomPetal(DrawContext context, int x, int y, int rotation) {
        int petalA = 0x14F3B1D2, petalB = 0x10E38BB8, petalC = 0x0CF6C6DE, core = 0x16FFF4BE;
        switch (rotation) {
            case 1 -> {
                context.fill(x + 1, y, x + 3, y + 4, petalA);
                context.fill(x, y + 1, x + 2, y + 5, petalB);
                context.fill(x + 2, y, x + 4, y + 3, petalC);
                context.fill(x + 1, y + 2, x + 2, y + 3, core);
            }
            case 2 -> {
                context.fill(x + 1, y, x + 5, y + 2, petalA);
                context.fill(x, y + 1, x + 4, y + 3, petalB);
                context.fill(x + 2, y + 2, x + 5, y + 4, petalC);
                context.fill(x + 2, y + 1, x + 3, y + 2, core);
            }
            case 3 -> {
                context.fill(x + 2, y, x + 4, y + 4, petalA);
                context.fill(x + 1, y + 1, x + 3, y + 5, petalB);
                context.fill(x, y + 1, x + 3, y + 3, petalC);
                context.fill(x + 2, y + 2, x + 3, y + 3, core);
            }
            default -> {
                context.fill(x, y, x + 4, y + 2, petalA);
                context.fill(x + 1, y + 1, x + 5, y + 3, petalB);
                context.fill(x, y + 2, x + 3, y + 4, petalC);
                context.fill(x + 2, y + 1, x + 3, y + 2, core);
            }
        }
    }
}
