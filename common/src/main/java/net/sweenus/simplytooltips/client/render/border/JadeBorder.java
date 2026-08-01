package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Jade inlay with gold filigree. Accents: A jade, B jade shadow, C gold, D bright gold, E deep jade. */
public class JadeBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF49A77B, 0xFF2D7E5A, 0xFFE4BC5E, 0xFFFFE3A0, 0xFF1F5A43);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int jadeA = p.a(), jadeB = p.b(), goldA = p.c(), goldB = p.d(), deep = p.e();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            boolean flip = (i & 1) == 0;

            context.fill(px, y + 1, px + 3, y + 2, jadeA);
            context.fill(px + 1, y + 2, px + 4, y + 3, jadeB);
            context.fill(px + 1, y + 1, px + 2, y + 2, goldA);
            if (flip) context.fill(px + 2, y, px + 3, y + 1, goldB);

            context.fill(px, y + h - 3, px + 3, y + h - 2, jadeA);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, jadeB);
            context.fill(px, y + h - 2, px + 1, y + h - 1, goldA);
            if (!flip) context.fill(px + 1, y + h - 1, px + 2, y + h, goldB);

            if ((i % 3) == 1) {
                context.fill(px + 4, y + 1, px + 5, y + 2, deep);
                context.fill(px - 3, y + h - 2, px - 2, y + h - 1, deep);
            }
        }

        context.fill(x + 3, y + 3, x + 5, y + 5, goldA);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 5, goldA);
        context.fill(x + 3, y + h - 5, x + 5, y + h - 3, goldA);
        context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, goldA);
    }
}
