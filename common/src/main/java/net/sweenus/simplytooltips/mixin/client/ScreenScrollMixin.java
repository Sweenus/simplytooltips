package net.sweenus.simplytooltips.mixin.client;

import net.minecraft.client.Mouse;
import net.sweenus.simplytooltips.client.TooltipNavigationConfig;
import net.sweenus.simplytooltips.client.render.ScrollState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the raw GLFW scroll callback in {@link Mouse} so that when a scrollable tooltip
 * is active, the scroll delta is routed to {@link ScrollState} and the event is consumed
 * (preventing hotbar slot changes or screen scroll from firing simultaneously).
 *
 * <p>Targets {@code Mouse#onMouseScroll(JDD)V} (Yarn name in MC 1.21.1; intermediary: {@code method_1598}).
 * Targeting {@code Screen#mouseScrolled} crashes because Screen does not directly declare that
 * method — it only inherits it from the {@code Element} interface.
 */
@Mixin(Mouse.class)
public abstract class ScreenScrollMixin {

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void simplytooltips$onMouseScrolled(long window, double horizontal, double vertical,
            CallbackInfo ci) {
        if (TooltipNavigationConfig.scrollableTooltip() && ScrollState.isScrollableActive()) {
            ScrollState.queueScroll(vertical);
            ci.cancel(); // prevent MC from accumulating this delta for screen/hotbar dispatch
        }
    }
}
