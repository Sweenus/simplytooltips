package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Leaning crystal facets with cut-gem corner ticks. Accents: A lilac, B shadow, C core, D ink, E spark. */
public class AmethystBorder implements BorderPattern {

    private static final int SPACING = 9;

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFC4ADFF, 0xFF2A1160, 0xFFF4ECFF, 0xFF120836, 0xFFE070FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        for (int px = x + 8, i = 0; px < x + w - 8; px += SPACING, i++) {
            drawFacet(context, px, y + 1, 1, spikeLength(i), (i & 1) == 0, p);
            drawFacet(context, px, y + h - 1, -1, spikeLength(i + 5), (i & 1) != 0, p);
        }

        drawGemTick(context, x + 4, y + 4, p);
        drawGemTick(context, x + w - 5, y + h - 5, p);
    }

    private static void drawFacet(DrawContext context, int px, int edgeY, int step,
                                  int length, boolean leanRight, BorderPalette p) {
        int dir = leanRight ? 1 : -1;
        int lit = leanRight ? -1 : 1;

        for (int n = 0; n < length; n++) {
            int cx = px + (n / 2) * dir;
            int half = n == 0 ? 1 : 0;

            fillRow(context, cx - half - 1, cx + half + 2, edgeY, step, n, p.b());
            fillRow(context, cx - half, cx + half + 1, edgeY, step, n, p.a());

            if (n >= length - 2) {
                int lx = cx + lit * (half + 1);
                fillRow(context, lx, lx + 1, edgeY, step, n, p.c());
            }
        }

        int tipX = px + ((length - 1) / 2) * dir;
        fillRow(context, tipX, tipX + 1, edgeY, step, length, p.e());
    }

    private static void drawGemTick(DrawContext context, int cx, int cy, BorderPalette p) {
        context.fill(cx - 1, cy - 1, cx + 2, cy, p.b());
        context.fill(cx - 2, cy, cx + 3, cy + 1, p.b());
        context.fill(cx - 1, cy + 1, cx + 2, cy + 2, p.b());

        context.fill(cx, cy - 1, cx + 1, cy, p.a());
        context.fill(cx - 1, cy, cx + 2, cy + 1, p.a());
        context.fill(cx, cy + 1, cx + 1, cy + 2, p.a());

        context.fill(cx, cy, cx + 1, cy + 1, p.c());
    }

    private static int spikeLength(int index) {
        int h = index * 668265263 + 374761393;
        h ^= h >>> 15;
        h *= 0x85EBCA77;
        h ^= h >>> 13;
        return 3 + Math.floorMod(h, 4);
    }

    private static void fillRow(DrawContext context, int x0, int x1, int edgeY, int step, int n, int color) {
        int top = step > 0 ? edgeY + n : edgeY - n - 1;
        context.fill(x0, top, x1, top + 1, color);
    }
}
