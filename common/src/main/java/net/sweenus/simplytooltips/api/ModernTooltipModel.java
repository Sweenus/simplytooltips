package net.sweenus.simplytooltips.api;

import net.minecraft.text.Text;

import java.util.List;

/**
 * The complete data model for a modern tooltip.
 *
 * <p>{@code themeKey} (field 10) is optional ({@code null} = use priority chain).
 * When non-null, {@code TooltipRenderer} will look up a {@code ThemeDefinition} in the
 * {@code ThemeRegistry} to obtain the full theme including motif, animation style, etc.
 *
 * <p>{@code hint} (field 11) is an optional single {@link Text} rendered below the badge row
 * in the header area. Intended for interactive key-hint lines (e.g. "[ALT] [CTRL]").
 *
 * <p>{@link #SECTION_MARKER} can be prepended to {@code abilityLines} entries to request
 * sub-section header rendering (separator above + {@code sectionHeader} colour + "◆ " prefix).
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
        String themeKey,         // nullable; if set, ThemeRegistry overrides theme + borderStyle
        Text hint                // nullable; rendered below badges in the header area
) {
    /**
     * Prefix marker for ability lines that should be rendered as sub-section headers.
     * Lines prefixed with this character are drawn with a separator above and the
     * {@code sectionHeader} colour, matching the style of the "◆ Description" header.
     * Use the Unicode private-use character {@code \uE000} as the sentinel.
     */
    public static final String SECTION_MARKER = "\uE000";
}
