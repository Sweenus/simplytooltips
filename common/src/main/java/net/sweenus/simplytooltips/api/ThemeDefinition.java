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
         * <ul>
         *   <li>{@code "breathe_spin_bob"} — scale pulse + pendulum spin + vertical bob (default)
         *   <li>{@code "spin"}    — slow continuous rotation only
         *   <li>{@code "bob"}     — gentle vertical bob only
         *   <li>{@code "breathe"} — scale pulse only
         *   <li>{@code "static"}  — no animation
         * </ul>
         */
        String itemAnimStyle,

        /**
         * Animation style for the title text.
         * <ul>
         *   <li>{@code "wave"}    — travelling vertical wave (default)
         *   <li>{@code "shimmer"} — brightness glint sweeps across letters
         *   <li>{@code "pulse"}   — whole title brightens and dims on a slow cycle
         *   <li>{@code "flicker"} — subtle irregular fire-light brightness variation
         *   <li>{@code "shiver"}  — per-letter jitter / shivering effect
         *   <li>{@code "quiver"}  — very subtle slow micro-jitter (legacy subtle shiver look)
         *   <li>{@code "breathe_spin_bob"} — combined scale pulse + gentle tilt + vertical bob
         *   <li>{@code "static"}  — no animation, plain text
         * </ul>
         */
        String titleAnimStyle,

        /**
         * Shape of the frame drawn around the item icon.
         * <ul>
         *   <li>{@code "diamond"} — rotated diamond (default)
         *   <li>{@code "square"}  — beveled square
         *   <li>{@code "circle"}  — pixel circle
         *   <li>{@code "cross"}   — plus / cross shape
         *   <li>{@code "none"}    — no frame
         * </ul>
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
