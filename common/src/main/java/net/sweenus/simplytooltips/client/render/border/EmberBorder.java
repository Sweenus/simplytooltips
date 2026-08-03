package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Flame tongues and cinders. Accents: A flame, B flame shadow, C hot tip, D coal. */
public class EmberBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFFF8A4A, 0xFFE3522E, 0xFFFFC178, 0xFF5E1D16);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int emberA = p.a(), emberB = p.b(), hot = p.c(), coal = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            boolean offset = (i & 1) == 0;

            // Top flame tongues
            context.fill(px, y + 1, px + 2, y + 2, emberA);
            context.fill(px + 1, y + 2, px + 3, y + 3, emberB);
            if (offset) {
                context.fill(px + 1, y, px + 2, y + 1, hot);
            } else {
                context.fill(px, y, px + 1, y + 1, hot);
            }

            // Bottom mirrored cinders
            context.fill(px, y + h - 3, px + 2, y + h - 2, emberA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, emberB);
            if (offset) {
                context.fill(px, y + h - 1, px + 1, y + h, hot);
            } else {
                context.fill(px + 1, y + h - 1, px + 2, y + h, hot);
            }

            // Dark coal accent between motifs
            if (i % 3 == 1) {
                context.fill(px + 3, y + 1, px + 4, y + 2, coal);
                context.fill(px - 2, y + h - 2, px - 1, y + h - 1, coal);
            }
        }

        // Corner spark accents
        context.fill(x + 3, y + 3, x + 5, y + 4, hot);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, hot);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, hot);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, hot);
    }
}
