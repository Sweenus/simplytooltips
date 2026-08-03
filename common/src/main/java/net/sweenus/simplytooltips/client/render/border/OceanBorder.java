package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Kelp-toned tongues and cinders. Accents: A frond, B frond shadow, C highlight, D deep fleck. */
public class OceanBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF3A662F, 0xFF264A24, 0xFF5D8E49, 0xFF132013);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int frondA = p.a(), frondB = p.b(), highlight = p.c(), deep = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            boolean flip = (i & 1) == 0;

            // Top tongues
            context.fill(px, y + 1, px + 2, y + 2, frondA);
            context.fill(px + 1, y + 2, px + 3, y + 3, frondB);
            if (flip) context.fill(px + 1, y, px + 2, y + 1, highlight);
            else      context.fill(px, y, px + 1, y + 1, highlight);

            // Bottom cinders
            context.fill(px, y + h - 3, px + 2, y + h - 2, frondA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, frondB);
            if (flip) context.fill(px, y + h - 1, px + 1, y + h, highlight);
            else      context.fill(px + 1, y + h - 1, px + 2, y + h, highlight);

            if ((i % 3) == 1) {
                context.fill(px + 3, y + 1, px + 4, y + 2, deep);
                context.fill(px - 2, y + h - 2, px - 1, y + h - 1, deep);
            }
        }

        context.fill(x + 3, y + 3, x + 5, y + 4, highlight);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, highlight);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, highlight);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, highlight);
    }
}
