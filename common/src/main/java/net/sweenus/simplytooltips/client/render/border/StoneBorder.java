package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Chipped stone chunks. Accents: A chip, B chip shadow, C dust. */
public class StoneBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF8D939E, 0xFF636A76, 0xFFADB4C0);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int chipA = p.a(), chipB = p.b(), dust = p.c();
        for (int px = x + 8, i = 0; px < x + w - 9; px += 10, i++) {
            int wChunk = (i % 3 == 0) ? 3 : 2;
            context.fill(px, y + 1, px + wChunk, y + 2, chipA);
            context.fill(px + 1, y + 2, px + wChunk + 1, y + 3, chipB);
            context.fill(px, y + h - 3, px + wChunk, y + h - 2, chipA);
            context.fill(px - 1, y + h - 2, px + wChunk - 1, y + h - 1, chipB);
            if ((i % 4) == 1) {
                context.fill(px + 1, y + 1, px + 2, y + 2, dust);
                context.fill(px, y + h - 2, px + 1, y + h - 1, dust);
            }
        }
    }
}
