package net.sweenus.simplytooltips.api;

import net.minecraft.item.ItemStack;

import java.util.Objects;

/** One item and stable output stem in a batch tooltip export. */
public record TooltipExportEntry(String outputName, ItemStack stack) {
    public TooltipExportEntry {
        Objects.requireNonNull(outputName, "outputName");
        Objects.requireNonNull(stack, "stack");
        if (outputName.isBlank() || !outputName.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid tooltip export name: " + outputName);
        }
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Tooltip export stack cannot be empty");
        }
        stack = stack.copy();
    }
}
