package net.sweenus.simplytooltips.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import net.sweenus.simplytooltips.client.render.TooltipRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Unique private static ItemStack simplytooltips$lastRealStack = ItemStack.EMPTY;

    // --- Injection points ---

    @Inject(method = "drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"), cancellable = true)
    private void simplytooltips$drawModernTooltip(TextRenderer textRenderer, ItemStack stack,
                                                  int x, int y, CallbackInfo ci) {
        if (stack == null || stack.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        simplytooltips$lastRealStack = stack;

        List<Text> raw = Screen.getTooltipFromItem(client, stack);
        if (raw == null || raw.isEmpty()) return;

        Optional<TooltipProvider> provider = TooltipProviderRegistry.find(stack);
        if (provider.isEmpty()) return;

        TooltipRenderer.render(
                (DrawContext) (Object) this, textRenderer, stack, raw, provider.get(),
                x, y, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        ci.cancel();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At("HEAD"), cancellable = true)
    private void simplytooltips$drawModernTooltipFromLines(TextRenderer textRenderer, List<Text> text,
                                                           java.util.Optional<?> data,
                                                           int x, int y, CallbackInfo ci) {
        simplytooltips$tryRenderFromLines(textRenderer, text, x, y, ci);
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void simplytooltips$drawModernTooltipFromSimpleLines(TextRenderer textRenderer, List<Text> text,
                                                                  int x, int y, CallbackInfo ci) {
        simplytooltips$tryRenderFromLines(textRenderer, text, x, y, ci);
    }

    // --- Stack-resolution helpers ---

    @Unique
    private void simplytooltips$tryRenderFromLines(TextRenderer textRenderer, List<Text> text,
                                                   int x, int y, CallbackInfo ci) {
        if (text == null || text.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        String title = text.get(0).getString();
        ItemStack resolved = simplytooltips$findRealStack(client, title);
        if (resolved.isEmpty()) return;

        Optional<TooltipProvider> provider = TooltipProviderRegistry.find(resolved);
        if (provider.isEmpty()) return;

        TooltipRenderer.render(
                (DrawContext) (Object) this, textRenderer, resolved, text, provider.get(),
                x, y, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        ci.cancel();
    }

    @Unique
    private static ItemStack simplytooltips$findRealStack(MinecraftClient client, String title) {
        if (title == null || title.isBlank()) return ItemStack.EMPTY;

        // 1. Cached stack from drawItemTooltip
        if (!simplytooltips$lastRealStack.isEmpty()
                && simplytooltips$lastRealStack.getName().getString().equals(title)) {
            return simplytooltips$lastRealStack;
        }

        // 2. Focused slot in a handled screen
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            try {
                java.lang.reflect.Field focusedSlotField = null;
                for (java.lang.reflect.Field f : HandledScreen.class.getDeclaredFields()) {
                    if (f.getType() == Slot.class) {
                        focusedSlotField = f;
                        break;
                    }
                }
                if (focusedSlotField != null) {
                    focusedSlotField.setAccessible(true);
                    Slot slot = (Slot) focusedSlotField.get(handledScreen);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (slotStack.getName().getString().equals(title)) {
                            return slotStack;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Cursor stack
        if (client.player != null) {
            ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty() && cursorStack.getName().getString().equals(title)) {
                return cursorStack;
            }
        }

        // 4. Registry scan (no component data)
        for (Item item : Registries.ITEM) {
            ItemStack candidate = new ItemStack(item);
            if (candidate.getName().getString().equals(title)) {
                return candidate;
            }
        }

        return ItemStack.EMPTY;
    }
}
