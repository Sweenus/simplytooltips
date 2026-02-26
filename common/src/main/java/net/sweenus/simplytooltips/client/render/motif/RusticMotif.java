package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.client.render.TooltipPainter;

public class RusticMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w <= 0 || h <= 0) return;

        int shift = (int) ((timeMs / 22L) % Math.max(1, w));
        int left = 0x12F0DFC1;
        int right = 0x18CDB28C;

        for (int col = 0; col < w; col++) {
            int u = (col + shift) % w;
            float phase = (float) (u / (double) Math.max(1, w));
            float t = 0.5f + 0.5f * (float) Math.sin(phase * Math.PI * 2.0);
            int color = TooltipPainter.lerpColor(left, right, t);
            context.fill(x + col, y, x + col + 1, y + h, color);
        }
    }
}
