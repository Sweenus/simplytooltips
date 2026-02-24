package net.sweenus.simplytooltips.api;

import net.minecraft.text.Text;

import java.util.List;

/**
 * The complete data model for a modern tooltip.
 *
 * <p>{@code themeKey} (field 10) is optional ({@code null} = use {@code theme} directly).
 * When non-null, {@code TooltipRenderer} will look up a {@code ThemeDefinition} in the
 * {@code ThemeRegistry} to obtain the full theme including motif, animation style, etc.
 */
public record ModernTooltipModel(
        String title,
        List<String> badges,
        int borderStyle,
        List<String> abilityLines,
        List<String> bodyLines,
        List<Text> extraLines,
        TooltipTheme theme,
        UpgradeSection upgradeSection,
        String animKeyExtra,
        String themeKey          // nullable; if set, ThemeRegistry overrides theme + borderStyle
) {}
