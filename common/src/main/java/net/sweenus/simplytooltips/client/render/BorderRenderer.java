package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderDefinition;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipTheme;
import net.sweenus.simplytooltips.client.render.border.BorderPattern;

/**
 * Draws the decorative border frame and delegates its decoration to a {@link BorderPattern}.
 *
 * <p>The frame itself (outer lines, inner highlight, corner diamonds) is drawn here in the theme's
 * border colours, unless the {@link BorderDefinition} overrides them. The pattern on top comes from
 * {@link BorderRegistry}, so both built-in and mod-registered borders go through the same path.
 */
public class BorderRenderer {

    /**
     * Draws the outer border lines, inner highlight lines, corner diamonds, and the decorative
     * pattern named by {@code definition}.
     */
    public static void drawDecorativeBorder(DrawContext context, int x, int y, int w, int h,
                                            TooltipTheme theme, BorderDefinition definition) {
        BorderDefinition def = definition != null ? definition : BorderDefinition.none();
        BorderPattern pattern = BorderRegistry.get(def.pattern());

        BorderPalette palette = pattern != null
                ? def.palette().over(pattern.defaultPalette(theme))
                : def.palette();

        int frame      = palette.frameOr(theme.border());
        int frameInner = palette.frameInnerOr(theme.borderInner());

        // Outer border lines
        context.fill(x, y, x + w, y + 1, frame);
        context.fill(x, y + h - 1, x + w, y + h, frame);
        context.fill(x, y, x + 1, y + h, frame);
        context.fill(x + w - 1, y, x + w, y + h, frame);

        // Inner highlight lines
        context.fill(x + 1, y + 1, x + w - 1, y + 2, frameInner);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, frameInner);
        context.fill(x + 1, y + 1, x + 2, y + h - 1, frameInner);
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, frameInner);

        // Corner diamonds
        drawSmallDiamond(context, x + 6,     y,         frame);
        drawSmallDiamond(context, x + w - 7, y,         frame);
        drawSmallDiamond(context, x + 6,     y + h - 1, frame);
        drawSmallDiamond(context, x + w - 7, y + h - 1, frame);

        if (pattern != null) {
            pattern.draw(context, x, y, w, h, theme, palette);
        }
    }

    /**
     * @deprecated Borders are keyed by string now — pass a {@link BorderDefinition} instead. Retained
     *             so callers written against the old {@link TooltipBorderStyle} constants keep working.
     */
    @Deprecated
    public static void drawDecorativeBorder(DrawContext context, int x, int y, int w, int h,
                                            TooltipTheme theme, int borderStyle) {
        drawDecorativeBorder(context, x, y, w, h, theme,
                BorderDefinition.ofPattern(TooltipBorderStyle.keyOf(borderStyle)));
    }

    /**
     * Draws only the decorative pattern for {@code borderStyle}, without the surrounding frame.
     *
     * @deprecated Use {@link BorderRegistry#get(String)} and call the pattern directly.
     */
    @Deprecated
    public static void drawBorderPattern(DrawContext context, int x, int y, int w, int h,
                                         TooltipTheme theme, int borderStyle) {
        BorderPattern pattern = BorderRegistry.get(TooltipBorderStyle.keyOf(borderStyle));
        if (pattern != null) {
            pattern.draw(context, x, y, w, h, theme, pattern.defaultPalette(theme));
        }
    }

    /** 3×3 diamond centered at (cx, cy). */
    public static void drawSmallDiamond(DrawContext context, int cx, int cy, int color) {
        context.fill(cx, cy - 1, cx + 1, cy, color);
        context.fill(cx - 1, cy, cx + 2, cy + 1, color);
        context.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    private BorderRenderer() {}
}
