package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Ooze drips and spores. Accents: A ooze, B ooze shadow, C toxic highlight, D dark fleck. */
public class PoisonBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF6AB64A, 0xFF3C8A34, 0xFFA5F06E, 0xFF223A1E);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int oozeA = p.a(), oozeB = p.b(), toxic = p.c(), dark = p.d();

        for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
            boolean flip = (i & 1) == 0;

            // Top ooze drips
            context.fill(px, y + 1, px + 2, y + 2, oozeA);
            context.fill(px + 1, y + 2, px + 3, y + 3, oozeB);
            if (flip) context.fill(px + 1, y + 3, px + 2, y + 4, toxic);
            else      context.fill(px, y + 3, px + 1, y + 4, toxic);

            // Bottom mirrored spores
            context.fill(px, y + h - 3, px + 2, y + h - 2, oozeA);
            context.fill(px - 1, y + h - 2, px + 1, y + h - 1, oozeB);
            if (flip) context.fill(px, y + h - 4, px + 1, y + h - 3, toxic);
            else      context.fill(px + 1, y + h - 4, px + 2, y + h - 3, toxic);

            if ((i % 3) == 1) {
                context.fill(px + 3, y + 1, px + 4, y + 2, dark);
                context.fill(px - 2, y + h - 2, px - 1, y + h - 1, dark);
            }
        }

        context.fill(x + 3, y + 3, x + 5, y + 4, toxic);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, toxic);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, toxic);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, toxic);
    }
}
