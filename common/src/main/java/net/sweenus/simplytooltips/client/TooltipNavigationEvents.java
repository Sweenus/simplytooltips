package net.sweenus.simplytooltips.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.sweenus.simplytooltips.client.render.TabState;
import org.lwjgl.glfw.GLFW;

/**
 * Registers cross-platform (Architectury) input and tick events for tooltip navigation.
 * Called from both {@code SimplyTooltipsFabricClient} and {@code SimplyTooltipsNeoForgeClient}.
 */
public class TooltipNavigationEvents {

    /**
     * Registers all navigation event listeners.
     * Must be called during client initialisation on both platforms.
     */
    public static void register() {
        // Mouse scroll is handled by ScreenScrollMixin, which intercepts Screen#mouseScrolled
        // directly. This is more reliable than Architectury's MOUSE_SCROLLED event for the
        // inventory-screen context on Fabric.

        // Raw key press — fires at GLFW level before the screen handles input, so it works even
        // when an inventory screen is open (wasPressed() only fires when currentScreen == null).
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (TooltipNavigationConfig.tooltipTabs()
                    && action == GLFW.GLFW_PRESS
                    && TooltipKeybinds.CYCLE_TAB.matchesKey(keyCode, scanCode)
                    && TabState.multiTab()) {
                TabState.cycleNext();
                return EventResult.interruptTrue(); // consume — prevent other handlers from seeing G
            }
            return EventResult.pass();
        });
    }

    private TooltipNavigationEvents() {}
}
