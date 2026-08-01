package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;
import net.sweenus.simplytooltips.client.render.TooltipPainter;

/**
 * Rubble chunks with dust flecks. Accents: A dark rock, B mid rock, C dust.
 *
 * <p>Unlike the other built-ins this one derives its default colours from the active theme, so an
 * earthy border tints itself to whatever palette it sits in. A border definition can still pin the
 * accents to fixed colours.
 */
public class EarthBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(
                TooltipPainter.lerpColor(theme.border(), 0xFF3A2E22, 0.55f),
                TooltipPainter.lerpColor(theme.border(), 0xFF6E5A40, 0.32f),
                TooltipPainter.lerpColor(theme.borderInner(), 0xFFCAB28D, 0.22f)
        );
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int rockDark = p.a(), rockMid = p.b(), dust = p.c();
        for (int px = x + 8, i = 0; px < x + w - 9; px += 10, i++) {
            int wChunk = (i % 3 == 0) ? 3 : 2;
            context.fill(px, y + 1, px + wChunk, y + 2, rockMid);
            context.fill(px + 1, y + 2, px + wChunk + 1, y + 3, rockDark);
            context.fill(px, y + h - 3, px + wChunk, y + h - 2, rockMid);
            context.fill(px - 1, y + h - 2, px + wChunk - 1, y + h - 1, rockDark);
        }
        for (int px = x + 14; px < x + w - 14; px += 18) {
            context.fill(px, y + 2, px + 1, y + 4, dust);
            context.fill(px + 1, y + 3, px + 2, y + 4, rockDark);
            context.fill(px, y + h - 4, px + 1, y + h - 2, dust);
            context.fill(px - 1, y + h - 3, px, y + h - 2, rockDark);
        }
        context.fill(x + 3, y + 3, x + 5, y + 5, rockMid);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 5, rockMid);
        context.fill(x + 3, y + h - 5, x + 5, y + h - 3, rockMid);
        context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, rockMid);
    }
}
