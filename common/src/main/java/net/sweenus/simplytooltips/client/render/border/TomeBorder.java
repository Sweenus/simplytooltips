package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Tooled leather with brass corner protectors. Accents: A leather, B stitch, C brass, D ink, E gilt. */
public class TomeBorder implements BorderPattern {

    private static final int SPACING = 6;
    private static final int CORNER  = 9;

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF8A5F35, 0xFF3E2614, 0xFFC08A3A, 0xFF2A1A0C, 0xFFE8C46A);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int leather = p.a(), stitch = p.b();

        context.fill(x + 1, y + 1, x + w - 1, y + 3, leather);
        context.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, leather);
        context.fill(x + 1, y + 1, x + 3, y + h - 1, leather);
        context.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, leather);

        context.fill(x + 3, y + 3, x + w - 3, y + 4, stitch);
        context.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, stitch);
        context.fill(x + 3, y + 3, x + 4, y + h - 3, stitch);
        context.fill(x + w - 4, y + 3, x + w - 3, y + h - 3, stitch);

        for (int px = x + CORNER + 4; px < x + w - CORNER - 4; px += SPACING) {
            context.fill(px, y + 2, px + 2, y + 3, stitch);
            context.fill(px, y + h - 3, px + 2, y + h - 2, stitch);
        }
        for (int py = y + CORNER + 4; py < y + h - CORNER - 4; py += SPACING) {
            context.fill(x + 2, py, x + 3, py + 2, stitch);
            context.fill(x + w - 3, py, x + w - 2, py + 2, stitch);
        }

        drawCorner(context, x + 1, y + 1, 1, 1, p);
        drawCorner(context, x + w - 2, y + 1, -1, 1, p);
        drawCorner(context, x + 1, y + h - 2, 1, -1, p);
        drawCorner(context, x + w - 2, y + h - 2, -1, -1, p);
    }

    private static void drawCorner(DrawContext context, int cx, int cy, int sx, int sy, BorderPalette p) {
        for (int n = 0; n < CORNER; n++) {
            int thick = n < CORNER - 3 ? 3 : (n < CORNER - 1 ? 2 : 1);

            for (int t = 0; t < thick; t++) {
                int color = t == 1 ? p.e() : (t == 2 ? p.d() : p.c());
                int hx = cx + sx * n, hy = cy + sy * t;
                int vx = cx + sx * t, vy = cy + sy * n;
                context.fill(hx, hy, hx + 1, hy + 1, color);
                context.fill(vx, vy, vx + 1, vy + 1, color);
            }
        }
    }
}
