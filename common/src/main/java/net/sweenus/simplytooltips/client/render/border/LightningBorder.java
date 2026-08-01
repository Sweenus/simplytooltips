package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Mirrored bolts with corner sparks. Accents: A bolt, B bolt shadow, C spark. */
public class LightningBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFE7F1FF, 0xFF9CB8F8, 0xFF74A8FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int boltA = p.a(), boltB = p.b(), spark = p.c();
        for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
            if ((i & 1) == 0) {
                context.fill(px, y + 1, px + 1, y + 3, boltA);
                context.fill(px + 1, y + 2, px + 2, y + 4, boltB);
                context.fill(px + 2, y + 2, px + 4, y + 3, spark);
                context.fill(px, y + h - 4, px + 1, y + h - 2, boltA);
                context.fill(px + 1, y + h - 5, px + 2, y + h - 3, boltB);
                context.fill(px + 2, y + h - 4, px + 4, y + h - 3, spark);
            } else {
                context.fill(px + 2, y + 1, px + 3, y + 3, boltA);
                context.fill(px + 1, y + 2, px + 2, y + 4, boltB);
                context.fill(px, y + 2, px + 1, y + 3, spark);
                context.fill(px + 2, y + h - 4, px + 3, y + h - 2, boltA);
                context.fill(px + 1, y + h - 5, px + 2, y + h - 3, boltB);
                context.fill(px, y + h - 4, px + 1, y + h - 3, spark);
            }
        }
        context.fill(x + 3, y + 3, x + 5, y + 4, spark);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 4, spark);
        context.fill(x + 3, y + h - 4, x + 5, y + h - 3, spark);
        context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, spark);
    }
}
