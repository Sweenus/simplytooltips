package net.sweenus.simplytooltips.client.tooltip;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Compat provider that renders Simply Swords-structured items from addon mods using
 * the full Simply Swords tooltip pipeline: ability section (LORE tab), mainhand
 * attribute block (STATS tab with stat bars), and button-hint extraction.
 *
 * <p>Activated by tagging items in
 * {@code data/simplytooltips/tags/item/simply_swords_compat.json}.
 * Badges and themes are intentionally left to the data-driven
 * {@code item_themes/*.json} system rather than being auto-detected here.
 *
 * <p>Registered at priority {@code 1}, above {@link GenericTooltipProvider} ({@code 0}),
 * so that any mod-specific provider registered at a higher priority takes precedence.
 */
public final class SimplySwordsCompatTooltipProvider implements TooltipProvider {

    public static final TagKey<Item> SIMPLY_SWORDS_COMPAT_TAG = TagKey.of(
            RegistryKeys.ITEM, Identifier.of("simplytooltips", "simply_swords_compat")
    );

    /**
     * Action-label prefixes that are rendered as sub-section headers inside the LORE tab.
     * Matching is case-insensitive and substring-based, because SS prepends a custom glyph
     * icon before the human-readable text (e.g. "\uAB40 On Right Click: ").
     */
    private static final String[] SECTION_HEADER_PREFIXES = {
            "on right click",
            "hold right click",
            "on left click",
            "hold left click",
            "on sneak",
    };

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isIn(SIMPLY_SWORDS_COMPAT_TAG);
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String title = rawLines.isEmpty() ? stack.getName().getString() : rawLines.get(0).getString();

        List<String> abilityLines = parseAbilityLines(rawLines);
        List<Text>   extraLines   = parseExtraLines(rawLines);
        Text         hint         = parseHintLine(rawLines);

        // Badges and themeKey are left null/empty so that item_themes/*.json data takes
        // full control. If no data entry exists the renderer falls back to rarity.
        return new ModernTooltipModel(
                title,
                List.of(),
                TooltipBorderStyle.DEFAULT,
                abilityLines,
                List.of(),
                extraLines,
                TooltipTheme.defaultTheme(),
                null,
                null,
                null,
                hint
        );
    }

    // --- Parsing helpers ---

    /**
     * Collects ability description lines from rawLines[1..N].
     * Stops at the vanilla attribute block header ("When in Main Hand" / "When in Off Hand").
     * Skips button-hint lines; preserves internal blank lines as paragraph separators.
     * Action-label lines ("On Right Click:", etc.) are prefixed with
     * {@link ModernTooltipModel#SECTION_MARKER} so the renderer draws them as sub-section headers.
     */
    private static List<String> parseAbilityLines(List<Text> rawLines) {
        List<String> result = new ArrayList<>();
        String attrMain = Text.translatable("item.modifiers.mainhand").getString();
        String attrOff  = Text.translatable("item.modifiers.offhand").getString();

        boolean seenContent = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
            if (s.equals(attrMain) || s.equals(attrOff)) break;
            if (isButtonHint(s)) continue;

            if (s.isBlank()) {
                if (seenContent) result.add("");  // blank line between paragraphs → visual gap
            } else if (isSectionHeader(s)) {
                // Consume the preceding blank so the renderer's separator replaces it
                if (!result.isEmpty() && result.get(result.size() - 1).isBlank()) {
                    result.remove(result.size() - 1);
                }
                result.add(ModernTooltipModel.SECTION_MARKER + s);
                seenContent = true;
            } else {
                seenContent = true;
                result.add(s);
            }
        }

        // Trim trailing blank lines
        while (!result.isEmpty() && result.get(result.size() - 1).isBlank()) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    /**
     * Collects vanilla attribute {@link Text} lines (the mainhand attack damage / speed / range
     * block). These populate the STATS tab and drive the stat-bar renderer.
     */
    private static List<Text> parseExtraLines(List<Text> rawLines) {
        List<Text> result = new ArrayList<>();
        String attrMain = Text.translatable("item.modifiers.mainhand").getString();
        String attrOff  = Text.translatable("item.modifiers.offhand").getString();
        boolean inAttr = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
            if (s.equals(attrMain) || s.equals(attrOff)) inAttr = true;
            if (inAttr) result.add(rawLines.get(i));
        }
        return result;
    }

    /**
     * Returns the first button-hint {@link Text} found in rawLines (preserving styled colours),
     * or {@code null} if none exists. The hint is shown below the badge row in the header area.
     *
     * <p>Button-hint rows consist of three NBSP-separated segments (info / search / config glyphs).
     */
    private static Text parseHintLine(List<Text> rawLines) {
        for (int i = 1; i < rawLines.size(); i++) {
            if (isButtonHint(rawLines.get(i).getString())) {
                return rawLines.get(i);
            }
        }
        return null;
    }

    /**
     * Detects the interactive tooltip button row added by {@code TooltipUtils.addDynamicButtonTooltip()}.
     * Requires at least three non-blank NBSP-separated segments to avoid false-matching
     * ability lines that use NBSP for indentation.
     */
    private static boolean isButtonHint(String s) {
        if (s == null || s.isBlank()) return false;
        String[] parts = s.split("\u00A0+");
        int nonBlankParts = 0;
        for (String part : parts) {
            if (!part.isBlank()) nonBlankParts++;
        }
        return nonBlankParts >= 3;
    }

    /**
     * Returns {@code true} if this line should be rendered as a sub-section header.
     * Matches known action-label prefixes case-insensitively anywhere in the string.
     */
    private static boolean isSectionHeader(String s) {
        String lower = s.toLowerCase();
        for (String prefix : SECTION_HEADER_PREFIXES) {
            if (lower.contains(prefix)) return true;
        }
        return false;
    }
}
