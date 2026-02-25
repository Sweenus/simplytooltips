package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility methods for drawing common tooltip elements:
 * background, separators, item icon frames, footer dots, badges,
 * animated title text, plus colour helpers and word-wrap.
 */
public class TooltipPainter {

    // --- Background ---

    /** Fills the tooltip panel with a vertical gradient from {@code theme.bgTop()} to {@code theme.bgBottom()}. */
    public static void drawGradientBackground(DrawContext context, int x, int y, int w, int h, TooltipTheme theme) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0.0F : (float) i / (float) (h - 1);
            int row = lerpColor(theme.bgTop(), theme.bgBottom(), t);
            context.fill(x, y + i, x + w, y + i + 1, row);
        }
    }

    // --- Separators ---

    /**
     * Draws a horizontal separator line with a centred diamond break.
     * The separator occupies 10 pixels of vertical space (lineY = y + 4).
     */
    public static void drawSeparator(DrawContext context, int x, int y, int width, TooltipTheme theme) {
        int lineY = y + 4;
        int midX  = x + width / 2;
        context.fill(x + 4, lineY, midX - 5, lineY + 1, theme.separator());
        context.fill(midX + 5, lineY, x + width - 4, lineY + 1, theme.separator());
        BorderRenderer.drawSmallDiamond(context, midX, lineY, theme.border());
    }

    // --- Item icon frames ---

    /**
     * Dispatches to the appropriate item-frame drawing method based on {@code shape}.
     *
     * <p>Recognised shape values:
     * <ul>
     *   <li>{@code "diamond"} — rotated diamond (default)
     *   <li>{@code "square"}  — beveled square
     *   <li>{@code "circle"}  — pixel circle (Bresenham)
     *   <li>{@code "cross"}   — plus / cross shape
     *   <li>{@code "none"}    — no frame drawn
     * </ul>
     */
    public static void drawItemFrame(DrawContext context, int x, int y, int size,
                                     TooltipTheme theme, String shape) {
        switch (shape != null ? shape : "diamond") {
            case "square"  -> drawSquareFrame(context, x, y, size, theme);
            case "circle"  -> drawCircleFrame(context, x, y, size, theme);
            case "cross"   -> drawCrossFrame(context, x, y, size, theme);
            case "none"    -> { /* no frame */ }
            default        -> drawDiamondFrame(context, x, y, size, theme);
        }
    }

    /** Diamond frame (24×24 default) around the item icon slot. */
    public static void drawDiamondFrame(DrawContext context, int x, int y, int size, TooltipTheme theme) {
        int cx = x + size / 2, cy = y + size / 2, half = size / 2;
        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrameInner());
        }
        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx - span + 1, cy + dy + 1, theme.diamondFrame());
            context.fill(cx + span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrame());
        }
        context.fill(cx, cy - half, cx + 1, cy - half + 1, theme.diamondFrame());
        context.fill(cx, cy + half, cx + 1, cy + half + 1, theme.diamondFrame());
    }

    /**
     * Beveled square frame: a solid rectangle with 1-pixel cut corners and a 1-pixel border.
     */
    private static void drawSquareFrame(DrawContext context, int x, int y, int size, TooltipTheme theme) {
        // Fill interior (inset by 1 on all sides — corners are left empty)
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, theme.diamondFrameInner());
        // Borders — no corner pixels (beveled look)
        context.fill(x + 1, y,          x + size - 1, y + 1,          theme.diamondFrame()); // top
        context.fill(x + 1, y + size - 1, x + size - 1, y + size,     theme.diamondFrame()); // bottom
        context.fill(x,     y + 1,      x + 1,        y + size - 1,   theme.diamondFrame()); // left
        context.fill(x + size - 1, y + 1, x + size,  y + size - 1,    theme.diamondFrame()); // right
    }

    /**
     * Circular frame using a scanline fill for the interior and a Bresenham outline for the border.
     */
    private static void drawCircleFrame(DrawContext context, int x, int y, int size, TooltipTheme theme) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r  = size / 2 - 1; // 11 for size=24

        // Scanline-fill solid disk with inner color
        for (int dy = -r; dy <= r; dy++) {
            int span = (int) Math.sqrt((double) r * r - (double) dy * dy);
            context.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrameInner());
        }

        // Bresenham circle outline overdraw
        int bx = 0, by = r, err = 3 - 2 * r;
        while (bx <= by) {
            plotCircleOctants(context, cx, cy, bx, by, theme.diamondFrame());
            if (err < 0) {
                err += 4 * bx + 6;
            } else {
                err += 4 * (bx - by) + 10;
                by--;
            }
            bx++;
        }
    }

    /** Plots all 8 symmetrical pixels for a Bresenham circle step. */
    private static void plotCircleOctants(DrawContext context, int cx, int cy, int bx, int by, int color) {
        context.fill(cx + bx, cy + by, cx + bx + 1, cy + by + 1, color);
        context.fill(cx - bx, cy + by, cx - bx + 1, cy + by + 1, color);
        context.fill(cx + bx, cy - by, cx + bx + 1, cy - by + 1, color);
        context.fill(cx - bx, cy - by, cx - bx + 1, cy - by + 1, color);
        context.fill(cx + by, cy + bx, cx + by + 1, cy + bx + 1, color);
        context.fill(cx - by, cy + bx, cx - by + 1, cy + bx + 1, color);
        context.fill(cx + by, cy - bx, cx + by + 1, cy - bx + 1, color);
        context.fill(cx - by, cy - bx, cx - by + 1, cy - bx + 1, color);
    }

    /**
     * Plus / cross frame: two overlapping bars forming a + shape.
     * Arm half-width = size/4 (6 px for size=24).
     */
    private static void drawCrossFrame(DrawContext context, int x, int y, int size, TooltipTheme theme) {
        int h   = size / 2;   // 12 for size=24
        int arm = size / 4;   // 6  for size=24
        int f   = theme.diamondFrame();

        // Fill cross interior
        context.fill(x,         y + h - arm, x + size,   y + h + arm, theme.diamondFrameInner()); // horizontal bar
        context.fill(x + h - arm, y,         x + h + arm, y + size,   theme.diamondFrameInner()); // vertical bar

        // Top arm border
        context.fill(x + h - arm,     y,        x + h + arm,     y + 1,        f); // top cap
        context.fill(x + h - arm - 1, y,        x + h - arm,     y + h - arm,  f); // left side
        context.fill(x + h + arm,     y,        x + h + arm + 1, y + h - arm,  f); // right side

        // Right arm border
        context.fill(x + size - 1,    y + h - arm, x + size,        y + h + arm, f); // right cap
        context.fill(x + h + arm,     y + h - arm - 1, x + size,    y + h - arm, f); // top edge
        context.fill(x + h + arm,     y + h + arm,     x + size,    y + h + arm + 1, f); // bottom edge

        // Bottom arm border
        context.fill(x + h - arm,     y + size - 1, x + h + arm,   y + size,    f); // bottom cap
        context.fill(x + h - arm - 1, y + h + arm,  x + h - arm,   y + size,    f); // left side
        context.fill(x + h + arm,     y + h + arm,  x + h + arm + 1, y + size,  f); // right side

        // Left arm border
        context.fill(x,               y + h - arm, x + 1,           y + h + arm, f); // left cap
        context.fill(x,               y + h - arm - 1, x + h - arm, y + h - arm, f); // top edge
        context.fill(x,               y + h + arm,     x + h - arm, y + h + arm + 1, f); // bottom edge
    }

    // --- Footer ---

    /**
     * Draws three footer diamonds horizontally centred at {@code cx}.
     * Spaced 8 pixels apart. Call with {@code cx = panelX + panelW / 2}.
     */
    public static void drawFooterDots(DrawContext context, int cx, int y, TooltipTheme theme) {
        BorderRenderer.drawSmallDiamond(context, cx - 8, y, theme.footerDot());
        BorderRenderer.drawSmallDiamond(context, cx,     y, theme.footerDot());
        BorderRenderer.drawSmallDiamond(context, cx + 8, y, theme.footerDot());
    }

    // --- Badges ---

    /**
     * Draws a coloured badge box with {@code label} inside it.
     *
     * @return the x coordinate immediately after the badge (for chaining multiple badges)
     */
    public static int drawBadge(DrawContext context, TextRenderer tr, String label,
                                 int x, int y, TooltipTheme theme) {
        int textW    = tr.getWidth(label);
        int padH     = 3;
        int badgeH   = tr.fontHeight;
        int badgeW   = textW + padH * 2;
        context.fill(x, y, x + badgeW, y + badgeH, theme.badgeBg());
        context.drawText(tr,
                Text.literal(label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.badgeCutout() & 0x00FFFFFF))),
                x + padH, y + 1, theme.badgeCutout(), false);
        return x + badgeW;
    }

    // --- Title animations ---

    /**
     * Draws {@code text} with a travelling wave that lifts each character slightly.
     * The wave cycles: active for {@code 520 + length * 85} ms then idle for 1700 ms.
     *
     * @param timeMs absolute time in milliseconds (e.g. {@code System.currentTimeMillis()})
     */
    public static void drawWaveText(DrawContext context, TextRenderer tr, String text,
                                    int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        final int    length    = text.length();
        final double amplitude = 1.4;
        final double spread    = 2.2;
        final long   idleMs    = 1700L;
        final long   activeMs  = 520L + (long) (length * 85L);
        final long   cycleMs   = activeMs + idleMs;

        long    cyclePos      = Math.floorMod(timeMs, cycleMs);
        boolean active        = cyclePos < activeMs;
        double  activeProgress = active ? (double) cyclePos / (double) activeMs : 1.0;
        double  startPeak     = -spread;
        double  endPeak       = (length - 1) + spread;
        double  peakPos       = startPeak + (endPeak - startPeak) * activeProgress;

        int cursorX = x;
        for (int i = 0; i < length; i++) {
            String ch       = String.valueOf(text.charAt(i));
            double distance = Math.abs(i - peakPos);
            double influence = distance < spread
                    ? 0.5 * (1.0 + Math.cos(Math.PI * (distance / spread)))
                    : 0.0;
            int yOffset = (int) Math.round(-influence * amplitude);
            context.drawText(tr,
                    Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                    cursorX, y + yOffset, color, true);
            cursorX += tr.getWidth(ch);
        }
    }

    /**
     * Draws {@code text} with a travelling brightness shimmer (glint) that sweeps each character.
     * Cycles: active for {@code 400 + length * 60} ms then idle for 2200 ms.
     */
    public static void drawShimmerText(DrawContext context, TextRenderer tr, String text,
                                       int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        final int    length   = text.length();
        final long   idleMs   = 2200L;
        final long   activeMs = 400L + (long)(length * 60L);
        final long   cycleMs  = activeMs + idleMs;
        final double spread   = 2.5;

        long    cyclePos = Math.floorMod(timeMs, cycleMs);
        boolean active   = cyclePos < activeMs;
        double  progress = active ? (double) cyclePos / activeMs : 2.0; // 2.0 → wave off-screen
        double  peakPos  = -spread + (length - 1 + 2.0 * spread) * progress;

        int baseA = (color >>> 24) & 0xFF;
        int baseR = (color >>> 16) & 0xFF;
        int baseG = (color >>>  8) & 0xFF;
        int baseB =  color         & 0xFF;

        int cursorX = x;
        for (int i = 0; i < length; i++) {
            String ch    = String.valueOf(text.charAt(i));
            double dist  = Math.abs(i - peakPos);
            double shine = dist < spread ? 0.5 * (1.0 + Math.cos(Math.PI * dist / spread)) : 0.0;
            float factor = (float)(shine * 0.60); // up to 60% lerp toward white
            int r = Math.min(255, baseR + (int)((255 - baseR) * factor));
            int g = Math.min(255, baseG + (int)((255 - baseG) * factor));
            int b = Math.min(255, baseB + (int)((255 - baseB) * factor));
            int c = (baseA << 24) | (r << 16) | (g << 8) | b;
            context.drawText(tr,
                    Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(c & 0x00FFFFFF))),
                    cursorX, y, c, true);
            cursorX += tr.getWidth(ch);
        }
    }

    /**
     * Draws {@code text} with a slow sinusoidal brightness pulse — the whole title
     * brightens toward white and back on a ~2.9 s period.
     */
    public static void drawPulseText(DrawContext context, TextRenderer tr, String text,
                                     int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        double pulse  = 0.5 * (1.0 + Math.sin(timeMs * 0.0022)); // 0.0 → 1.0
        float  factor = (float)(pulse * 0.35);                     // up to 35% brighter

        int baseA = (color >>> 24) & 0xFF;
        int r = Math.min(255, ((color >>> 16) & 0xFF) + (int)((255 - ((color >>> 16) & 0xFF)) * factor));
        int g = Math.min(255, ((color >>>  8) & 0xFF) + (int)((255 - ((color >>>  8) & 0xFF)) * factor));
        int b = Math.min(255, ( color         & 0xFF) + (int)((255 - ( color         & 0xFF)) * factor));
        int c = (baseA << 24) | (r << 16) | (g << 8) | b;
        context.drawText(tr,
                Text.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(c & 0x00FFFFFF))),
                x, y, c, true);
    }

    /**
     * Draws {@code text} with a subtle irregular flicker — two slightly irrational
     * sine waves combine to produce a pseudo-random fire-light brightness variation.
     */
    public static void drawFlickerText(DrawContext context, TextRenderer tr, String text,
                                       int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        double flicker = Math.sin(timeMs * 0.0317) * Math.cos(timeMs * 0.0193 + 1.2);
        float  factor  = (float)(flicker * 0.14 + 0.06); // roughly -0.08 to +0.20

        int baseA = (color >>> 24) & 0xFF;
        int r = Math.min(255, Math.max(0, ((color >>> 16) & 0xFF) + (int)((255 - ((color >>> 16) & 0xFF)) * factor)));
        int g = Math.min(255, Math.max(0, ((color >>>  8) & 0xFF) + (int)((255 - ((color >>>  8) & 0xFF)) * factor)));
        int b = Math.min(255, Math.max(0, ( color         & 0xFF) + (int)((255 - ( color         & 0xFF)) * factor)));
        int c = (baseA << 24) | (r << 16) | (g << 8) | b;
        context.drawText(tr,
                Text.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(c & 0x00FFFFFF))),
                x, y, c, true);
    }

    // --- Colour helpers ---

    /** Component-wise ARGB linear interpolation. */
    public static int lerpColor(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF, aR = (a >>> 16) & 0xFF, aG = (a >>> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >>> 16) & 0xFF, bG = (b >>> 8) & 0xFF, bB = b & 0xFF;
        return ((int)(aA + (bA - aA) * t) << 24) | ((int)(aR + (bR - aR) * t) << 16) |
               ((int)(aG + (bG - aG) * t) << 8)  |  (int)(aB + (bB - aB) * t);
    }

    // --- Text utilities ---

    /**
     * Word-wraps a list of strings so that no line exceeds {@code maxWidth} pixels
     * (as measured by {@code tr.getWidth}).  Blank / null lines become {@code " "}.
     */
    public static List<String> wrapStrings(List<String> lines, TextRenderer tr, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null || raw.isEmpty()) {
                wrapped.add(" ");
                continue;
            }
            String[] words = raw.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (tr.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                    wrapped.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) wrapped.add(current.toString());
        }
        return wrapped;
    }

    private TooltipPainter() {}
}
