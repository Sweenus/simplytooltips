package net.sweenus.simplytooltips.api;

import java.util.List;

/**
 * The full upgrade block displayed in a bow tooltip.
 *
 * <p>The Slots pip row is rendered by the engine directly from
 * {@link #totalSlots()} and {@link #usedSlots()}. The {@link #rows()} list
 * contains only the variable upgrade rows (String, Frame, etc.).
 *
 * @param totalSlots Total combined upgrade slots (used for the Slots pip row)
 * @param usedSlots  Number of currently occupied upgrade slots
 * @param rows       Variable upgrade rows, e.g. String and Frame rows
 * @param rune       Rune etching data to display below the upgrade rows
 */
public record UpgradeSection(
        int totalSlots,
        int usedSlots,
        List<UpgradeRow> rows,
        UpgradeRune rune
) {}
