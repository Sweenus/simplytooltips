package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Brushed steel plates. Accents: A steel, B steel shadow, C glint. */
public class IronBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFC4CDD8, 0xFF8893A1, 0xFFEAF1FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int steelA = p.a(), steelB = p.b(), glint = p.c();
        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            context.fill(px, y + 1, px + 3, y + 2, steelA);
            context.fill(px + 1, y + 2, px + 4, y + 3, steelB);
            context.fill(px, y + h - 3, px + 3, y + h - 2, steelA);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, steelB);
            if ((i & 1) == 0) {
                context.fill(px + 2, y + 1, px + 3, y + 2, glint);
                context.fill(px, y + h - 2, px + 1, y + h - 1, glint);
            }
        }
    }
}
