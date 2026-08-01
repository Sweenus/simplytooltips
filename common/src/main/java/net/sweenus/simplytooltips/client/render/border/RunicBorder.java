package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Carved runes, alternating orientation. Accents: A rune, B rune shadow, C sigil. */
public class RunicBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFA793FF, 0xFF6D58C9, 0xFFE0D6FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int runeA = p.a(), runeB = p.b(), sigil = p.c();
        for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
            if ((i & 1) == 0) {
                context.fill(px, y + 1, px + 1, y + 4, runeA);
                context.fill(px + 1, y + 2, px + 3, y + 3, runeB);
                context.fill(px + 1, y + 1, px + 2, y + 2, sigil);
                context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                context.fill(px - 1, y + h - 3, px + 1, y + h - 2, runeB);
            } else {
                context.fill(px, y + 2, px + 3, y + 3, runeA);
                context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                context.fill(px + 1, y + 2, px + 2, y + 3, sigil);
                context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
            }
        }
    }
}
