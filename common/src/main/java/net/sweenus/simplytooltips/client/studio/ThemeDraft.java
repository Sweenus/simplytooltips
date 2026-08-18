package net.sweenus.simplytooltips.client.studio;

import net.sweenus.simplytooltips.api.ThemeDefinition;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * A theme being edited in the Theme Studio.
 *
 * <p>Wraps the immutable {@link ThemeDefinition} in something mutable, and rebuilds the definition
 * lazily. Each rebuild yields a <em>new</em> record instance, which is what lets the renderer's
 * model cache notice an edit - it compares the forced definition by identity.
 */
public final class ThemeDraft {

    private String sourceKey;
    private ThemeDefinition original;

    private TooltipTheme colors;
    private String motif;
    private String borderStyle;
    private String itemAnimStyle;
    private String titleAnimStyle;
    private String itemBorderShape;
    private final List<String> customTextKeys = new ArrayList<>();

    private ThemeDefinition cached;

    public ThemeDraft(String key, ThemeDefinition def) {
        load(key, def);
    }

    /** Replaces everything in the buffer with another theme, discarding unsaved edits. */
    public void load(String key, ThemeDefinition def) {
        this.sourceKey = key;
        this.original = def;
        this.colors = def.colors();
        this.motif = def.motif();
        this.borderStyle = def.border();
        this.itemAnimStyle = def.itemAnimStyle();
        this.titleAnimStyle = def.titleAnimStyle();
        this.itemBorderShape = def.itemBorderShape();
        this.customTextKeys.clear();
        this.customTextKeys.addAll(def.customTextKeys());
        this.cached = null;
    }

    /** Throws away edits and returns to the theme as it was loaded. */
    public void revert() {
        load(sourceKey, original);
    }

    public ThemeDefinition toDefinition() {
        if (cached == null) {
            cached = new ThemeDefinition(colors, motif, borderStyle, itemAnimStyle,
                    titleAnimStyle, itemBorderShape, List.copyOf(customTextKeys));
        }
        return cached;
    }

    public boolean isDirty() {
        return !toDefinition().equals(original);
    }

    public String sourceKey() {
        return sourceKey;
    }

    /** Marks the current buffer as the saved state, so it stops reading as dirty. */
    public void markSaved(String key) {
        this.sourceKey = key;
        this.original = toDefinition();
    }

    // ---- accessors ----------------------------------------------------------------------------

    public TooltipTheme colors() {
        return colors;
    }

    public int color(String key) {
        return ThemeJson.colorOf(colors, key);
    }

    public void setColor(String key, int argb) {
        colors = ThemeJson.withColor(colors, key, argb);
        cached = null;
    }

    public boolean textShadow() {
        return colors.textShadow();
    }

    public void setTextShadow(boolean shadow) {
        colors = ThemeJson.withTextShadow(colors, shadow);
        cached = null;
    }

    public String motif() {
        return motif;
    }

    public void setMotif(String value) {
        motif = value;
        cached = null;
    }

    public String borderStyle() {
        return borderStyle;
    }

    public void setBorderStyle(String value) {
        borderStyle = value;
        cached = null;
    }

    public String itemAnimStyle() {
        return itemAnimStyle;
    }

    public void setItemAnimStyle(String value) {
        itemAnimStyle = value;
        cached = null;
    }

    public String titleAnimStyle() {
        return titleAnimStyle;
    }

    public void setTitleAnimStyle(String value) {
        titleAnimStyle = value;
        cached = null;
    }

    public String itemBorderShape() {
        return itemBorderShape;
    }

    public void setItemBorderShape(String value) {
        itemBorderShape = value;
        cached = null;
    }

    public List<String> customTextKeys() {
        return customTextKeys;
    }

    public void setCustomTextKeys(List<String> keys) {
        customTextKeys.clear();
        customTextKeys.addAll(keys);
        cached = null;
    }
}
