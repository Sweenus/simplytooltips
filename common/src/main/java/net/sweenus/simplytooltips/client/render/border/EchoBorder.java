package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Alternating sculk glyphs. Accents: A glyph, B glyph detail. */
public class EchoBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFB59AFF, 0xFF7C67D9);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int runeA = p.a(), runeB = p.b();
        for (int px = x + 10, i = 0; px < x + w - 10; px += 16, i++) {
            if ((i & 1) == 0) {
                context.fill(px, y + 1, px + 1, y + 4, runeA);
                context.fill(px + 1, y + 1, px + 2, y + 2, runeB);
                context.fill(px + 1, y + 3, px + 2, y + 4, runeB);
                context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                context.fill(px + 1, y + h - 4, px + 2, y + h - 3, runeB);
                context.fill(px + 1, y + h - 2, px + 2, y + h - 1, runeB);
            } else {
                context.fill(px, y + 2, px + 3, y + 3, runeA);
                context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
            }
        }
    }
}
