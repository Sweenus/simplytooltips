package net.sweenus.simplytooltips.api;

/**
 * One row in the upgrade section of a bow tooltip.
 *
 * @param icon     Unicode icon displayed at the row start, e.g. "◇"
 * @param label    Human-readable row name, e.g. "String"
 * @param pipColor ARGB color for filled pips and the icon
 * @param filled   Current upgrade level (number of filled pips)
 * @param max      Maximum upgrade level (total number of pips)
 * @param altText  Effect description shown when the player holds ALT
 */
public record UpgradeRow(
        String icon,
        String label,
        int pipColor,
        int filled,
        int max,
        String altText
) {}
