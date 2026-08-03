package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Bark grain. Accents: A bark, B bark shadow, C sap highlight. */
public class WoodBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFB1804F, 0xFF7A5332, 0xFFD7AD72);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int barkA = p.a(), barkB = p.b(), sap = p.c();
        for (int px = x + 9, i = 0; px < x + w - 10; px += 13, i++) {
            boolean flip = (i & 1) == 0;
            context.fill(px, y + 1, px + 3, y + 2, barkA);
            context.fill(px + 1, y + 2, px + 4, y + 3, barkB);
            context.fill(px + (flip ? 1 : 2), y + 1, px + (flip ? 2 : 3), y + 2, sap);
            context.fill(px, y + h - 3, px + 3, y + h - 2, barkA);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, barkB);
            context.fill(px + (flip ? 0 : 1), y + h - 3, px + (flip ? 1 : 2), y + h - 2, sap);
        }
    }
}
