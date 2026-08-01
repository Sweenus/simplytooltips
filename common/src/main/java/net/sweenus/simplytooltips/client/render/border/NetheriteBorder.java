package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Dark alloy plates with a faint glow. Accents: A alloy, B alloy shadow, C glow. */
public class NetheriteBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF6A6079, 0xFF3C374A, 0xFFB08EE0);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int alloyA = p.a(), alloyB = p.b(), glow = p.c();
        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            context.fill(px, y + 1, px + 3, y + 2, alloyA);
            context.fill(px + 1, y + 2, px + 4, y + 3, alloyB);
            context.fill(px, y + h - 3, px + 3, y + h - 2, alloyA);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, alloyB);
            if ((i % 4) == 0) {
                context.fill(px + 2, y + 1, px + 3, y + 2, glow);
                context.fill(px, y + h - 2, px + 1, y + h - 1, glow);
            }
        }
    }
}
