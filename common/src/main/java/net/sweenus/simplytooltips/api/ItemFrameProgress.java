package net.sweenus.simplytooltips.api;

import net.minecraft.text.Text;

/**
 * Optional progress information rendered immediately outside the tooltip item frame.
 *
 * @param value         current progress value
 * @param maxValue      maximum progress value
 * @param progressColor ARGB colour used for the completed frame perimeter
 * @param glowColor     legacy-named ARGB colour used for the in-line travelling highlight
 * @param levelLabel    label drawn beside the top-right of the item frame
 */
public record ItemFrameProgress(
        int value,
        int maxValue,
        int progressColor,
        int glowColor,
        Text levelLabel
) {
    public ItemFrameProgress {
        maxValue = Math.max(1, maxValue);
        value = Math.clamp(value, 0, maxValue);
        levelLabel = levelLabel == null ? Text.literal(Integer.toString(value)) : levelLabel;
    }

    public float fraction() {
        return value / (float) maxValue;
    }

    /** Preferred semantic accessor for the in-line travelling highlight colour. */
    public int highlightColor() {
        return glowColor;
    }
}
