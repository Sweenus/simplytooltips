package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Uneven wax drips with brass wick ticks. Accents: A tallow, B shadow, C core, D ink, E brass. */
public class CandleBorder implements BorderPattern {

    private static final int SPACING = 10;

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFD8C48A, 0xFF4A3418, 0xFFFFF6DE, 0xFF1E120C, 0xFFC49D1C);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        for (int px = x + 8, i = 0; px < x + w - 8; px += SPACING, i++) {
            drawDrip(context, px, y + 1, 1, dripLength(i), p);
            drawDrip(context, px, y + h - 1, -1, dripLength(i + 7), p);
        }

        drawWickTick(context, x + 3, y + 3, 1, 1, p);
        drawWickTick(context, x + w - 4, y + h - 4, -1, -1, p);
    }

    private static void drawDrip(DrawContext context, int px, int edgeY, int step,
                                 int length, BorderPalette p) {
        fillRow(context, px - 2, px + 3, edgeY, step, 0, p.b());
        fillRow(context, px - 1, px + 2, edgeY, step, 0, p.a());
        fillRow(context, px - 1, px + 2, edgeY, step, 1, p.e());

        for (int n = 2; n < length; n++) {
            fillRow(context, px, px + 1, edgeY, step, n, p.a());
        }

        fillRow(context, px - 1, px + 2, edgeY, step, length, p.b());
        fillRow(context, px, px + 1, edgeY, step, length, p.a());
        fillRow(context, px, px + 1, edgeY, step, length + 1, p.c());
    }

    private static void drawWickTick(DrawContext context, int x, int y, int stepX, int stepY, BorderPalette p) {
        for (int n = 0; n < 3; n++) {
            int cx = x + stepX * n;
            int cy = y + stepY * n;
            context.fill(cx, cy, cx + 1, cy + 1, n == 2 ? p.e() : p.a());
        }
        context.fill(x, y + stepY * 3, x + 1, y + stepY * 3 + 1, p.c());
    }

    private static int dripLength(int index) {
        int h = index * 374761393 + 668265263;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        return 2 + Math.floorMod(h, 5);
    }

    private static void fillRow(DrawContext context, int x0, int x1, int edgeY, int step, int n, int color) {
        int top = step > 0 ? edgeY + n : edgeY - n - 1;
        context.fill(x0, top, x1, top + 1, color);
    }
}
