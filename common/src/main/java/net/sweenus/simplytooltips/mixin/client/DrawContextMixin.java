package net.sweenus.simplytooltips.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.tooltip.OrderedTextTooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import net.sweenus.simplytooltips.client.TooltipNavigationConfig;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.TooltipRenderer;
import net.sweenus.simplytooltips.client.tooltip.GenericTooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Unique private static ItemStack simplytooltips$lastRealStack = ItemStack.EMPTY;

    @Unique private static Field simplytooltips$cachedFocusedSlotField = null;
    @Unique private static boolean simplytooltips$focusedSlotFieldResolved = false;
    @Unique private static final Map<String, ItemStack> simplytooltips$nameToStackCache = new HashMap<>();
    @Unique private static Map<String, ItemStack> simplytooltips$itemNameLookup = null;

    /** Stack and raw lines associated with the currently executing vanilla tooltip call chain. */
    @Unique private ItemStack simplytooltips$activeStack = ItemStack.EMPTY;
    @Unique private List<Text> simplytooltips$activeLines = List.of();

    // --- Injection points ---

    @Inject(method = "drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"))
    private void simplytooltips$captureItemTooltipStack(TextRenderer textRenderer, ItemStack stack,
                                                        int x, int y, CallbackInfo ci) {
        if (stack == null || stack.isEmpty()) return;
        simplytooltips$lastRealStack = stack;
        simplytooltips$activeStack = stack;
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At("HEAD"))
    private void simplytooltips$captureTooltipLines(TextRenderer textRenderer, List<Text> text,
                                                    java.util.Optional<?> data,
                                                    int x, int y, CallbackInfo ci) {
        simplytooltips$captureLinesAndResolveStack(text);
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V",
            at = @At("HEAD"), require = 0)
    private void simplytooltips$captureSimpleTooltipLines(TextRenderer textRenderer, List<Text> text,
                                                          int x, int y, CallbackInfo ci) {
        simplytooltips$captureLinesAndResolveStack(text);
    }

    /** Forge overload that carries an explicit stack. Absent on Fabric. */
    @Inject(method = "renderTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"), require = 0)
    private void simplytooltips$captureForgeTooltip(TextRenderer textRenderer, List<Text> text,
                                                    java.util.Optional<?> data, ItemStack stack,
                                                    int x, int y, CallbackInfo ci) {
        if (stack != null && !stack.isEmpty()) {
            simplytooltips$lastRealStack = stack;
            simplytooltips$activeStack = stack;
        }
        simplytooltips$activeLines = text != null ? text : List.of();
    }

    /**
     * Intercept only after vanilla/the loader has converted and gathered tooltip components.
     * This preserves Fabric TooltipData factories and Forge GatherComponents additions.
     */
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V",
            at = @At("HEAD"), cancellable = true)
    private void simplytooltips$drawGatheredTooltip(TextRenderer textRenderer,
                                                    List<TooltipComponent> components,
                                                    int x, int y, TooltipPositioner positioner,
                                                    CallbackInfo ci) {
        if (components == null || components.isEmpty()
                || simplytooltips$activeLines == null || simplytooltips$activeLines.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ItemStack stack = simplytooltips$activeStack;
        if (stack == null || stack.isEmpty()) {
            stack = simplytooltips$findRealStack(client, simplytooltips$activeLines.get(0).getString());
        }
        if (stack.isEmpty()) return;

        Optional<TooltipProvider> provider = TooltipProviderRegistry.find(stack);
        if (provider.isEmpty() || !simplytooltips$shouldRenderFor(stack, provider.get())) return;

        List<TooltipComponent> nativeComponents = components.stream()
                .filter(component -> !(component instanceof OrderedTextTooltipComponent))
                .toList();

        TooltipRenderer.render(
                (DrawContext) (Object) this, textRenderer, stack, simplytooltips$activeLines,
                provider.get(), nativeComponents, x, y,
                client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());

        simplytooltips$clearActiveTooltip();
        ci.cancel();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V",
            at = @At("RETURN"))
    private void simplytooltips$clearGatheredTooltip(TextRenderer textRenderer,
                                                     List<TooltipComponent> components,
                                                     int x, int y, TooltipPositioner positioner,
                                                     CallbackInfo ci) {
        simplytooltips$clearActiveTooltip();
    }

    // --- Stack-resolution helpers ---

    @Unique
    private void simplytooltips$captureLinesAndResolveStack(List<Text> text) {
        if (text == null || text.isEmpty()) return;
        simplytooltips$activeLines = text;

        if (simplytooltips$activeStack != null && !simplytooltips$activeStack.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        simplytooltips$activeStack = simplytooltips$findRealStack(client, text.get(0).getString());
    }

    @Unique
    private void simplytooltips$clearActiveTooltip() {
        simplytooltips$activeStack = ItemStack.EMPTY;
        simplytooltips$activeLines = List.of();
    }

    @Unique
    private static ItemStack simplytooltips$findRealStack(MinecraftClient client, String title) {
        if (title == null || title.isBlank()) return ItemStack.EMPTY;

        if (!simplytooltips$lastRealStack.isEmpty()
                && title.equals(simplytooltips$safeStackName(simplytooltips$lastRealStack))) {
            return simplytooltips$lastRealStack;
        }

        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            try {
                if (!simplytooltips$focusedSlotFieldResolved) {
                    for (Field f : HandledScreen.class.getDeclaredFields()) {
                        if (f.getType() == Slot.class) {
                            f.setAccessible(true);
                            simplytooltips$cachedFocusedSlotField = f;
                            break;
                        }
                    }
                    simplytooltips$focusedSlotFieldResolved = true;
                }
                if (simplytooltips$cachedFocusedSlotField != null) {
                    Slot slot = (Slot) simplytooltips$cachedFocusedSlotField.get(handledScreen);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (title.equals(simplytooltips$safeStackName(slotStack))) {
                            return slotStack;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Cursor / held stack
        if (client.player != null) {
            ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty() && title.equals(simplytooltips$safeStackName(cursorStack))) {
                return cursorStack;
            }
        }

        if (client.currentScreen instanceof MerchantScreen merchantScreen) {
            for (TradeOffer offer : merchantScreen.getScreenHandler().getRecipes()) {
                ItemStack sellItem = offer.getSellItem();
                if (!sellItem.isEmpty() && title.equals(simplytooltips$safeStackName(sellItem))) {
                    return sellItem;
                }
            }
        }

        // Per-title cache covers both positive and negative results.
        if (simplytooltips$nameToStackCache.containsKey(title)) {
            ItemStack cached = simplytooltips$nameToStackCache.get(title);
            return cached != null ? cached : ItemStack.EMPTY;
        }

        // Build the reverse-lookup map lazily on first use (runs once per session).
        if (simplytooltips$itemNameLookup == null) {
            Map<String, ItemStack> lookup = new HashMap<>();
            for (Item item : Registries.ITEM) {
                ItemStack candidate = new ItemStack(item);
                String candidateName = simplytooltips$safeStackName(candidate);
                if (candidateName != null && !candidateName.isBlank()) {
                    lookup.put(candidateName, candidate);
                }
            }
            simplytooltips$itemNameLookup = lookup;
        }

        ItemStack found = simplytooltips$itemNameLookup.getOrDefault(title, ItemStack.EMPTY);
        // Cache the result — including EMPTY — so this title is never looked up again.
        simplytooltips$nameToStackCache.put(title, found);
        return found;
    }

    @Unique
    private static String simplytooltips$safeStackName(ItemStack stack) {
        try {
            return stack != null && !stack.isEmpty() ? stack.getName().getString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static boolean simplytooltips$shouldRenderFor(ItemStack stack, TooltipProvider provider) {
        if (!TooltipNavigationConfig.tooltipRenderingEnabled()) {
            return false;
        }

        if (!ItemThemeRegistry.isEnabledForStack(stack)) {
            return false;
        }

        if (!(provider instanceof GenericTooltipProvider)) {
            return true;
        }

        String namespace = Registries.ITEM.getId(stack.getItem()).getNamespace();
        if ("minecraft".equals(namespace)) {
            return TooltipNavigationConfig.applyTooltipsToVanillaItems();
        }
        if (TooltipNavigationConfig.applyTooltipsToModItems()) {
            return true;
        }
        return ItemThemeRegistry.hasThemeForStack(stack);
    }
}
