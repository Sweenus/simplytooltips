package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Honey blobs with wax caps. Accents: A honey, B wax, C outline, D corner glint. */
public class BeeBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFE8B847, 0xFFF4D77B, 0xFF6A4A1C, 0x99DDF7FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int honey = p.a(), wax = p.b(), outline = p.c(), glint = p.d();
        for (int px = x + 8; px < x + w - 10; px += 12) {
            context.fill(px, y + 1, px + 3, y + 3, honey);
            context.fill(px + 1, y, px + 2, y + 1, wax);
            context.fill(px, y + 1, px + 1, y + 2, outline);
            context.fill(px + 2, y + 2, px + 3, y + 3, outline);
            context.fill(px, y + h - 3, px + 3, y + h - 1, honey);
            context.fill(px + 1, y + h - 1, px + 2, y + h, wax);
            context.fill(px, y + h - 2, px + 1, y + h - 1, outline);
            context.fill(px + 2, y + h - 3, px + 3, y + h - 2, outline);
        }
        context.fill(x + 3, y + 4, x + 5, y + 5, glint);
        context.fill(x + w - 5, y + 4, x + w - 3, y + 5, glint);
        context.fill(x + 3, y + h - 5, x + 5, y + h - 4, glint);
        context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 4, glint);
    }
}
