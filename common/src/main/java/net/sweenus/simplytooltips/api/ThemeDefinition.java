package net.sweenus.simplytooltips.api;

import java.util.List;

/**
 * Full data-driven theme definition: colour palette plus visual behaviour settings.
 * Instances are loaded from {@code assets/simplytooltips/themes/<key>.json}.
 *
 * <p>Defaults (used when a JSON field is absent):
 * <ul>
 *   <li>{@code motif} = {@code "none"} (no background particles)
 *   <li>{@code itemAnimStyle} = {@code "breathe_spin_bob"}
 *   <li>{@code titleAnimStyle} = {@code "wave"}
 *   <li>{@code itemBorderShape} = {@code "diamond"}
 *   <li>{@code customTextKeys} = empty list
 * </ul>
 */
public record ThemeDefinition(
        /** The 19-color palette. */
        TooltipTheme colors,

        /** Key into {@code MotifRegistry}, e.g. {@code "vine"} or {@code "none"} for no motif. */
        String motif,

        /**
         * Animation style for the item icon in the header.
         * Recognised values: {@code "breathe_spin_bob"}, {@code "static"}.
         */
        String itemAnimStyle,

        /**
         * Animation style for the title text.
         * Recognised values: {@code "wave"}, {@code "static"}.
         */
        String titleAnimStyle,

        /**
         * Shape of the frame around the item icon.
         * Currently only {@code "diamond"} is rendered; reserved for future expansion.
         */
        String itemBorderShape,

        /**
         * Translation key strings resolved via {@code Text.translatable(key).getString()} at
         * render time and inserted as extra lines after the body section.
         */
        List<String> customTextKeys
) {
    /** Default definition matching the legacy golden theme behaviour. */
    public static ThemeDefinition defaultDefinition() {
        return new ThemeDefinition(
                TooltipTheme.defaultTheme(),
                "none",
                "breathe_spin_bob",
                "wave",
                "diamond",
                List.of()
        );
    }
}
