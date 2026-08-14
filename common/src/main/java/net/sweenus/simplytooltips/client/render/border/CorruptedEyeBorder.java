package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

public class CorruptedEyeBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF713994, 0xFF25102F, 0xFFA76FD0, 0xFF09040D, 0xFF4B225F);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int corruption = p.a();
        int shadow = p.b();
        int highlight = p.c();
        int ink = p.d();
        int bruise = p.e();

        for (int px = x + 9, i = 0; px < x + w - 9; px += 14, i++) {
            boolean flip = (i & 1) == 0;

            context.fill(px - 4, y + 1, px + 4, y + 2, shadow);
            context.fill(px - 2, y + 1, px + 2, y + 2, corruption);
            context.fill(px, y + 2, px + 1, y + 5, bruise);
            context.fill(px, y + 2, px + 1, y + 3, highlight);
            if (flip) {
                context.fill(px - 3, y + 2, px, y + 3, corruption);
                context.fill(px - 3, y + 3, px - 2, y + 4, ink);
            } else {
                context.fill(px + 1, y + 2, px + 4, y + 3, corruption);
                context.fill(px + 3, y + 3, px + 4, y + 4, ink);
            }

            context.fill(px - 4, y + h - 2, px + 4, y + h - 1, shadow);
            context.fill(px - 2, y + h - 2, px + 2, y + h - 1, corruption);
            context.fill(px, y + h - 5, px + 1, y + h - 2, bruise);
            context.fill(px, y + h - 3, px + 1, y + h - 2, highlight);
            if (flip) {
                context.fill(px + 1, y + h - 3, px + 4, y + h - 2, corruption);
                context.fill(px + 3, y + h - 4, px + 4, y + h - 3, ink);
            } else {
                context.fill(px - 3, y + h - 3, px, y + h - 2, corruption);
                context.fill(px - 3, y + h - 4, px - 2, y + h - 3, ink);
            }
        }

        context.fill(x + 2, y + 2, x + 7, y + 3, shadow);
        context.fill(x + 3, y + 3, x + 4, y + 7, corruption);
        context.fill(x + 4, y + 3, x + 5, y + 4, highlight);
        context.fill(x + w - 7, y + h - 3, x + w - 2, y + h - 2, shadow);
        context.fill(x + w - 4, y + h - 7, x + w - 3, y + h - 3, corruption);
        context.fill(x + w - 5, y + h - 4, x + w - 4, y + h - 3, highlight);
    }
}
