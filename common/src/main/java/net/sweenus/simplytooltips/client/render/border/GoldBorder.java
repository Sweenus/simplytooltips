package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Gilded plates with occasional shine. Accents: A gold, B gold shadow, C shine. */
public class GoldBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFFFCB62, 0xFFCC9328, 0xFFFFE8A4);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int goldA = p.a(), goldB = p.b(), shine = p.c();
        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            context.fill(px, y + 1, px + 3, y + 2, goldA);
            context.fill(px + 1, y + 2, px + 4, y + 3, goldB);
            context.fill(px, y + h - 3, px + 3, y + h - 2, goldA);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, goldB);
            if ((i % 3) == 1) {
                context.fill(px + 1, y, px + 2, y + 1, shine);
                context.fill(px + 2, y + h - 1, px + 3, y + h, shine);
            }
        }
    }
}
