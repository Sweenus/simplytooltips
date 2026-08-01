package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Frost crosses. Accents: A ice. */
public class IceBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFBFE9FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int ice = p.a();
        for (int px = x + 10; px < x + w - 10; px += 12) {
            context.fill(px, y, px + 1, y + 3, ice);
            context.fill(px - 1, y + 1, px + 2, y + 2, ice);
            context.fill(px, y + h - 3, px + 1, y + h, ice);
            context.fill(px - 1, y + h - 2, px + 2, y + h - 1, ice);
        }
    }
}
