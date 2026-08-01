package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Cut gem facets. Accents: A facet, B facet shadow, C flash. */
public class DiamondBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF75E6F3, 0xFF35A6BA, 0xFFCFFFFF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int diaA = p.a(), diaB = p.b(), flash = p.c();
        for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
            context.fill(px, y + 1, px + 2, y + 2, diaA);
            context.fill(px + 1, y + 2, px + 3, y + 3, diaB);
            context.fill(px + 1, y + 1, px + 2, y + 2, flash);
            context.fill(px, y + h - 3, px + 2, y + h - 2, diaA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, diaB);
            if ((i & 1) == 0) {
                context.fill(px + 2, y + 2, px + 3, y + 3, flash);
                context.fill(px - 1, y + h - 3, px, y + h - 2, flash);
            }
        }
    }
}
