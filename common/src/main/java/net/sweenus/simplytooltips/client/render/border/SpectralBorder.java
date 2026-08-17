package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Tapering rift slivers with corner fractures. Accents: A rift, B shadow, C core, D ink, E glow. */
public class SpectralBorder implements BorderPattern {

    private static final int SPACING = 10;

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF6FB8EF, 0xFF11314F, 0xFFEAF6FF, 0xFF0A1B2C, 0xFF2E7BB8);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        for (int px = x + 8, i = 0; px < x + w - 8; px += SPACING, i++) {
            boolean tall = (i & 1) == 0;
            drawShard(context, px, y + 1, 1, tall, p);
            drawShard(context, px, y + h - 1, -1, tall, p);
        }

        drawCrack(context, x + 3, y + 3, 1, 1, p);
        drawCrack(context, x + w - 4, y + h - 4, -1, -1, p);
    }

    private static void drawShard(DrawContext context, int px, int edgeY, int step,
                                  boolean tall, BorderPalette p) {
        int base = tall ? 3 : 2;
        fillRow(context, px - base, px + base + 1, edgeY, step, 0, p.b());
        fillRow(context, px - base + 1, px + base, edgeY, step, 0, p.a());

        if (tall) fillRow(context, px - 1, px + 2, edgeY, step, 1, p.a());
        fillRow(context, px, px + 1, edgeY, step, 1, p.c());
        fillRow(context, px, px + 1, edgeY, step, 2, tall ? p.c() : p.e());

        if (!tall) return;
        fillRow(context, px, px + 1, edgeY, step, 3, p.e());
        fillRow(context, px, px + 1, edgeY, step, 4, p.e());
        fillRow(context, px, px + 1, edgeY, step, 5, p.d());
    }

    private static void drawCrack(DrawContext context, int x, int y, int stepX, int stepY, BorderPalette p) {
        for (int n = 0; n < 3; n++) {
            int cx = x + stepX * n;
            int cy = y + stepY * n;
            context.fill(cx, cy, cx + 1, cy + 1, n == 0 ? p.c() : p.e());
            context.fill(cx + stepX, cy, cx + stepX + 1, cy + 1, p.b());
        }
    }

    private static void fillRow(DrawContext context, int x0, int x1, int edgeY, int step, int n, int color) {
        int top = step > 0 ? edgeY + n : edgeY - n - 1;
        context.fill(x0, top, x1, top + 1, color);
    }
}
