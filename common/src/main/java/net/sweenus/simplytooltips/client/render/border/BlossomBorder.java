package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Small four-petal flowers. Accents: A petal, B core. */
public class BlossomBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFF3B1D2, 0xFFFFF4BE);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int petal = p.a(), core = p.b();
        for (int px = x + 12; px < x + w - 12; px += 16) {
            context.fill(px, y + 1, px + 1, y + 4, petal);
            context.fill(px - 1, y + 2, px + 2, y + 3, petal);
            context.fill(px, y + 2, px + 1, y + 3, core);
            context.fill(px, y + h - 4, px + 1, y + h - 1, petal);
            context.fill(px - 1, y + h - 3, px + 2, y + h - 2, petal);
            context.fill(px, y + h - 3, px + 1, y + h - 2, core);
        }
    }
}
