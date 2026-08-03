package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Wave crests with foam bubbles. Accents: A crest, B foam. */
public class BubbleBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFF8EE7F8, 0xFFC8F7FF);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int crest = p.a(), foam = p.b();
        for (int px = x + 6, step = 0; px < x + w - 6; px += 8, step++) {
            int dy = (step % 2 == 0) ? 0 : 1;
            context.fill(px, y + 1 + dy, px + 5, y + 2 + dy, crest);
            context.fill(px, y + h - 2 - dy, px + 5, y + h - 1 - dy, crest);
        }
        for (int px = x + 12; px < x + w - 12; px += 18) {
            context.fill(px, y + 2, px + 2, y + 4, foam);
            context.fill(px + 1, y + 3, px + 3, y + 5, crest);
            context.fill(px, y + h - 5, px + 2, y + h - 3, foam);
            context.fill(px + 1, y + h - 4, px + 3, y + h - 2, crest);
        }
    }
}
