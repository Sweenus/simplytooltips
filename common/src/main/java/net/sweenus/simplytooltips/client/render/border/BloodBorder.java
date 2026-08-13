package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Running blood and dried flecks. Accents: A blood, B shadow, C fresh highlight, D dried fleck. */
public class BloodBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFB6404A, 0xFF8A2A32, 0xFFF06E78, 0xFF3A1E20);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int bloodA = p.a(), bloodB = p.b(), fresh = p.c(), dried = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            boolean flip = (i & 1) == 0;

            // Top runs
            context.fill(px, y + 1, px + 2, y + 2, bloodA);
            context.fill(px + 1, y + 2, px + 3, y + 3, bloodB);
            if (flip) context.fill(px + 1, y + 3, px + 2, y + 4, fresh);
            else      context.fill(px, y + 3, px + 1, y + 4, fresh);

            // Bottom mirrored spatter
            context.fill(px, y + h - 3, px + 2, y + h - 2, bloodA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, bloodB);
            if (flip) context.fill(px, y + h - 4, px + 1, y + h - 3, fresh);
            else      context.fill(px + 1, y + h - 4, px + 2, y + h - 3, fresh);

            if ((i % 3) == 1) {
                context.fill(px + 3, y + 1, px + 4, y + 2, dried);
                context.fill(px - 2, y + h - 2, px - 1, y + h - 1, dried);
            }
        }

        context.fill(x + 3, y + 3, x + 5, y + 4, fresh);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, fresh);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, fresh);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, fresh);
    }
}
