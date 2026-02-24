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
 * background, separators, diamond frames, footer dots, badges,
 * wave-animated title text, plus colour helpers and word-wrap.
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

    // --- Structural shapes ---

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

    // --- Animated title text ---

    /**
     * Draws {@code text} with a travelling wave that lifts each character slightly.
     * The wave cycles: active for {@code 520 + length * 85} ms then idle for 1700 ms.
     *
     * @param timeMs absolute time in milliseconds (e.g. {@code System.currentTimeMillis()})
     */
    public static void drawWaveText(DrawContext context, TextRenderer tr, String text,
                                    int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        final int    length   = text.length();
        final double amplitude = 1.4;
        final double spread    = 2.2;
        final long   idleMs    = 1700L;
        final long   activeMs  = 520L + (long) (length * 85L);
        final long   cycleMs   = activeMs + idleMs;

        long   cyclePos       = Math.floorMod(timeMs, cycleMs);
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
