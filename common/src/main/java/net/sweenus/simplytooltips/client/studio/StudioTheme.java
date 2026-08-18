package net.sweenus.simplytooltips.client.studio;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Chrome palette and drawing primitives for the Theme Studio.
 *
 * <p>Deliberately separate from {@code TooltipTheme}: these colours dress the editor itself and
 * never change, while the theme being edited can be any colour at all.
 */
public final class StudioTheme {

    public static final int SCRIM        = 0xB4060709;
    public static final int PANEL        = 0xF0111318;
    public static final int PANEL_EDGE   = 0xFF272C36;
    public static final int HEADER       = 0xFF171B22;
    public static final int RAIL         = 0xFF0E1116;
    public static final int STAGE        = 0xFF090B0F;
    public static final int STAGE_EDGE   = 0xFF1C2029;
    public static final int FIELD        = 0xFF0A0C10;
    public static final int FIELD_EDGE   = 0xFF232936;

    public static final int ACCENT       = 0xFFE0B457;
    public static final int ACCENT_SOFT  = 0x26E0B457;
    public static final int ACCENT_DIM   = 0xFF8A6A2E;

    public static final int TEXT_PRIMARY = 0xFFE6EAF0;
    public static final int TEXT_BODY    = 0xFFAAB2C0;
    public static final int TEXT_DIM     = 0xFF6B7484;
    public static final int TEXT_OFF     = 0xFF454C59;

    public static final int HOVER        = 0x14FFFFFF;
    public static final int DANGER       = 0xFFD05A4A;
    public static final int OK           = 0xFF6FCB63;

    public static final int BUTTON       = 0xFF1B2029;
    public static final int BUTTON_EDGE  = 0xFF3A414F;
    public static final int BUTTON_HOT   = 0xFF262D39;
    public static final int PRIMARY_FILL = 0xFF2A2413;

    public static final int ROW_H = 12;

    /**
     * A panel with a one-pixel edge whose corner pixels are left out. At Minecraft's GUI scale that
     * single omitted pixel is what makes the card read as rounded rather than as a hard box.
     */
    public static void card(DrawContext context, int x, int y, int w, int h, int fill, int edge) {
        if (w < 2 || h < 2) return;
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        context.fill(x + 1, y, x + w - 1, y + 1, edge);
        context.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        context.fill(x, y + 1, x + 1, y + h - 1, edge);
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
    }

    public static void hLine(DrawContext context, int x, int y, int w, int color) {
        context.fill(x, y, x + w, y + 1, color);
    }

    public static void vLine(DrawContext context, int x, int y, int h, int color) {
        context.fill(x, y, x + 1, y + h, color);
    }

    /** A checker of faint dots, so a dark tooltip is still visibly sitting on a stage. */
    public static void stageGrid(DrawContext context, int x, int y, int w, int h) {
        for (int gx = x + 2; gx < x + w - 2; gx += 8) {
            for (int gy = y + 2; gy < y + h - 2; gy += 8) {
                context.fill(gx, gy, gx + 1, gy + 1, 0x18FFFFFF);
            }
        }
    }

    /**
     * Shortens {@code text} to fit {@code maxWidth}, marking the cut so a clipped theme key is never
     * mistaken for a real one.
     */
    public static String trim(TextRenderer tr, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (tr.getWidth(text) <= maxWidth) return text;
        // A one or two character label is all signal: cutting "<" down to ".." replaces the only
        // thing it said with nothing. Let it overflow its box instead.
        if (text.length() <= 2) return text;

        String cut = text;
        while (!cut.isEmpty() && tr.getWidth(cut + "..") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.isEmpty() ? text : cut + "..";
    }

    /**
     * Shortens by cutting from the middle, keeping the tail.
     *
     * <p>Theme colour keys differ by their endings - {@code diamondFrame} against
     * {@code diamondFrameInner} - so trimming from the right turns two different rows into the
     * same string. Keeping the last few characters keeps them apart at any column width.
     */
    public static String trimMiddle(TextRenderer tr, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (tr.getWidth(text) <= maxWidth) return text;

        int tailLength = Math.min(5, text.length() - 1);
        String tail = text.substring(text.length() - tailLength);
        String head = text.substring(0, text.length() - tailLength);

        while (!head.isEmpty() && tr.getWidth(head + ".." + tail) > maxWidth) {
            head = head.substring(0, head.length() - 1);
        }
        return head.isEmpty() ? trim(tr, text, maxWidth) : head + ".." + tail;
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Draws a colour sample over a checkerboard, so partial alpha is visible rather than guessed. */
    public static void swatch(DrawContext context, int x, int y, int w, int h, int argb) {
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                boolean light = ((cx / 3) + (cy / 3)) % 2 == 0;
                context.fill(x + cx, y + cy, x + cx + 1, y + cy + 1, light ? 0xFF6A6A6A : 0xFF3C3C3C);
            }
        }
        context.fill(x, y, x + w, y + h, argb);
        context.fill(x - 1, y - 1, x + w + 1, y, BUTTON_EDGE);
        context.fill(x - 1, y + h, x + w + 1, y + h + 1, BUTTON_EDGE);
        context.fill(x - 1, y, x, y + h, BUTTON_EDGE);
        context.fill(x + w, y, x + w + 1, y + h, BUTTON_EDGE);
    }

    public static String hex(int argb) {
        return String.format("%08X", argb);
    }

    private StudioTheme() {}
}
