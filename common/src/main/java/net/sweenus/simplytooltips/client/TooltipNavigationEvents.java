package net.sweenus.simplytooltips.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.sweenus.simplytooltips.client.render.ScrollState;
import net.sweenus.simplytooltips.client.render.TabState;

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
        // Mouse scroll — queue a scroll delta when the scrollable tooltip feature is active.
        // Architectury MouseScrolled signature: (MinecraftClient client, double amountX, double amountY)
        ClientRawInputEvent.MOUSE_SCROLLED.register((client, amountX, amountY) -> {
            if (TooltipNavigationConfig.SCROLLABLE_TOOLTIP) {
                ScrollState.queueScroll(amountY);
            }
            return EventResult.pass();
        });

        // Client tick — poll the cycle-tab keybind each tick when the tab feature is active.
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (TooltipNavigationConfig.TOOLTIP_TABS) {
                while (TooltipKeybinds.CYCLE_TAB.wasPressed()) {
                    TabState.cycleNext();
                }
            }
        });
    }

    private TooltipNavigationEvents() {}
}
