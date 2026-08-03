package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Planked wood notches with rivets. Accents: A wood, B wood shadow, C straw, D rivet. */
public class RusticBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFA87A4A, 0xFF7B5532, 0xFFD7B37A, 0xFF4B5A5D);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int woodA = p.a(), woodB = p.b(), straw = p.c(), nail = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 14, i++) {
            boolean flip = (i & 1) == 0;

            // Top wood notch
            context.fill(px, y + 1, px + 3, y + 2, woodA);
            context.fill(px + 1, y + 2, px + 4, y + 3, woodB);
            if (flip) context.fill(px + 1, y + 1, px + 2, y + 2, straw);
            else      context.fill(px + 2, y + 1, px + 3, y + 2, straw);

            // Bottom mirrored notch
            context.fill(px, y + h - 3, px + 3, y + h - 2, woodA);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, woodB);
            if (flip) context.fill(px, y + h - 3, px + 1, y + h - 2, straw);
            else      context.fill(px + 1, y + h - 3, px + 2, y + h - 2, straw);
        }

        // tiny rivet-like corner studs
        context.fill(x + 3, y + 3, x + 4, y + 4, nail);
        context.fill(x + w - 4, y + 3, x + w - 3, y + 4, nail);
        context.fill(x + 3, y + h - 4, x + 4, y + h - 3, nail);
        context.fill(x + w - 4, y + h - 4, x + w - 3, y + h - 3, nail);
    }
}
