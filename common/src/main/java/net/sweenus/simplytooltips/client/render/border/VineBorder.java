package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Alternating leaves with occasional stems. Accents: A leaf, B alternate leaf, C stem. */
public class VineBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF79BE77, 0xFF5EA661, 0xFF3E7A44);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int leafA = p.a(), leafB = p.b(), stem = p.c();
        for (int px = x + 9, i = 0; px < x + w - 10; px += 11, i++) {
            boolean flip = (i & 1) == 0;
            int leaf = flip ? leafA : leafB;
            context.fill(px, y + 1, px + 2, y + 2, leaf);
            if (flip) { context.fill(px + 1, y + 2, px + 3, y + 3, leaf); }
            else       { context.fill(px - 1, y + 2, px + 1, y + 3, leaf); }
            context.fill(px, y + h - 3, px + 2, y + h - 2, leaf);
            if (flip) { context.fill(px - 1, y + h - 2, px + 1, y + h - 1, leaf); }
            else       { context.fill(px + 1, y + h - 2, px + 3, y + h - 1, leaf); }
            if (i % 3 == 0) {
                context.fill(px, y + 3, px + 1, y + 5, stem);
                context.fill(px, y + h - 5, px + 1, y + h - 3, stem);
            }
        }
    }
}
