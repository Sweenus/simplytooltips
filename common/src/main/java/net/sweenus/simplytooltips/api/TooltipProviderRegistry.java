package net.sweenus.simplytooltips.api;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class TooltipProviderRegistry {
    private static final List<Entry> PROVIDERS = new ArrayList<>();

    private TooltipProviderRegistry() {
    }

    public static void register(TooltipProvider provider, int priority) {
        PROVIDERS.add(new Entry(provider, priority));
        PROVIDERS.sort(Comparator.comparingInt(Entry::priority).reversed());
    }

    public static Optional<TooltipProvider> find(ItemStack stack) {
        for (Entry entry : PROVIDERS) {
            if (entry.provider().supports(stack)) {
                return Optional.of(entry.provider());
            }
        }
        return Optional.empty();
    }

    private record Entry(TooltipProvider provider, int priority) {
    }
}
