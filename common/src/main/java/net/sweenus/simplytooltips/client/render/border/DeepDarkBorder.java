package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Sculk veins with glowing nodes. Accents: A vein, B vein shadow, C glow, D ink. */
public class DeepDarkBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF2E8985, 0xFF235F5B, 0xFF66D7CD, 0xFF142327);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int veinA = p.a(), veinB = p.b(), glow = p.c(), ink = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            boolean flip = (i & 1) == 0;

            context.fill(px, y + 1, px + 2, y + 2, veinA);
            context.fill(px + 1, y + 2, px + 3, y + 3, veinB);
            context.fill(px + 1, y + 1, px + 2, y + 2, glow);
            if (flip) context.fill(px - 1, y + 2, px, y + 3, ink);
            else      context.fill(px + 3, y + 2, px + 4, y + 3, ink);

            context.fill(px, y + h - 3, px + 2, y + h - 2, veinA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, veinB);
            context.fill(px, y + h - 2, px + 1, y + h - 1, glow);
            if (flip) context.fill(px + 2, y + h - 2, px + 3, y + h - 1, ink);
            else      context.fill(px - 2, y + h - 2, px - 1, y + h - 1, ink);
        }

        context.fill(x + 3, y + 3, x + 5, y + 5, glow);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 5, glow);
        context.fill(x + 3, y + h - 5, x + 5, y + h - 3, glow);
        context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, glow);
    }
}
