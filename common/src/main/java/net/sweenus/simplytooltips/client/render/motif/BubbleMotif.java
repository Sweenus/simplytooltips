package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class BubbleMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spawnRange = Math.max(12, (maxX - minX) - 5);
        for (int i = 0; i < 8; i++) {
            int speedSeed = ((i * 17) + 3) % 5;
            double speedOffset = (speedSeed - 2) * 0.00008;
            double riseSpeed = (0.008 + i * 0.0009) + speedOffset;
            int travel = Math.max(10, maxY - minY);
            double riseProgress = (timeMs * riseSpeed) + i * 21;
            int py = maxY - (int) (riseProgress % travel);
            int lane = (i * 31 + i * i * 9) % spawnRange;
            double wobble = Math.sin((timeMs * 0.0015) + i * 1.4) * 6.0;
            int px = Math.max(minX, Math.min(maxX - 4, minX + lane + (int) wobble));
            context.fill(px, py, px + 2, py + 2, 0x148EE7F8);
            context.fill(px + 1, py + 1, px + 4, py + 4, 0x0EC8F7FF);
            context.fill(px + 1, py, px + 2, py + 1, 0x1AF2FDFF);
        }
    }
}
