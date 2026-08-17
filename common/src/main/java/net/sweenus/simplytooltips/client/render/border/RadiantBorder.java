package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Crown of sun rays with corner halo ticks. Accents: A gold, B shadow, C core, D ink, E amber. */
public class RadiantBorder implements BorderPattern {

    private static final int SPACING = 10;

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFFFC233, 0xFF4A2E0A, 0xFFFFFFF1, 0xFF1A1030, 0xFFFFAD27);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        for (int px = x + 8, i = 0; px < x + w - 8; px += SPACING, i++) {
            boolean tall = (i & 1) == 0;
            drawRay(context, px, y + 1, 1, tall, p);
            drawRay(context, px, y + h - 1, -1, tall, p);
        }

        drawHaloTick(context, x + 3, y + 3, 1, 1, p);
        drawHaloTick(context, x + w - 4, y + h - 4, -1, -1, p);
    }

    private static void drawRay(DrawContext context, int px, int edgeY, int step,
                                boolean tall, BorderPalette p) {
        int base = tall ? 2 : 1;
        fillRow(context, px - base, px + base + 1, edgeY, step, 0, p.b());
        fillRow(context, px - base + 1, px + base, edgeY, step, 0, p.e());

        fillRow(context, px - 1, px + 2, edgeY, step, 1, tall ? p.a() : p.b());
        fillRow(context, px, px + 1, edgeY, step, 2, p.a());

        if (!tall) {
            fillRow(context, px, px + 1, edgeY, step, 3, p.e());
            return;
        }

        fillRow(context, px, px + 1, edgeY, step, 3, p.a());
        fillRow(context, px, px + 1, edgeY, step, 4, p.e());
        fillRow(context, px, px + 1, edgeY, step, 5, p.c());
    }

    private static void drawHaloTick(DrawContext context, int x, int y, int stepX, int stepY, BorderPalette p) {
        for (int n = 0; n < 3; n++) {
            int cx = x + stepX * n;
            int cy = y + stepY * (2 - n);
            context.fill(cx, cy, cx + 1, cy + 1, n == 1 ? p.c() : p.a());
            context.fill(cx, cy + stepY, cx + 1, cy + stepY + 1, p.d());
        }
    }

    private static void fillRow(DrawContext context, int x0, int x1, int edgeY, int step, int n, int color) {
        int top = step > 0 ? edgeY + n : edgeY - n - 1;
        context.fill(x0, top, x1, top + 1, color);
    }
}
