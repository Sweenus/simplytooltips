package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Arcane glyphs with corner sparks. Accents: A rune, B rune shadow, C sigil, D spark. */
public class EnchantedBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFD4B7FF, 0xFF9D73E8, 0xFF6D4AC7, 0xFFE8DBFF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int runeA = p.a(), runeB = p.b(), sigil = p.c(), spark = p.d();

        for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
            if ((i & 1) == 0) {
                context.fill(px, y + 1, px + 1, y + 4, runeA);
                context.fill(px + 1, y + 2, px + 3, y + 3, runeB);
                context.fill(px + 1, y + 1, px + 2, y + 2, spark);

                context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                context.fill(px - 1, y + h - 3, px + 1, y + h - 2, runeB);
                context.fill(px, y + h - 2, px + 1, y + h - 1, spark);
            } else {
                context.fill(px, y + 2, px + 3, y + 3, runeA);
                context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                context.fill(px + 1, y + 2, px + 2, y + 3, sigil);

                context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
                context.fill(px + 1, y + h - 3, px + 2, y + h - 2, sigil);
            }
        }

        context.fill(x + 3, y + 3, x + 5, y + 5, spark);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 5, spark);
        context.fill(x + 3, y + h - 5, x + 5, y + h - 3, spark);
        context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, spark);
    }
}
