package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Static utility methods for drawing common tooltip elements:
 * background, separators, item icon frames, footer dots, badges,
 * animated title text, plus colour helpers and word-wrap.
 */
public class TooltipPainter {

    // State for one-shot "hinge_fall" title animation (resets when tooltip animation resets).
    private static long   hingeLastElapsedMs = -1L;
    private static int    hingeTargetIndex   = -1;
    private static long   hingeStartDelayMs  = 0L;
    private static int    hingeDirection     = 1;
    private static float  hingeDrift         = 0.0F;
    private static String hingeLastText      = "";

    // State for one-shot-per-tooltip random obfuscation toggles.
    private static long      obfLastElapsedMs = -1L;
    private static String    obfLastText      = "";
    private static long[]    obfNextToggleMs  = new long[0];
    private static boolean[] obfActive        = new boolean[0];

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

    // --- Scrollbar ---

    /**
     * Draws a 2 px wide vertical scrollbar on the inner-right edge of the tooltip panel.
     *
     * @param context      draw context
     * @param trackX       left edge of the 2 px track (typically {@code panelX + panelW - 4})
     * @param trackY       top of the scrollable viewport
     * @param trackH       height of the scrollable viewport
     * @param scrollOffset current scroll offset in pixels (0 = top)
     * @param scrollMax    maximum scroll offset in pixels
     * @param theme        active tooltip theme (used for thumb colour)
     */
    public static void drawScrollbar(DrawContext context, int trackX, int trackY, int trackH,
                                     int scrollOffset, int scrollMax, TooltipTheme theme) {
        if (scrollMax <= 0) return;

        // Track
        context.fill(trackX, trackY, trackX + 2, trackY + trackH, 0x44000000);

        // Thumb size — proportional to visible ratio, minimum 6 px
        float visibleRatio = (float) trackH / (float)(trackH + scrollMax);
        int   thumbH       = Math.max(6, (int)(trackH * visibleRatio));

        // Thumb position along track
        float thumbT = (float) scrollOffset / (float) scrollMax;
        int   thumbY = trackY + (int)((trackH - thumbH) * thumbT);

        // Thumb colour — theme border at 0xCC alpha
        int thumbColor = (theme.border() & 0x00FFFFFF) | 0xCC000000;
        context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, thumbColor);
    }

    // --- Tab dots ---

    /**
     * Draws one indicator dot per tab, centred at {@code cx}.
     * Dots are spaced 8 px apart. The active tab's dot is a brightened 5×5 diamond;
     * inactive tabs use the standard 3×3 footer diamond.
     *
     * @param context    draw context
     * @param cx         horizontal centre of the dot row
     * @param y          vertical centre of the dot row
     * @param tabs       ordered list of available tabs
     * @param activeTab  the currently active tab
     * @param theme      active tooltip theme
     */
    public static void drawTabDots(DrawContext context, int cx, int y,
                                   List<TabState.Tab> tabs, TabState.Tab activeTab,
                                   TooltipTheme theme) {
        int n = tabs.size();
        if (n == 0) return;

        // Dots are spaced 8 px apart; the row is centred on cx
        int startCx = cx - (n - 1) * 4;

        for (int i = 0; i < n; i++) {
            int dotCx = startCx + i * 8;
            if (tabs.get(i) == activeTab) {
                // Active: 5×5 diamond, brightened 50% toward white
                int c = lerpColor(theme.footerDot(), 0xFFFFFFFF, 0.50f);
                context.fill(dotCx,     y - 2, dotCx + 1, y - 1, c);
                context.fill(dotCx - 1, y - 1, dotCx + 2, y,     c);
                context.fill(dotCx - 2, y,     dotCx + 3, y + 1, c);
                context.fill(dotCx - 1, y + 1, dotCx + 2, y + 2, c);
                context.fill(dotCx,     y + 2, dotCx + 1, y + 3, c);
            } else {
                // Inactive: standard 3×3 footer diamond
                BorderRenderer.drawSmallDiamond(context, dotCx, y, theme.footerDot());
            }
        }
    }

    /**
     * Draws tab indicator dots plus a small keycap hint to the right (e.g. "G").
     * The keycap communicates the current keybind used to cycle tabs.
     */
    public static void drawTabDotsWithKeyHint(DrawContext context, TextRenderer tr, int cx, int y,
                                              List<TabState.Tab> tabs, TabState.Tab activeTab,
                                              TooltipTheme theme, String keyLabel) {
        drawTabDots(context, cx, y, tabs, activeTab, theme);

        String label = normaliseKeyLabel(keyLabel);
        if (label.isEmpty()) return;

        int n = tabs.size();
        if (n <= 0) return;

        int rightDotCx = cx + (n - 1) * 4;
        float keyTextScale = 0.70f;
        int textW = Math.max(1, Math.round(tr.getWidth(label) * keyTextScale));
        int keyW = Math.max(10, textW + 6);
        int keyH = 7;
        int keyX = rightDotCx + 8;
        int keyY = y - 4;

        int keyBg = 0xFFFFFFFF;
        int keyText = 0xFF202020;

        // Flat key block: fixed white, no theme/shadow treatment.
        context.fill(keyX, keyY, keyX + keyW, keyY + keyH, keyBg);

        int textX = keyX + (keyW - textW) / 2;
        int textY = keyY + 1;
        context.getMatrices().push();
        context.getMatrices().translate(textX, textY, 0);
        context.getMatrices().scale(keyTextScale, keyTextScale, 1.0f);
        context.drawText(tr,
                Text.literal(label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(keyText & 0x00FFFFFF))),
                0, 0, keyText, false);
        context.getMatrices().pop();
    }

    private static String normaliseKeyLabel(String keyLabel) {
        if (keyLabel == null) return "";
        String s = keyLabel.trim();
        if (s.isEmpty()) return "";

        s = s.replace("Keyboard ", "")
             .replace("keyboard.", "")
             .replace("NUMPAD ", "NP")
             .replace("LEFT ", "L")
             .replace("RIGHT ", "R")
             .replace("CONTROL", "CTRL");

        if (s.startsWith("Mouse ")) {
            String tail = s.substring("Mouse ".length()).trim();
            if (!tail.isEmpty()) {
                s = "M" + tail;
            }
        }
        s = s.replace("Button ", "");

        String lower = s.toLowerCase();
        if (lower.equals("left alt") || lower.equals("right alt")
                || lower.equals("lalt") || lower.equals("ralt")
                || lower.equals("alt left") || lower.equals("alt right")) {
            return "Alt";
        }

        if (s.equalsIgnoreCase("Unknown key") || s.equalsIgnoreCase("unknown")) {
            return "";
        }

        if (s.length() > 1 && s.length() > 4) {
            s = s.substring(0, 4);
        }
        return s.toUpperCase();
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

    /**
     * Draws {@code text} with a subtle per-letter jitter to simulate shivering.
     * Offsets are deterministic (sin/cos based), so there is no frame-to-frame random flicker.
     */
    public static void drawShiverText(DrawContext context, TextRenderer tr, String text,
                                      int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int charW = tr.getWidth(ch);

            // Horizontal-biased shiver: side-to-side motion with minimal vertical movement.
            double t = timeMs * 0.016 + i * 1.11;
            int xOffset = (int) Math.round(Math.sin(t) * 0.58 + Math.cos(t * 1.31) * 0.22);
            int yOffset = (int) Math.round(Math.cos(t * 0.72) * 0.10);

            context.drawText(tr,
                    Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                    cursorX + xOffset, y + yOffset, color, true);

            cursorX += charW;
        }
    }

    /**
     * Draws {@code text} with a very subtle, slow all-direction micro-jitter.
     * Intended as a softer alternative to {@link #drawShiverText}.
     */
    public static void drawQuiverText(DrawContext context, TextRenderer tr, String text,
                                      int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int charW = tr.getWidth(ch);

            double t = timeMs * 0.010 + i * 1.37;
            int xOffset = (int) Math.round(Math.sin(t) * 0.45);
            int yOffset = (int) Math.round(Math.cos(t * 0.87) * 0.55);

            context.drawText(tr,
                    Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                    cursorX + xOffset, y + yOffset, color, true);

            cursorX += charW;
        }
    }

    /**
     * Draws {@code text} with a combined breathe + spin + bob transform,
     * mirroring the {@code "breathe_spin_bob"} item animation feel.
     */
    public static void drawBreatheSpinBobText(DrawContext context, TextRenderer tr, String text,
                                              int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        int textW = tr.getWidth(text);
        float centerX = x + (textW / 2.0F);
        float centerY = y + (tr.fontHeight / 2.0F);

        float breatheScale = 1.0F + (float) Math.sin(timeMs * 0.0042) * 0.040F;
        float spinDegrees  = (float) Math.sin(timeMs * 0.0018) * 2.2F;
        float bobOffset    = (float) Math.sin(timeMs * 0.0026 + 1.1) * 0.9F;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY + bobOffset, 0.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinDegrees));
        context.getMatrices().scale(breatheScale, breatheScale, 1.0F);
        context.getMatrices().translate(-centerX, -centerY, 0.0F);

        context.drawText(tr,
                Text.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                x, y, color, true);

        context.getMatrices().pop();
    }

    /**
     * Draws {@code text} where characters drop in from above, bounce briefly,
     * then settle to a static position until the tooltip closes.
     *
     * @param elapsedMs tooltip-lifetime elapsed milliseconds (not absolute wall-clock time)
     */
    public static void drawDropBounceText(DrawContext context, TextRenderer tr, String text,
                                          int x, int y, int color, long elapsedMs) {
        if (text == null || text.isEmpty()) return;

        final long charDelayMs   = 34L;
        final long dropDuration  = 230L;
        final long bounceWindow  = 280L;
        final double dropStartY  = -10.0;

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int charW = tr.getWidth(ch);

            long tChar = elapsedMs - (i * charDelayMs);
            int yOffset;

            if (tChar <= 0L) {
                yOffset = (int) Math.round(dropStartY);
            } else if (tChar < dropDuration) {
                double p = (double) tChar / (double) dropDuration;
                double eased = 1.0 - Math.pow(1.0 - p, 3.0); // ease-out cubic
                yOffset = (int) Math.round(dropStartY * (1.0 - eased));
            } else {
                long tBounce = tChar - dropDuration;
                if (tBounce < bounceWindow) {
                    double osc = Math.sin(tBounce * 0.040) * 1.9;
                    double damp = Math.exp(-tBounce / 120.0);
                    yOffset = (int) Math.round(osc * damp);
                } else {
                    yOffset = 0;
                }
            }

            context.drawText(tr,
                    Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                    cursorX, y + yOffset, color, true);

            cursorX += charW;
        }
    }

    /**
     * Draws text that starts static, then one random non-space character loosens,
     * swings on a hinge, and falls away permanently until tooltip reopen/reset.
     *
     * <p>Use tooltip elapsed time so the sequence is one-shot per tooltip session.
     */
    public static void drawHingeFallText(DrawContext context, TextRenderer tr, String text,
                                         int x, int y, int color, long elapsedMs) {
        if (text == null || text.isEmpty()) return;

        final long introCharDelayMs = 16L;
        final long introDropMs      = 170L;
        final long introImpactMs    = 180L;
        final long introTotalMs     = introDropMs + introImpactMs + (text.length() - 1L) * introCharDelayMs;

        // Reset one-shot state whenever tooltip animation lifetime resets.
        if (elapsedMs < hingeLastElapsedMs || !text.equals(hingeLastText) || hingeTargetIndex < 0) {
            int[] candidates = text.chars()
                    .map(c -> (char) c)
                    .map(ch -> Character.isWhitespace(ch) ? -1 : 1)
                    .toArray();

            int count = 0;
            for (int v : candidates) if (v == 1) count++;

            if (count > 0) {
                int pick = ThreadLocalRandom.current().nextInt(count);
                int seen = 0;
                for (int i = 0; i < candidates.length; i++) {
                    if (candidates[i] == 1) {
                        if (seen == pick) {
                            hingeTargetIndex = i;
                            break;
                        }
                        seen++;
                    }
                }
            } else {
                hingeTargetIndex = -1;
            }

            hingeStartDelayMs = introTotalMs + 430L + ThreadLocalRandom.current().nextLong(280L);
            hingeDirection    = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            hingeDrift        = (float) ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            hingeLastText     = text;
        }

        hingeLastElapsedMs = elapsedMs;

        final long swingMs = 780L;
        final long fallMs  = 980L;

        // Opening slam: all characters drop from above, hit hard, then settle.
        if (elapsedMs < introTotalMs) {
            int cursorXIntro = x;
            for (int i = 0; i < text.length(); i++) {
                String ch = String.valueOf(text.charAt(i));
                int charW = tr.getWidth(ch);

                long tChar = elapsedMs - (i * introCharDelayMs);
                int yOffset;
                if (tChar <= 0L) {
                    yOffset = -28;
                } else if (tChar < introDropMs) {
                    double p = (double) tChar / (double) introDropMs;
                    yOffset = (int) Math.round(-28.0 * (1.0 - (p * p)));
                } else {
                    long tImpact = tChar - introDropMs;
                    if (tImpact < introImpactMs) {
                        double shock = Math.sin(tImpact * 0.22) * 4.2;
                        double damp  = Math.exp(-tImpact / 70.0);
                        yOffset = (int) Math.round(shock * damp);
                    } else {
                        yOffset = 0;
                    }
                }

                context.drawText(tr,
                        Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                        cursorXIntro, y + yOffset, color, true);
                cursorXIntro += charW;
            }
            return;
        }

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int charW = tr.getWidth(ch);

            // Non-target characters remain static for the entire tooltip session.
            if (i != hingeTargetIndex) {
                context.drawText(tr,
                        Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                        cursorX, y, color, true);
                cursorX += charW;
                continue;
            }

            long t = elapsedMs - hingeStartDelayMs;

            // Before loosening, target character is still static.
            if (t <= 0L) {
                context.drawText(tr,
                        Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                        cursorX, y, color, true);
                cursorX += charW;
                continue;
            }

            // Swinging phase: hinged near top-left corner of glyph.
            if (t < swingMs) {
                float p = (float) t / (float) swingMs;
                float amp = 6.0F + (14.0F * p);
                float angle = (float) (Math.sin(p * Math.PI * 3.4) * amp) * hingeDirection;

                context.getMatrices().push();
                float pivotX = cursorX + 0.5F;
                float pivotY = y + 0.5F;
                context.getMatrices().translate(pivotX, pivotY, 0.0F);
                context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
                context.getMatrices().translate(-pivotX, -pivotY, 0.0F);
                context.drawText(tr,
                        Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                        cursorX, y, color, true);
                context.getMatrices().pop();

                cursorX += charW;
                continue;
            }

            // Falling phase: character peels off and drops out of view.
            long tf = t - swingMs;
            if (tf < fallMs) {
                float dx = hingeDrift * (tf * 0.018F);
                float dy = (tf * 0.060F) + (tf * tf * 0.00020F);
                float spin = hingeDirection * (10.0F + (tf * 0.17F));

                float drawX = cursorX + dx;
                float drawY = y + dy;
                float centerX = drawX + (charW / 2.0F);
                float centerY = drawY + (tr.fontHeight / 2.0F);

                context.getMatrices().push();
                context.getMatrices().translate(centerX, centerY, 0.0F);
                context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
                context.getMatrices().translate(-centerX, -centerY, 0.0F);
                context.drawText(tr,
                        Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                        (int) drawX, (int) drawY, color, true);
                context.getMatrices().pop();
            }
            // After fall phase: do not draw this character anymore in current tooltip session.

            cursorX += charW;
        }
    }

    /**
     * Draws text where individual characters randomly toggle between normal and
     * obfuscated rendering on irregular intervals.
     *
     * <p>Uses tooltip elapsed time so state resets when tooltip is reopened.
     */
    public static void drawObfuscateText(DrawContext context, TextRenderer tr, String text,
                                         int x, int y, int color, long elapsedMs) {
        if (text == null || text.isEmpty()) return;

        boolean reset = elapsedMs < obfLastElapsedMs
                || !text.equals(obfLastText)
                || obfNextToggleMs.length != text.length();

        if (reset) {
            obfNextToggleMs = new long[text.length()];
            obfActive = new boolean[text.length()];
            for (int i = 0; i < text.length(); i++) {
                obfActive[i] = false;
                obfNextToggleMs[i] = ThreadLocalRandom.current().nextLong(180L, 820L);
            }
            obfLastText = text;
        }
        obfLastElapsedMs = elapsedMs;

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            String ch = String.valueOf(raw);

            if (!Character.isWhitespace(raw)) {
                while (elapsedMs >= obfNextToggleMs[i]) {
                    obfActive[i] = !obfActive[i];
                    long nextDelta = obfActive[i]
                            ? ThreadLocalRandom.current().nextLong(130L, 340L)
                            : ThreadLocalRandom.current().nextLong(260L, 900L);
                    obfNextToggleMs[i] += nextDelta;
                }
            } else {
                obfActive[i] = false;
            }

            Style style = Style.EMPTY
                    .withColor(TextColor.fromRgb(color & 0x00FFFFFF))
                    .withObfuscated(obfActive[i]);
            context.drawText(tr, Text.literal(ch).setStyle(style), cursorX, y, color, true);
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
