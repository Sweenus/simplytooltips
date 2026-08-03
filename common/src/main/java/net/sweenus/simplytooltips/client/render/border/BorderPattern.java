package net.sweenus.simplytooltips.client.render.border;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.BorderPalette;
import net.sweenus.simplytooltips.api.TooltipTheme;

/**
 * Draws the decorative pixel art along a tooltip's top and bottom border lines.
 *
 * <p>Implementations are registered by key in
 * {@link net.sweenus.simplytooltips.client.render.BorderRegistry} and selected by a theme's
 * {@code border} field or a {@code borders/*.json} definition. The plain outline, inner highlight
 * and corner diamonds are drawn by {@link net.sweenus.simplytooltips.client.render.BorderRenderer}
 * before this runs — a pattern only adds decoration on top.
 */
public interface BorderPattern {

    /**
     * Draws decoration for a panel of size {@code w × h} at {@code (x, y)}.
     *
     * @param palette fully resolved colours: this pattern's {@link #defaultPalette} with any
     *                overrides from the border definition already layered on top.
     */
    void draw(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, BorderPalette palette);

    /**
     * The colours this pattern draws with when a border definition does not override them.
     *
     * @param theme the resolved tooltip theme, for patterns that derive their colours from it
     *              (see {@link EarthBorder}); most implementations ignore it and return constants.
     */
    BorderPalette defaultPalette(TooltipTheme theme);
}
