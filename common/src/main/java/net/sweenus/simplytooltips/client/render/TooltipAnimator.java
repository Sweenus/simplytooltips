package net.sweenus.simplytooltips.client.render;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.config.SimplyTooltipsConfig;

/**
 * Tracks per-tooltip animation state (key, start time, last-frame time).
 * All state is global-static because only one tooltip is ever visible at a time.
 */
public class TooltipAnimator {

    private static String lastAnimKey         = "";
    private static long   animStartMs         = 0L;
    private static long   animLastFrameMs     = 0L;

    /** Milliseconds of no-tooltip before animation resets (read live from config). */
    private static long resetGapMs() {
        return SimplyTooltipsConfig.INSTANCE.animation.animationResetDelayMs.get();
    }

    /**
     * Updates animation state for the current frame and returns elapsed milliseconds
     * since the animation start for the current tooltip.
     *
     * @param stack         the item stack being tooltipped
     * @param animKeySuffix optional suffix (e.g. upgrade state) that resets animation when it changes;
     *                      may be {@code null}
     */
    public static long updateAndGetElapsed(ItemStack stack, String animKeySuffix) {
        long now = System.currentTimeMillis();
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String key = itemId == null ? "unknown" : itemId.toString();
        if (animKeySuffix != null && !animKeySuffix.isEmpty()) key += animKeySuffix;

        boolean keyChanged = !key.equals(lastAnimKey);
        boolean timedOut   = (now - animLastFrameMs) > resetGapMs();
        if (keyChanged || timedOut) {
            animStartMs   = now;
            lastAnimKey   = key;
        }
        animLastFrameMs = now;
        return Math.max(0L, now - animStartMs);
    }

    private TooltipAnimator() {}
}
