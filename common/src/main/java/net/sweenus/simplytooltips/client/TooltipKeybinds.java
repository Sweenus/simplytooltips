package net.sweenus.simplytooltips.client;

import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinding definitions for tooltip navigation.
 * Registration with each platform is handled in the respective platform client entrypoints.
 */
public class TooltipKeybinds {

    public static final String CATEGORY = "simplytooltips.key.category";

    /**
     * Cycles to the next content tab while a tooltip is displayed.
     * Default key: {@code G}.
     */
    public static final KeyBinding CYCLE_TAB = new KeyBinding(
            "key.simplytooltips.cycle_tab",
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    private TooltipKeybinds() {}
}
