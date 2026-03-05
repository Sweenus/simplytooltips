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
 * <p>{@code affixLines} (field 12) is an optional list of lines for the dedicated AFFIXES tab.
 * When non-null and non-empty, a separate AFFIXES tab is shown. Lines may be prefixed with
 * {@link #SECTION_MARKER} for sub-section headers (e.g. "Affixes", "Sockets").
 * If {@code null}, no AFFIXES tab is added.
 *
 * <p>{@link #SECTION_MARKER} can be prepended to {@code abilityLines} or {@code affixLines}
 * entries to request sub-section header rendering (separator above + {@code sectionHeader}
 * colour + "◆ " prefix).
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
        String themeKey,        // nullable; if set, ThemeRegistry overrides theme + borderStyle
        Text hint,              // nullable; rendered below badges in the header area
        List<String> affixLines // nullable; if non-empty, adds a dedicated AFFIXES tab
) {
    /**
     * Prefix marker for ability/affix lines that should be rendered as sub-section headers.
     * Lines prefixed with this character are drawn with a separator above and the
     * {@code sectionHeader} colour, matching the style of the "◆ Description" header.
     * Use the Unicode private-use character {@code \uE000} as the sentinel.
     */
    public static final String SECTION_MARKER = "\uE000";

    /**
     * Sentinel for a subtle horizontal divider drawn between consecutive affix entries
     * in the AFFIXES tab. Renders as a thin 1 px line at reduced opacity — visually
     * lighter than the full {@code drawSeparator()} line used between sections.
     * Use the Unicode private-use character {@code \uE001} as the sentinel.
     */
    public static final String AFFIX_DIVIDER = "\uE001";

    /**
     * Legacy 11-parameter constructor for backward compatibility.
     * Delegates to the canonical constructor with {@code affixLines = null}.
     * Existing providers (e.g. SimplyBowsTooltipProvider) that were compiled against the
     * previous 11-field record continue to work without modification.
     */
    public ModernTooltipModel(String title, List<String> badges, int borderStyle,
                               List<String> abilityLines, List<String> bodyLines,
                               List<Text> extraLines, TooltipTheme theme,
                               UpgradeSection upgradeSection, String animKeyExtra,
                               String themeKey, Text hint) {
        this(title, badges, borderStyle, abilityLines, bodyLines, extraLines, theme,
                upgradeSection, animKeyExtra, themeKey, hint, null);
    }
}
