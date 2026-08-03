package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Drifting soul wisps. Accents: A wisp, B wisp shadow, C core, D ash. */
public class SoulBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF8EFFF5, 0xFF52D7CA, 0xFFD8FFFB, 0xFF1E3436);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int wispA = p.a(), wispB = p.b(), core = p.c(), ash = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 13, i++) {
            boolean flip = (i & 1) == 0;

            context.fill(px, y + 1, px + 2, y + 2, wispA);
            context.fill(px + 1, y + 2, px + 3, y + 3, wispB);
            context.fill(px + 1, y + 1, px + 2, y + 2, core);
            if (flip) context.fill(px - 1, y + 2, px, y + 3, ash);
            else      context.fill(px + 3, y + 2, px + 4, y + 3, ash);

            context.fill(px, y + h - 3, px + 2, y + h - 2, wispA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, wispB);
            context.fill(px, y + h - 2, px + 1, y + h - 1, core);
            if (flip) context.fill(px + 2, y + h - 2, px + 3, y + h - 1, ash);
            else      context.fill(px - 2, y + h - 2, px - 1, y + h - 1, ash);
        }

        context.fill(x + 3, y + 3, x + 5, y + 4, core);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, core);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, core);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, core);
    }
}
