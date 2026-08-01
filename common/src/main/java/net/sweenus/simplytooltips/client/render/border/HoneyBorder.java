package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/** Honeycomb notches. Accents: A comb, B lower comb, C wax, D outline, E sticky corner highlight. */
public class HoneyBorder implements BorderPattern {

    @Override
    public BorderPalette defaultPalette(TooltipTheme theme) {
        return BorderPalette.accents(0xFFE0A72E, 0xFFF3CC6C, 0xFFFBE8AB, 0xFF6B4714, 0x88FFE6A0);
    }

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette p) {
        int combA = p.a(), combB = p.b(), wax = p.c(), outline = p.d(), sticky = p.e();

        for (int px = x + 8, i = 0; px < x + w - 9; px += 11, i++) {
            boolean shift = (i & 1) == 0;

            // Top mini-hex notch
            context.fill(px + 1, y + 1, px + 4, y + 2, combA);
            context.fill(px, y + 2, px + 1, y + 3, outline);
            context.fill(px + 4, y + 2, px + 5, y + 3, outline);
            context.fill(px + 1, y + 3, px + 4, y + 4, combB);
            context.fill(px + 2, y + 2, px + 3, y + 3, wax);
            if (shift) context.fill(px + 2, y, px + 3, y + 1, wax);

            // Bottom mirrored notch
            context.fill(px + 1, y + h - 4, px + 4, y + h - 3, combA);
            context.fill(px, y + h - 3, px + 1, y + h - 2, outline);
            context.fill(px + 4, y + h - 3, px + 5, y + h - 2, outline);
            context.fill(px + 1, y + h - 2, px + 4, y + h - 1, combB);
            context.fill(px + 2, y + h - 3, px + 3, y + h - 2, wax);
            if (!shift) context.fill(px + 2, y + h - 1, px + 3, y + h, wax);
        }

        // Sticky corner highlights
        context.fill(x + 3, y + 3, x + 5, y + 5, sticky);
        context.fill(x + w - 5, y + 3, x + w - 3, y + 5, sticky);
        context.fill(x + 3, y + h - 5, x + 5, y + h - 3, sticky);
        context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, sticky);
    }
}
