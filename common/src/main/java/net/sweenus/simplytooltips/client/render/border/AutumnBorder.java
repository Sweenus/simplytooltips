package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Drifting leaves with twigs and acorns. Accents: A leaf, B alternate leaf, C vein, D twig, E acorn. */
public class AutumnBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFE5A24F, 0xFFD67B35, 0xFF8A4A26, 0xFF6A3D24, 0xFFC18B54);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int leafA = p.a(), leafB = p.b(), vein = p.c(), twig = p.d(), acorn = p.e();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 13, i++) {
            boolean flip = (i & 1) == 0;
            int leaf = flip ? leafA : leafB;

            // Top drifting leaf motif
            context.fill(px, y + 1, px + 2, y + 2, leaf);
            context.fill(px + 1, y + 2, px + 3, y + 3, leaf);
            context.fill(px + 1, y + 1, px + 2, y + 2, vein);
            if (flip) context.fill(px - 1, y + 2, px, y + 3, twig);
            else      context.fill(px + 3, y + 2, px + 4, y + 3, twig);

            // Bottom mirrored motif
            context.fill(px, y + h - 3, px + 2, y + h - 2, leaf);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, leaf);
            context.fill(px, y + h - 2, px + 1, y + h - 1, vein);
            if (flip) context.fill(px + 2, y + h - 2, px + 3, y + h - 1, twig);
            else      context.fill(px - 2, y + h - 2, px - 1, y + h - 1, twig);

            // occasional acorn knot accent
            if ((i % 3) == 1) {
                context.fill(px + 4, y + 1, px + 5, y + 2, acorn);
                context.fill(px - 3, y + h - 2, px - 2, y + h - 1, acorn);
            }
        }

        // Corner twig accents
        context.fill(x + 3, y + 3, x + 5, y + 4, twig);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, twig);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, twig);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, twig);
    }
}
