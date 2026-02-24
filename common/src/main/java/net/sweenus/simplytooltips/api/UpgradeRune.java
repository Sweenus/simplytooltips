package net.sweenus.simplytooltips.api;

import java.util.List;

/**
 * Rune etching display data for a bow tooltip.
 *
 * @param runeName    Display name of the rune, e.g. "Pain" or "None"
 * @param isNone      If {@code true}, the rune row is rendered dimly (no etching)
 * @param runeColor   ARGB color for the rune icon and name
 * @param effectLines Lines describing the rune's effect (pre-indented, e.g. "  +20% damage")
 */
public record UpgradeRune(
        String runeName,
        boolean isNone,
        int runeColor,
        List<String> effectLines
) {}
