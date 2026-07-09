package net.sweenus.simplytooltips.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.sweenus.simplytooltips.client.render.TabState;
import net.sweenus.simplytooltips.client.render.TooltipGifRecorder;
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
            if (isTextInputScreen(client.currentScreen)) {
                return EventResult.pass();
            }

            if (action == GLFW.GLFW_PRESS
                    && TooltipKeybinds.CAPTURE_GIF.matchesKey(keyCode, scanCode)) {
                TooltipGifRecorder.requestCapture();
                return EventResult.interruptTrue();
            }

            if (TooltipNavigationConfig.tooltipTabs()
                    && action == GLFW.GLFW_PRESS
                    && TooltipKeybinds.CYCLE_TAB.matchesKey(keyCode, scanCode)
                    && TabState.multiTab()) {
                TabState.cycleNext();
                return EventResult.interruptTrue(); // consume — prevent other handlers from seeing G
            }
            return EventResult.pass();
        });

        ClientTickEvent.CLIENT_POST.register(client -> TooltipGifRecorder.tick());
    }

    private static boolean isTextInputScreen(Screen screen) {
        if (screen == null) return false;
        return screen instanceof ChatScreen
                || screen instanceof AbstractSignEditScreen
                || screen instanceof BookEditScreen
                || screen instanceof AnvilScreen
                || isTextInputFocused(screen);
    }

    private static boolean isTextInputFocused(Element element) {
        if (element == null) return false;
        if ((element instanceof TextFieldWidget || element instanceof EditBoxWidget) && element.isFocused()) {
            return true;
        }

        if (element instanceof ParentElement parent) {
            Element focused = parent.getFocused();
            if (focused != null && focused != element && isTextInputFocused(focused)) {
                return true;
            }
            for (Element child : parent.children()) {
                if (child != element && isTextInputFocused(child)) {
                    return true;
                }
            }
        }

        return false;
    }

    private TooltipNavigationEvents() {}
}
