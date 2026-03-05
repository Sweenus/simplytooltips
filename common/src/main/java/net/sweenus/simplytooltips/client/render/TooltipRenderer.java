package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplytooltips.api.*;
import net.sweenus.simplytooltips.client.TooltipKeybinds;
import net.sweenus.simplytooltips.client.TooltipNavigationConfig;
import net.sweenus.simplytooltips.client.render.motif.BackgroundMotif;
import net.sweenus.simplytooltips.client.tooltip.ApotheosisCompat;
import net.sweenus.simplytooltips.client.tooltip.SimplySwordsCompatTooltipProvider;
import net.sweenus.simplytooltips.config.SimplyTooltipsConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main tooltip rendering pipeline.
 * Orchestrates layout calculations and delegates all drawing to the specialised helper classes.
 */
public class TooltipRenderer {

    private static final Pattern INLINE_STAT_PATTERN = Pattern.compile(
            "^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s+(Attack Damage|Attack Speed|Attack Range|Entity Interaction Range)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNIQUE_EFFECT_PATTERN = Pattern.compile(
            "(?i)^.*?unique\\s+effect\\s*:\\s*(.+?)\\s*$"
    );
    private static final String DEFAULT_ABILITY_HEADER = "Description";
    // Tag used to drive the full Simply Swords rendering pipeline (ability header extraction,
    // LORE tab, STATS tab with stat bars). Defined on the provider; referenced here for
    // prepareAbilitySection. See SimplySwordsCompatTooltipProvider for full details.
    private static final TagKey<Item> SIMPLY_SWORDS_COMPAT_TAG =
            SimplySwordsCompatTooltipProvider.SIMPLY_SWORDS_COMPAT_TAG;
    private static final String STAT_LABEL_REFERENCE = "Attack Damage";
    private static final int STAT_BAR_MIN_WIDTH = 52;
    private static final int STAT_BAR_HEIGHT = 4;

    /** Extra vertical gap added after each body / extra / affix content line (not section headers). */
    private static final int BODY_LINE_EXTRA_GAP = 2;

    /** Height consumed by an {@link ModernTooltipModel#AFFIX_DIVIDER} sentinel in the AFFIXES tab. */
    private static final int AFFIX_DIVIDER_H = 7;

    /**
     * Minimum scale at which the title is rendered before switching to truncation with "...".
     * Below this threshold the text would be unreadably small, so the string is clipped instead.
     */
    private static final float MIN_TITLE_SCALE = 0.6f;

    /** Regex used to split a body-line string into numeric and non-numeric segments for colour highlighting. */
    private static final Pattern NUMBER_SEGMENT_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    private static final int AFFIX_POSITIVE_COLOR = 0xFF6FCB63;
    private static final int AFFIX_NEGATIVE_COLOR = 0xFFD05A4A;
    private static final int AFFIX_NEUTRAL_COLOR  = 0xFFB8742F;
    private static volatile List<EffectNameEntry> STATUS_EFFECT_ENTRIES;


    private static int padding()      { return SimplyTooltipsConfig.INSTANCE.layout.padding.get(); }
    private static int lineSpacing()  { return SimplyTooltipsConfig.INSTANCE.layout.lineSpacing.get(); }
    private static int maxTextWidth() { return SimplyTooltipsConfig.INSTANCE.layout.maxTextWidth.get(); }
    /** Hard cap on body viewport height before scroll activates (~17 lines at default). */
    private static int maxBodyH()     { return SimplyTooltipsConfig.INSTANCE.layout.maxBodyHeight.get(); }

    /**
     * Renders a full modern tooltip for the given item stack.
     *
     * @param context  the draw context
     * @param tr       text renderer
     * @param stack    the item being tooltipped
     * @param rawLines raw vanilla tooltip lines (used by the provider to build the model)
     * @param provider the provider that handles this stack
     * @param x        mouse/item x position
     * @param y        mouse/item y position
     * @param screenW  scaled screen width
     * @param screenH  scaled screen height
     */
    public static void render(DrawContext context, TextRenderer tr, ItemStack stack, List<Text> rawLines,
                              TooltipProvider provider, int x, int y, int screenW, int screenH) {

        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, 400.0f);

        boolean altDown = false;
        try { altDown = Screen.hasAltDown(); } catch (Throwable ignored) {}

        ModernTooltipModel model = provider.build(stack, rawLines, altDown);
        // Post-process: move Apotheosis affix/imbue lines to the STATS tab bodyLines
        // under a dedicated "Affixes" section header, regardless of which provider ran.
        model = ApotheosisCompat.augment(model, rawLines, stack, altDown);

        // Resolve theme via priority chain: provider key > item/tag data > rarity
        ThemeDefinition resolvedDef      = resolveTheme(stack, model);
        final TooltipTheme theme         = resolvedDef.colors();
        final String       motifStr      = resolvedDef.motif();
        final int          borderStyle   = borderStyleFor(motifStr);
        final String resolvedMotifKey    = "none".equals(motifStr) ? null : motifStr;
        final String itemAnimStyle       = resolvedDef.itemAnimStyle();
        final String titleAnimStyle      = resolvedDef.titleAnimStyle();
        final String itemBorderShape     = resolvedDef.itemBorderShape();

        // Data-driven badge override: item_themes JSON can replace the provider's default badges
        List<String> dataBadges = ItemThemeRegistry.resolveBadgesForStack(stack);
        List<String> badges = dataBadges != null ? dataBadges : model.badges();

        long tooltipElapsedMs = TooltipAnimator.updateAndGetElapsed(stack, model.animKeyExtra());

        // ---- Text wrapping ----
        AbilitySectionData abilitySection = prepareAbilitySection(stack, model.abilityLines());
        List<String> wrappedAbility  = TooltipPainter.wrapStrings(abilitySection.lines(), tr, maxTextWidth());
        List<String> wrappedCustom   = wrapCustomTextKeys(resolvedDef.customTextKeys(), tr, maxTextWidth());
        List<String> wrappedBody     = TooltipPainter.wrapStrings(model.bodyLines(), tr, maxTextWidth());
        List<String> wrappedAffixes  = model.affixLines() != null
                ? wrapAffixStrings(model.affixLines(), tr, maxTextWidth())
                : List.of();
        wrappedAffixes = filterBracketOnlyAffixLines(wrappedAffixes, altDown);
        List<InlineStatRow> bodyStats = new ArrayList<>(wrappedBody.size());
        for (String line : wrappedBody) {
            bodyStats.add(parseInlineStatRow(line));
        }
        List<String> wrappedExtra   = new ArrayList<>();
        for (Text t : model.extraLines()) {
            wrappedExtra.addAll(TooltipPainter.wrapStrings(List.of(t.getString()), tr, maxTextWidth()));
        }
        List<InlineStatRow> extraStats = new ArrayList<>(wrappedExtra.size());
        for (String line : wrappedExtra) {
            extraStats.add(parseInlineStatRow(line));
        }
        int statValueColumnW = inlineStatValueColumnWidth(tr, bodyStats, extraStats);

        // ---- Tab and scroll state ----
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        ScrollState.notifyItem(itemId);

        List<TabState.Tab> availableTabs = new ArrayList<>();
        if (TooltipNavigationConfig.tooltipTabs()) {
            if (!wrappedAbility.isEmpty() || !wrappedCustom.isEmpty())        availableTabs.add(TabState.Tab.LORE);
            if (model.upgradeSection() != null)                               availableTabs.add(TabState.Tab.FORGE);
            if (!wrappedBody.isEmpty() || !wrappedExtra.isEmpty())            availableTabs.add(TabState.Tab.STATS);
            if (!wrappedAffixes.isEmpty())                                    availableTabs.add(TabState.Tab.AFFIXES);
            TabState.notifyItem(itemId, availableTabs);
        }

        boolean tabsActive   = TooltipNavigationConfig.tooltipTabs() && TabState.multiTab();
        boolean drawLore     = !tabsActive || TabState.activeTab() == TabState.Tab.LORE;
        boolean drawForge    = !tabsActive || TabState.activeTab() == TabState.Tab.FORGE;
        boolean drawStats    = !tabsActive || TabState.activeTab() == TabState.Tab.STATS;
        boolean drawAffixes  = !tabsActive || TabState.activeTab() == TabState.Tab.AFFIXES;

        // ---- Layout ----
        int lineHeight  = tr.fontHeight + lineSpacing();
        int upgradeRowH = lineHeight + 3;
        int sectionGap  = 4;
        int iconAreaW   = 36;
        int hintRowH    = (model.hint() != null) ? lineHeight + 2 : 0;
        int headerBottomPad = Math.max(2, padding() - 8);
        int headerH     = padding() + 16 + 6 + 12 + hintRowH + headerBottomPad;
        int separatorH  = 10;

        boolean hasAbility = !wrappedAbility.isEmpty();
        boolean hasCustom  = !wrappedCustom.isEmpty();
        boolean hasBody    = !wrappedBody.isEmpty();
        boolean hasExtra   = !wrappedExtra.isEmpty();

        UpgradeSection upgradeSection = model.upgradeSection();
        boolean hasUpgrade = upgradeSection != null;
        boolean splitBowUpgradeTabs = hasUpgrade && tabsActive && isSimplyBowsStack(stack);
        boolean drawUpgradeSummary = hasUpgrade && splitBowUpgradeTabs && drawLore;
        boolean drawUpgradeRuneDetails = hasUpgrade && splitBowUpgradeTabs && drawForge;
        boolean drawUpgradeFull = hasUpgrade && !splitBowUpgradeTabs && drawForge;
        boolean customNeedsSeparator = hasCustom && drawLore && hasAbility;
        boolean loreUpgradeSeparator = drawLore && (hasAbility || hasCustom) && (drawUpgradeSummary || drawUpgradeFull);
        boolean statsNeedsLoreSeparator =
                ((drawLore && (hasAbility || hasCustom)) || drawUpgradeSummary || drawUpgradeRuneDetails || drawUpgradeFull)
                        && hasBody && drawStats;
        boolean extraNeedsSeparator = hasExtra && drawStats && (
                hasBody
                        || (!tabsActive && (hasAbility || hasCustom || hasUpgrade))
        );
        List<String> wrappedRuneEffect = hasUpgrade
                ? TooltipPainter.wrapStrings(upgradeSection.rune().effectLines(), tr, maxTextWidth())
                : List.of();
        boolean hasAffixes    = !wrappedAffixes.isEmpty();
        boolean hasBodyContent = (hasAbility && drawLore)
                || (hasCustom && drawLore)
                || drawUpgradeSummary
                || drawUpgradeRuneDetails
                || drawUpgradeFull
                || ((hasBody || hasExtra) && drawStats)
                || (hasAffixes && drawAffixes);

        // Panel width
        int textContentW = 0;
        // Cap title contribution so a long Apotheosis-renamed item doesn't widen the panel beyond
        // maxTextWidth(). The rendering step scales the title down (or truncates) to fit.
        textContentW = Math.max(textContentW,
                Math.min(tr.getWidth(model.title()) + iconAreaW + 4,
                         maxTextWidth()));
        if (!wrappedAbility.isEmpty()) {
            textContentW = Math.max(textContentW, tr.getWidth("\u25C6 " + abilitySection.header()));
        }
        for (String s : wrappedAbility) {
            // Strip SECTION_MARKER and add "◆ " prefix for accurate width measurement
            String measured = s.startsWith(ModernTooltipModel.SECTION_MARKER)
                    ? "\u25C6 " + s.substring(ModernTooltipModel.SECTION_MARKER.length()) : s;
            textContentW = Math.max(textContentW, tr.getWidth(measured));
        }
        for (String s : wrappedCustom) {
            textContentW = Math.max(textContentW, tr.getWidth(s));
        }
        for (int i = 0; i < wrappedBody.size(); i++) {
            String bodyLine = wrappedBody.get(i);
            if (bodyLine.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                // Measure as "◆ Label" — the actual rendered text — not the raw SECTION_MARKER string.
                String measured = "\u25C6 " + bodyLine.substring(ModernTooltipModel.SECTION_MARKER.length());
                textContentW = Math.max(textContentW, tr.getWidth(measured));
            } else {
                InlineStatRow stat = bodyStats.get(i);
                if (stat != null) {
                    int statW = statRowMinWidth(tr, stat, statValueColumnW);
                    textContentW = Math.max(textContentW, statW);
                } else {
                    textContentW = Math.max(textContentW, tr.getWidth(bodyLine));
                }
            }
        }
        for (int i = 0; i < wrappedExtra.size(); i++) {
            InlineStatRow stat = extraStats.get(i);
            if (stat != null) {
                int statW = statRowMinWidth(tr, stat, statValueColumnW);
                textContentW = Math.max(textContentW, statW);
            } else {
                textContentW = Math.max(textContentW, tr.getWidth(wrappedExtra.get(i)));
            }
        }
        for (String affixLine : wrappedAffixes) {
            if (affixLine.equals(ModernTooltipModel.AFFIX_DIVIDER)) {
                // zero width contribution — just a visual divider
            } else if (affixLine.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                String measured = "\u25C6 " + affixLine.substring(ModernTooltipModel.SECTION_MARKER.length());
                textContentW = Math.max(textContentW, tr.getWidth(measured));
            } else {
                textContentW = Math.max(textContentW, tr.getWidth(affixLine));
            }
        }

        if (hasUpgrade) {
            int slotsW = tr.getWidth("\u25C7 ") + tr.getWidth("Slots  ") + 2
                       + upgradeSection.totalSlots() * 7;
            textContentW = Math.max(textContentW, slotsW);
            for (UpgradeRow row : upgradeSection.rows()) {
                int iconW    = tr.getWidth(row.icon() + " ");
                int lblW     = tr.getWidth(row.label() + "  ") + 2;
                int contentW = altDown
                    ? Math.min(iconW + lblW + tr.getWidth(row.altText()), maxTextWidth())
                    : iconW + lblW + row.max() * 7;
                textContentW = Math.max(textContentW, contentW);
            }
            for (String line : wrappedRuneEffect)
                textContentW = Math.max(textContentW, tr.getWidth(line));
        }

        if (badges != null && !badges.isEmpty()) {
            int totalBadgeW = 0;
            for (int bi = 0; bi < badges.size(); bi++) {
                totalBadgeW += tr.getWidth(badges.get(bi)) + 6;
                if (bi < badges.size() - 1) totalBadgeW += 4;
            }
            textContentW = Math.max(textContentW, iconAreaW + totalBadgeW + 4);
        }

        if (model.hint() != null) {
            textContentW = Math.max(textContentW, iconAreaW + tr.getWidth(model.hint().getString()) + 4);
        }

        textContentW = Math.max(textContentW, 150);
        int panelW   = padding() + textContentW + padding();

        // Panel height — each section is gated by its draw-tab boolean
        int bodyH = 0;
        if (hasAbility && drawLore) {
            bodyH += lineHeight + sectionGap; // "◆ Description" header
            boolean sawAbilityContent = false;
            for (String line : wrappedAbility) {
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    bodyH += (sawAbilityContent ? separatorH : 0) + lineHeight + sectionGap; // optional separator + sub-header
                } else {
                    bodyH += lineHeight;
                    if (!line.trim().isEmpty()) {
                        sawAbilityContent = true;
                    }
                }
            }
        }
        if (hasCustom && drawLore) {
            bodyH += customNeedsSeparator ? separatorH : 0;
            bodyH += wrappedCustom.size() * lineHeight;
        }
        if (loreUpgradeSeparator) bodyH += separatorH;
        if (drawUpgradeFull) {
            bodyH += lineHeight + sectionGap;              // "◆ Upgrades" header
            bodyH += upgradeRowH;                          // Slots row
            bodyH += upgradeRowH * upgradeSection.rows().size();
            bodyH += lineHeight;                           // spacer before rune
            bodyH += upgradeRowH + wrappedRuneEffect.size() * lineHeight;
        } else if (drawUpgradeSummary) {
            bodyH += lineHeight + sectionGap;              // "◆ Upgrades" header
            bodyH += upgradeRowH;                          // Slots row
            bodyH += upgradeRowH * upgradeSection.rows().size();
            bodyH += lineHeight;                           // spacer before rune
            bodyH += upgradeRowH;                          // Rune name row
        } else if (drawUpgradeRuneDetails) {
            bodyH += lineHeight + sectionGap;              // "◆ Rune" header
            bodyH += upgradeRowH;                          // Rune name row
            bodyH += wrappedRuneEffect.size() * lineHeight;
        }
        if (statsNeedsLoreSeparator) {
            bodyH += separatorH;
        }
        if (hasBody && drawStats) {
            for (int i = 0; i < wrappedBody.size(); i++) {
                if (wrappedBody.get(i).startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    bodyH += lineHeight + sectionGap;
                } else {
                    bodyH += statRowHeight(bodyStats.get(i), lineHeight) + BODY_LINE_EXTRA_GAP;
                }
            }
        }
        if (hasExtra && drawStats) {
            bodyH += extraNeedsSeparator ? separatorH : 0;
            for (InlineStatRow stat : extraStats) {
                bodyH += statRowHeight(stat, lineHeight) + BODY_LINE_EXTRA_GAP;
            }
        }
        if (hasAffixes && drawAffixes) {
            boolean sawAffixContent = false;
            for (int i = 0; i < wrappedAffixes.size(); i++) {
                String line = wrappedAffixes.get(i);
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    bodyH += (sawAffixContent ? separatorH : 0) + lineHeight + sectionGap;
                } else if (line.equals(ModernTooltipModel.AFFIX_DIVIDER)) {
                    bodyH += AFFIX_DIVIDER_H;
                } else {
                    boolean nextIsDivider = i + 1 < wrappedAffixes.size()
                            && wrappedAffixes.get(i + 1).equals(ModernTooltipModel.AFFIX_DIVIDER);
                    bodyH += lineHeight + (nextIsDivider ? 0 : BODY_LINE_EXTRA_GAP);
                    if (!line.trim().isEmpty()) sawAffixContent = true;
                }
            }
        }

        int footerH = 14;
        int panelH  = headerH + (hasBodyContent ? separatorH : 0) + bodyH + footerH;

        // Panel position (with screen-edge clamping)
        int panelX = x + 12;
        int panelY = y - 12;
        if (panelX + panelW > screenW - 6) panelX = x - panelW - 12;
        if (panelX < 6) panelX = 6;
        if (panelY + panelH > screenH - 6) panelY = screenH - panelH - 6;
        if (panelY < 6) panelY = 6;

        // Scroll clamping — clamp body height to the smaller of screen-safe max and a fixed viewport cap
        final int MAX_BODY_H   = Math.min(
                screenH - headerH - footerH - (hasBodyContent ? separatorH : 0) - 24,
                maxBodyH());
        final boolean scrollActive = TooltipNavigationConfig.scrollableTooltip() && bodyH > MAX_BODY_H;
        final int clampedBodyH = scrollActive ? MAX_BODY_H : bodyH;
        final int scrollMax    = scrollActive ? (bodyH - clampedBodyH) : 0;
        if (scrollActive) {
            panelH = headerH + (hasBodyContent ? separatorH : 0) + clampedBodyH + footerH;
            if (panelY + panelH > screenH - 6) panelY = screenH - panelH - 6;
            if (panelY < 6) panelY = 6;
        }

        // ---- Draw background and border ----
        TooltipPainter.drawGradientBackground(context, panelX, panelY, panelW, panelH, theme);

        BackgroundMotif motif = MotifRegistry.get(resolvedMotifKey);
        if (motif != null) motif.draw(context, panelX, panelY, panelW, panelH, System.currentTimeMillis());

        BorderRenderer.drawDecorativeBorder(context, panelX, panelY, panelW, panelH, theme, borderStyle);

        int cursorY = panelY + padding();

        // ---- Header: item icon ----
        int iconFrameX = panelX + padding() + 2;
        int iconFrameY = cursorY + 2;
        TooltipPainter.drawItemFrame(context, iconFrameX, iconFrameY, 24, theme, itemBorderShape);

        long  iconTimeMs = System.currentTimeMillis();
        float breatheScale, spinDegrees, bobOffset;
        switch (itemAnimStyle != null ? itemAnimStyle : "breathe_spin_bob") {
            case "spin" -> {
                // Slow continuous rotation, no scale or bob
                breatheScale = 1.0F;
                spinDegrees  = (float)(iconTimeMs % 9000L) / 9000.0F * 360.0F;
                bobOffset    = 0.0F;
            }
            case "bob" -> {
                // Vertical bob only
                breatheScale = 1.0F;
                spinDegrees  = 0.0F;
                bobOffset    = (float) Math.sin(iconTimeMs * 0.0026) * 1.8F;
            }
            case "breathe" -> {
                // Scale pulse only
                breatheScale = 1.0F + (float) Math.sin(iconTimeMs * 0.0042) * 0.12F;
                spinDegrees  = 0.0F;
                bobOffset    = 0.0F;
            }
            case "static" -> {
                breatheScale = 1.0F;
                spinDegrees  = 0.0F;
                bobOffset    = 0.0F;
            }
            default -> {
                // "breathe_spin_bob" — all three combined
                breatheScale = 1.0F + (float) Math.sin(iconTimeMs * 0.0042) * 0.055F;
                spinDegrees  = (float) Math.sin(iconTimeMs * 0.0018) * 4.0F;
                bobOffset    = (float) Math.sin(iconTimeMs * 0.0026 + 1.1) * 0.7F;
            }
        }

        context.getMatrices().push();
        float itemCenterX = iconFrameX + 12.0F;
        float itemCenterY = iconFrameY + 12.0F + bobOffset;
        context.getMatrices().translate(itemCenterX, itemCenterY, 0.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinDegrees));
        context.getMatrices().scale(breatheScale, breatheScale, 1.0F);
        context.getMatrices().translate(-8.0F, -8.0F, 0.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();

        // ---- Header: title + badges ----
        int nameX = panelX + padding() + iconAreaW;
        int nameY = cursorY + 4;

        // Scale the title down proportionally if it is wider than the available header space.
        // At MIN_TITLE_SCALE the string is truncated with "..." to avoid unreadably small text.
        int availTitleW = panelW - padding() - iconAreaW - padding() - 4;
        float titleScale = Math.min(1.0f, (float) availTitleW / Math.max(1, tr.getWidth(model.title())));
        String displayTitle = model.title();
        if (titleScale < MIN_TITLE_SCALE) {
            titleScale = MIN_TITLE_SCALE;
            int maxTitlePixelW = (int) (availTitleW / MIN_TITLE_SCALE);
            displayTitle = truncateTitleToWidth(tr, model.title(), maxTitlePixelW);
        }
        if (titleScale < 1.0f) {
            context.getMatrices().push();
            context.getMatrices().translate(nameX, nameY, 0);
            context.getMatrices().scale(titleScale, titleScale, 1.0f);
            context.getMatrices().translate(-nameX, -nameY, 0);
        }
        switch (titleAnimStyle != null ? titleAnimStyle : "wave") {
            case "shimmer" -> TooltipPainter.drawShimmerText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
            case "pulse"   -> TooltipPainter.drawPulseText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
            case "flicker" -> TooltipPainter.drawFlickerText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
            case "shiver", "shivering" -> TooltipPainter.drawShiverText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
            case "quiver"  -> TooltipPainter.drawQuiverText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
            case "breathe_spin_bob" -> TooltipPainter.drawBreatheSpinBobText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
            case "drop_bounce" -> TooltipPainter.drawDropBounceText(context, tr, displayTitle, nameX, nameY, theme.name(), tooltipElapsedMs);
            case "hinge_fall" -> {
                // Clip title animation to tooltip bounds so off-panel motion stays hidden.
                context.enableScissor(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1);
                TooltipPainter.drawHingeFallText(context, tr, displayTitle, nameX, nameY, theme.name(), tooltipElapsedMs);
                context.disableScissor();
            }
            case "obfuscate" -> TooltipPainter.drawObfuscateText(context, tr, displayTitle, nameX, nameY, theme.name(), tooltipElapsedMs);
            case "static"  -> context.drawText(tr, Text.literal(displayTitle).setStyle(
                                  Style.EMPTY.withColor(TextColor.fromRgb(theme.name() & 0x00FFFFFF))),
                                  nameX, nameY, theme.name(), true);
            default        -> TooltipPainter.drawWaveText(context, tr, displayTitle, nameX, nameY, theme.name(), iconTimeMs);
        }
        if (titleScale < 1.0f) context.getMatrices().pop();

        int badgeY = cursorY + 4 + tr.fontHeight + 3;
        if (badges != null && !badges.isEmpty()) {
            int badgeX = nameX;
            for (int bi = 0; bi < badges.size(); bi++) {
                badgeX = TooltipPainter.drawBadge(context, tr, badges.get(bi), badgeX, badgeY, theme);
                if (bi < badges.size() - 1) badgeX += 4;
            }
        }

        if (model.hint() != null) {
            String hintText = model.hint().getString();
            int hintX = panelX + panelW - padding() - 3 - tr.getWidth(hintText);
            int hintY = panelY + headerH - tr.fontHeight + 2;
            context.drawText(tr, model.hint(), hintX, hintY, theme.hint(), false);
        }

        cursorY = panelY + headerH;

        // ---- Separator after header ----
        if (hasBodyContent) {
            TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
            cursorY += separatorH;
        }

        // ---- Scroll scissor region ----
        final int bodyClipTop    = cursorY;
        final int bodyClipBottom = bodyClipTop + clampedBodyH;
        if (scrollActive) {
            ScrollState.flushScrollDelta(scrollMax);
            context.enableScissor(panelX, bodyClipTop, panelX + panelW, bodyClipBottom);
            cursorY -= ScrollState.get();
        }

        // ---- Ability / Description section ----
        if (hasAbility && drawLore) {
            context.drawText(tr,
                    Text.literal("\u25C6 " + abilitySection.header()).setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                    panelX + padding(), cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;
            boolean sawAbilityContent = false;
            for (String line : wrappedAbility) {
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    // Sub-section header: separator + "◆ " label in sectionHeader colour
                    String headerText = "\u25C6 " + line.substring(ModernTooltipModel.SECTION_MARKER.length());
                    if (sawAbilityContent) {
                        TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
                        cursorY += separatorH;
                    }
                    context.drawText(tr,
                            Text.literal(headerText).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                            panelX + padding(), cursorY, theme.sectionHeader(), true);
                    cursorY += lineHeight + sectionGap;
                } else {
                    context.drawText(tr,
                            Text.literal(line).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                            panelX + padding(), cursorY, theme.body(), true);
                    cursorY += lineHeight;
                    if (!line.trim().isEmpty()) {
                        sawAbilityContent = true;
                    }
                }
            }
        }

        // ---- Theme custom text section (below Description) ----
        if (hasCustom && drawLore) {
            if (customNeedsSeparator) {
                TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
                cursorY += separatorH;
            }
            for (String line : wrappedCustom) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                        panelX + padding(), cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // ---- Separator between lore text and upgrades ----
        if (loreUpgradeSeparator) {
            TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
            cursorY += separatorH;
        }

        // ---- Upgrade section ----
        if (drawUpgradeFull || drawUpgradeSummary) {
            int leftX = panelX + padding();
            context.drawText(tr,
                    Text.literal("\u25C6 Upgrades").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                    leftX, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;

            int pipSequenceBase = 0;

            // Slots row
            int slotLabelColor = TooltipPainter.lerpColor(theme.body(), 0xFFFFFFFF, 0.10f);
            context.drawText(tr,
                    Text.literal("\u25C7").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(theme.slotFilled() & 0x00FFFFFF))),
                    leftX, cursorY, theme.slotFilled(), false);
            int slotPipX = leftX + tr.getWidth("\u25C7 ");
            context.drawText(tr,
                    Text.literal("Slots").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(slotLabelColor & 0x00FFFFFF))),
                    slotPipX, cursorY, slotLabelColor, false);
            slotPipX += tr.getWidth("Slots  ") + 2;
            for (int i = 0; i < upgradeSection.totalSlots(); i++) {
                boolean isFilled     = i < upgradeSection.usedSlots();
                int     slotColor    = isFilled ? theme.slotFilled() : theme.slotEmpty();
                int     topHighlight = isFilled ? 0xFFF5D060 : 0xFF4A4030;
                int     seqIdx       = isFilled ? (pipSequenceBase++) : -1;
                PipAnimator.drawAnimatedSquarePip(context, slotPipX, cursorY + 1, 5,
                        slotColor, topHighlight, tooltipElapsedMs, seqIdx, isFilled);
                slotPipX += 7;
            }
            cursorY += upgradeRowH;

            // Variable upgrade rows (String, Frame, etc.)
            for (UpgradeRow row : upgradeSection.rows()) {
                int labelColor = TooltipPainter.lerpColor(row.pipColor(), 0xFFFFFFFF, 0.20f);
                int descColor  = TooltipPainter.lerpColor(row.pipColor(), theme.body(), 0.50f);
                context.drawText(tr,
                        Text.literal(row.icon()).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(row.pipColor() & 0x00FFFFFF))),
                        leftX, cursorY, row.pipColor(), false);
                int rowLabelX = leftX + tr.getWidth(row.icon() + " ");
                context.drawText(tr,
                        Text.literal(row.label()).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(labelColor & 0x00FFFFFF))),
                        rowLabelX, cursorY, labelColor, false);
                int afterLabel = rowLabelX + tr.getWidth(row.label() + " ") + 2;
                if (altDown) {
                    context.drawText(tr,
                            Text.literal(row.altText()).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(descColor & 0x00FFFFFF))),
                            afterLabel, cursorY, descColor, false);
                } else {
                    int rowPipX = afterLabel;
                    for (int i = 0; i < row.max(); i++) {
                        boolean filled   = i < row.filled();
                        int     pipColor = filled ? row.pipColor() : theme.slotEmpty();
                        int     seqIdx   = filled ? (pipSequenceBase++) : -1;
                        PipAnimator.drawAnimatedPip(context, rowPipX, cursorY + 1,
                                pipColor, filled, tooltipElapsedMs, seqIdx);
                        rowPipX += 7;
                    }
                }
                cursorY += upgradeRowH;
            }

            cursorY += lineHeight; // spacer before rune

            // Rune row (summary in split-bow tabs, full in default mode)
            UpgradeRune rune           = upgradeSection.rune();
            int runeLabelColor         = TooltipPainter.lerpColor(rune.runeColor(), 0xFFFFFFFF, 0.22f);
            int runeTextColor          = rune.isNone() ? 0xFF6A6060 : rune.runeColor();
            int runeDescColor          = rune.isNone() ? 0xFF6A6060
                    : TooltipPainter.lerpColor(rune.runeColor(), theme.body(), 0.45f);
            context.drawText(tr,
                    Text.literal("\u25CE").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(rune.runeColor() & 0x00FFFFFF))),
                    leftX, cursorY, rune.runeColor(), false);
            int runeLabelX = leftX + tr.getWidth("\u25CE ");
            context.drawText(tr,
                    Text.literal("Rune: ").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(runeLabelColor & 0x00FFFFFF))),
                    runeLabelX, cursorY, runeLabelColor, false);
            int runeValX = runeLabelX + tr.getWidth("Rune: ");
            context.drawText(tr,
                    Text.literal(rune.runeName()).setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(runeTextColor & 0x00FFFFFF))),
                    runeValX, cursorY, runeTextColor, false);
            cursorY += upgradeRowH;
            if (drawUpgradeFull) {
                for (String effectLine : wrappedRuneEffect) {
                    context.drawText(tr,
                            Text.literal(effectLine).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(runeDescColor & 0x00FFFFFF))),
                            leftX, cursorY, runeDescColor, false);
                    cursorY += lineHeight;
                }
            }
        }

        if (drawUpgradeRuneDetails) {
            int leftX = panelX + padding();
            UpgradeRune rune = upgradeSection.rune();
            int runeLabelColor = TooltipPainter.lerpColor(rune.runeColor(), 0xFFFFFFFF, 0.22f);
            int runeTextColor  = rune.isNone() ? 0xFF6A6060 : rune.runeColor();
            int runeDescColor  = rune.isNone() ? 0xFF6A6060
                    : TooltipPainter.lerpColor(rune.runeColor(), theme.body(), 0.45f);

            context.drawText(tr,
                    Text.literal("\u25C6 Rune").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                    leftX, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;

            context.drawText(tr,
                    Text.literal("\u25CE").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(rune.runeColor() & 0x00FFFFFF))),
                    leftX, cursorY, rune.runeColor(), false);
            int runeLabelX = leftX + tr.getWidth("\u25CE ");
            context.drawText(tr,
                    Text.literal("Rune: ").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(runeLabelColor & 0x00FFFFFF))),
                    runeLabelX, cursorY, runeLabelColor, false);
            int runeValX = runeLabelX + tr.getWidth("Rune: ");
            context.drawText(tr,
                    Text.literal(rune.runeName()).setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(runeTextColor & 0x00FFFFFF))),
                    runeValX, cursorY, runeTextColor, false);
            cursorY += upgradeRowH;

            for (String effectLine : wrappedRuneEffect) {
                context.drawText(tr,
                        Text.literal(effectLine).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(runeDescColor & 0x00FFFFFF))),
                        leftX, cursorY, runeDescColor, false);
                cursorY += lineHeight;
            }
        }

        // ---- Separator between (lore/upgrades) and body ----
        if (statsNeedsLoreSeparator) {
            TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
            cursorY += separatorH;
        }

        // ---- Body lines ----
        if (hasBody && drawStats) {
            int contentLeft  = panelX + padding();
            int contentRight = panelX + panelW - padding();
            for (int i = 0; i < wrappedBody.size(); i++) {
                String line = wrappedBody.get(i);
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    String headerText = "\u25C6 " + line.substring(ModernTooltipModel.SECTION_MARKER.length());
                    context.drawText(tr,
                            Text.literal(headerText).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                            contentLeft, cursorY, theme.sectionHeader(), true);
                    cursorY += lineHeight + sectionGap;
                } else {
                    InlineStatRow stat = bodyStats.get(i);
                    if (stat != null) {
                        drawInlineStatRow(context, tr, stat, contentLeft, contentRight, cursorY, theme, statValueColumnW);
                    } else {
                        drawTextWithHighlightedNumbers(context, tr, line,
                                contentLeft, cursorY, theme.body(), theme.sectionHeader());
                    }
                    cursorY += statRowHeight(stat, lineHeight) + BODY_LINE_EXTRA_GAP;
                }
            }
        }

        // ---- Extra lines (enchantments, durability, etc.) ----
        if (hasExtra && drawStats) {
            if (extraNeedsSeparator) {
                TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
                cursorY += separatorH;
            }
            int extraColor   = TooltipPainter.lerpColor(theme.body(), 0xFFB8C2CF, 0.30f);
            int contentLeft  = panelX + padding();
            int contentRight = panelX + panelW - padding();
            for (int i = 0; i < wrappedExtra.size(); i++) {
                String line = wrappedExtra.get(i);
                InlineStatRow stat = extraStats.get(i);
                if (stat != null) {
                    drawInlineStatRow(context, tr, stat, contentLeft, contentRight, cursorY, theme, statValueColumnW);
                } else {
                    drawTextWithHighlightedNumbers(context, tr, line,
                            contentLeft, cursorY, extraColor, theme.sectionHeader());
                }
                cursorY += statRowHeight(stat, lineHeight) + BODY_LINE_EXTRA_GAP;
            }
        }

        // ---- Affixes tab ----
        if (hasAffixes && drawAffixes) {
            int contentLeft = panelX + padding();
            boolean sawAffixContent = false;
            int pendingLeadingRomanColor = 0;
            boolean inSocketsSection = false;
            boolean inGemDescriptionBlock = false;
            for (int i = 0; i < wrappedAffixes.size(); i++) {
                String line = wrappedAffixes.get(i);
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    pendingLeadingRomanColor = 0;
                    inGemDescriptionBlock = false;
                    String sectionTitle = line.substring(ModernTooltipModel.SECTION_MARKER.length());
                    inSocketsSection = isSocketsSectionTitle(sectionTitle);
                    String headerText = "\u25C6 " + sectionTitle;
                    if (sawAffixContent) {
                        TooltipPainter.drawSeparator(context, panelX + padding(), cursorY,
                                panelW - padding() * 2, theme);
                        cursorY += separatorH;
                    }
                    context.drawText(tr,
                            Text.literal(headerText).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                            contentLeft, cursorY, theme.sectionHeader(), true);
                    cursorY += lineHeight + sectionGap;
                } else if (line.equals(ModernTooltipModel.AFFIX_DIVIDER)) {
                    pendingLeadingRomanColor = 0;
                    inGemDescriptionBlock = false;
                    int subtleColor = (theme.separator() & 0x00FFFFFF) | 0x28000000;
                    int lineY = cursorY + 1;
                    context.fill(contentLeft + 8, lineY,
                            panelX + panelW - padding() - 8, lineY + 1, subtleColor);
                    cursorY += AFFIX_DIVIDER_H;
                } else {
                    String trimmed = line.trim();
                    if (isSocketEntryLine(trimmed)) {
                        inGemDescriptionBlock = false;
                    }
                    boolean gemDescStart = inSocketsSection
                            && line.startsWith("  ")
                            && trimmed.startsWith("\u2022");
                    boolean gemDescContinuation = inSocketsSection
                            && inGemDescriptionBlock
                            && !gemDescStart
                            && line.startsWith(" ")
                            && !trimmed.isEmpty()
                            && !isSocketEntryLine(trimmed);
                    if (gemDescStart) {
                        inGemDescriptionBlock = true;
                    } else if (inGemDescriptionBlock && !gemDescContinuation) {
                        inGemDescriptionBlock = false;
                    }

                    boolean gemDescLine = gemDescStart || gemDescContinuation;
                    int lineBaseColor = gemDescLine ? AFFIX_POSITIVE_COLOR : theme.body();
                    int lineNumberColor = gemDescLine ? AFFIX_POSITIVE_COLOR : theme.sectionHeader();

                    boolean nextIsDivider = i + 1 < wrappedAffixes.size()
                            && wrappedAffixes.get(i + 1).equals(ModernTooltipModel.AFFIX_DIVIDER);
                    pendingLeadingRomanColor = drawAffixTextWithHighlights(context, tr, line,
                            contentLeft, cursorY, lineBaseColor, lineNumberColor,
                            pendingLeadingRomanColor, altDown);
                    cursorY += lineHeight + (nextIsDivider ? 0 : BODY_LINE_EXTRA_GAP);
                    if (!line.trim().isEmpty()) sawAffixContent = true;
                }
            }
        }

        // ---- End of body — close scissor and draw scrollbar ----
        if (scrollActive) {
            context.disableScissor();
            TooltipPainter.drawScrollbar(context,
                    panelX + panelW - 4, bodyClipTop,
                    clampedBodyH, ScrollState.get(), scrollMax, theme);
        }

        // ---- Footer dots (or tab indicator dots) ----
        if (tabsActive) {
            String cycleTabKey = TooltipKeybinds.CYCLE_TAB.getBoundKeyLocalizedText().getString();
            TooltipPainter.drawTabDotsWithKeyHint(context, tr, panelX + panelW / 2, panelY + panelH - 8,
                    TabState.tabs(), TabState.activeTab(), theme, cycleTabKey);
        } else {
            TooltipPainter.drawFooterDots(context, panelX + panelW / 2, panelY + panelH - 8, theme);
        }

        // Record whether this frame had an active scroll region (used by MOUSE_SCROLLED to consume the event).
        ScrollState.setScrollableActive(scrollActive);
        TooltipGifRecorder.markTooltipRendered(panelX, panelY, panelW, panelH);

        context.getMatrices().pop();
    }

    // --- Helpers ---

    /** Maps a border-style constant to the motif key used by {@link MotifRegistry}. */
    private static String motifKeyFor(int borderStyle) {
        return switch (borderStyle) {
            case TooltipBorderStyle.VINE      -> "vine";
            case TooltipBorderStyle.BEE       -> "bee";
            case TooltipBorderStyle.BLOSSOM   -> "blossom";
            case TooltipBorderStyle.BUBBLE    -> "bubble";
            case TooltipBorderStyle.EARTH     -> "earth";
            case TooltipBorderStyle.ECHO      -> "echo";
            case TooltipBorderStyle.ICE       -> "ice";
            case TooltipBorderStyle.LIGHTNING -> "lightning";
            case TooltipBorderStyle.EMBER     -> "ember";
            case TooltipBorderStyle.ENCHANTED -> "enchanted";
            case TooltipBorderStyle.AUTUMN    -> "autumn";
            case TooltipBorderStyle.SOUL      -> "soul";
            case TooltipBorderStyle.DEEP_DARK -> "deepdark";
            case TooltipBorderStyle.POISON    -> "poison";
            case TooltipBorderStyle.OCEAN     -> "ocean";
            case TooltipBorderStyle.RUSTIC    -> "rustic";
            case TooltipBorderStyle.HONEY     -> "honey";
            case TooltipBorderStyle.JADE      -> "jade";
            case TooltipBorderStyle.WOOD      -> "wood";
            case TooltipBorderStyle.STONE     -> "stone";
            case TooltipBorderStyle.IRON      -> "iron";
            case TooltipBorderStyle.GOLD      -> "gold";
            case TooltipBorderStyle.DIAMOND   -> "diamond";
            case TooltipBorderStyle.NETHERITE -> "netherite";
            case TooltipBorderStyle.RUNIC     -> "runic";
            default -> null;
        };
    }

    /** Maps a motif name string (from {@link ThemeDefinition}) to a border-style constant. */
    private static int borderStyleFor(String motif) {
        if (motif == null) return TooltipBorderStyle.DEFAULT;
        return switch (motif) {
            case "vine"      -> TooltipBorderStyle.VINE;
            case "bee"       -> TooltipBorderStyle.BEE;
            case "blossom"   -> TooltipBorderStyle.BLOSSOM;
            case "bubble"    -> TooltipBorderStyle.BUBBLE;
            case "earth"     -> TooltipBorderStyle.EARTH;
            case "echo"      -> TooltipBorderStyle.ECHO;
            case "ice"       -> TooltipBorderStyle.ICE;
            case "lightning" -> TooltipBorderStyle.LIGHTNING;
            case "ember"     -> TooltipBorderStyle.EMBER;
            case "enchanted" -> TooltipBorderStyle.ENCHANTED;
            case "autumn"    -> TooltipBorderStyle.AUTUMN;
            case "soul"      -> TooltipBorderStyle.SOUL;
            case "deepdark"  -> TooltipBorderStyle.DEEP_DARK;
            case "poison"    -> TooltipBorderStyle.POISON;
            case "ocean"     -> TooltipBorderStyle.OCEAN;
            case "rustic"    -> TooltipBorderStyle.RUSTIC;
            case "honey"     -> TooltipBorderStyle.HONEY;
            case "jade"      -> TooltipBorderStyle.JADE;
            case "wood"      -> TooltipBorderStyle.WOOD;
            case "stone"     -> TooltipBorderStyle.STONE;
            case "iron"      -> TooltipBorderStyle.IRON;
            case "gold"      -> TooltipBorderStyle.GOLD;
            case "diamond"   -> TooltipBorderStyle.DIAMOND;
            case "netherite" -> TooltipBorderStyle.NETHERITE;
            case "runic"     -> TooltipBorderStyle.RUNIC;
            default          -> TooltipBorderStyle.DEFAULT;
        };
    }

    /**
     * Resolves the {@link ThemeDefinition} to use for this tooltip using the priority chain:
     * <ol>
     *   <li>Exact per-item mapping from {@link ItemThemeRegistry} (hard override)</li>
     *   <li>Provider-supplied {@code model.themeKey()} (e.g. mod-specific defaults)</li>
     *   <li>Per-tag mapping from {@link ItemThemeRegistry}</li>
     *   <li>Vanilla item rarity (common / uncommon / rare / epic)</li>
     *   <li>Registry default (golden fallback if a rarity key is somehow missing)</li>
     * </ol>
     */
    private static ThemeDefinition resolveTheme(ItemStack stack, ModernTooltipModel model) {
        // 1. Data-driven exact item mapping (always wins)
        String itemDataKey = ItemThemeRegistry.resolveItemThemeForStack(stack);
        if (itemDataKey != null)
            return ThemeRegistry.get(itemDataKey);

        // 2. Provider-supplied default
        if (model.themeKey() != null)
            return ThemeRegistry.get(model.themeKey());

        // 3. Data-driven tag mapping
        String tagDataKey = ItemThemeRegistry.resolveTagThemeForStack(stack);
        if (tagDataKey != null)
            return ThemeRegistry.get(tagDataKey);

        // 4. Rarity fallback
        Rarity rarity = stack.getRarity();
        return ThemeRegistry.get(rarityThemeKey(rarity != null ? rarity : Rarity.COMMON));
    }

    /** Maps a vanilla {@link Rarity} to the corresponding rarity theme key. */
    private static String rarityThemeKey(Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> "rarity_uncommon";
            case RARE     -> "rarity_rare";
            case EPIC     -> "rarity_epic";
            default       -> "rarity_common";
        };
    }

    private static List<String> wrapCustomTextKeys(List<String> customTextKeys, TextRenderer tr, int maxWidth) {
        if (customTextKeys == null || customTextKeys.isEmpty()) return List.of();

        List<String> wrapped = new ArrayList<>();
        for (String key : customTextKeys) {
            if (key == null || key.isBlank()) continue;
            String resolved = Text.translatable(key).getString();
            wrapped.addAll(TooltipPainter.wrapStrings(List.of(resolved), tr, maxWidth));
        }
        return wrapped;
    }

    private static List<String> wrapAffixStrings(List<String> lines, TextRenderer tr, int maxWidth) {
        if (lines == null || lines.isEmpty()) return List.of();

        List<String> wrapped = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null || raw.isEmpty()) {
                wrapped.add(" ");
                continue;
            }
            if (raw.startsWith(ModernTooltipModel.SECTION_MARKER)
                    || raw.equals(ModernTooltipModel.AFFIX_DIVIDER)) {
                wrapped.add(raw);
                continue;
            }

            int bulletIdx = raw.indexOf('\u2022');
            if (bulletIdx >= 0) {
                int prefixEnd = bulletIdx + 1;
                while (prefixEnd < raw.length() && Character.isWhitespace(raw.charAt(prefixEnd))) {
                    prefixEnd++;
                }
                String firstPrefix = raw.substring(0, prefixEnd);
                String continuationPrefix = spacesForPixelWidth(tr, tr.getWidth(firstPrefix));
                String content = raw.substring(prefixEnd).trim();
                wrapped.addAll(wrapWithPrefix(content, firstPrefix, continuationPrefix, tr, maxWidth));
            } else {
                wrapped.addAll(TooltipPainter.wrapStrings(List.of(raw), tr, maxWidth));
            }
        }
        return wrapped;
    }

    private static List<String> wrapWithPrefix(String content,
                                               String firstPrefix,
                                               String continuationPrefix,
                                               TextRenderer tr,
                                               int maxWidth) {
        if (content == null || content.isEmpty()) return List.of(firstPrefix);

        List<String> out = new ArrayList<>();
        String[] words = content.split("\\s+");

        StringBuilder current = new StringBuilder();
        String prefix = firstPrefix;
        for (String word : words) {
            String candidateBody = current.isEmpty() ? word : current + " " + word;
            String candidate = prefix + candidateBody;

            if (tr.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                out.add(prefix + current);
                current = new StringBuilder(word);
                prefix = continuationPrefix;
            } else {
                current = new StringBuilder(candidateBody);
            }
        }

        if (!current.isEmpty()) {
            out.add(prefix + current);
        }
        return out;
    }

    private static String spacesForPixelWidth(TextRenderer tr, int pixelWidth) {
        if (pixelWidth <= 0) return "";
        StringBuilder sb = new StringBuilder();
        while (tr.getWidth(sb.toString()) < pixelWidth) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static List<String> filterBracketOnlyAffixLines(List<String> wrappedAffixes, boolean altDown) {
        if (wrappedAffixes == null || wrappedAffixes.isEmpty() || altDown) {
            return wrappedAffixes != null ? wrappedAffixes : List.of();
        }

        List<String> filtered = new ArrayList<>(wrappedAffixes.size());
        for (String line : wrappedAffixes) {
            if (line == null || line.isEmpty()) {
                filtered.add(line);
                continue;
            }
            if (line.startsWith(ModernTooltipModel.SECTION_MARKER)
                    || line.equals(ModernTooltipModel.AFFIX_DIVIDER)) {
                filtered.add(line);
                continue;
            }

            String withoutBrackets = removeBracketedSegments(line);
            if (withoutBrackets.trim().isEmpty()) {
                continue;
            }
            filtered.add(line);
        }
        return filtered;
    }

    private static String removeBracketedSegments(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder(text.length());
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') {
                depth++;
                continue;
            }
            if (c == ']') {
                if (depth > 0) depth--;
                continue;
            }
            if (depth == 0) out.append(c);
        }
        return out.toString();
    }

    private static InlineStatRow parseInlineStatRow(String line) {
        if (line == null) return null;
        Matcher matcher = INLINE_STAT_PATTERN.matcher(line);
        if (!matcher.matches()) return null;

        double value;
        try {
            value = Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }

        String rawLabel = matcher.group(2).toLowerCase();
        return switch (rawLabel) {
            case "attack damage"             -> new InlineStatRow("Attack Damage", value, 0.0, 14.0);
            case "attack speed"              -> new InlineStatRow("Attack Speed",  value, 0.0, 3.0);
            case "attack range",
                 "entity interaction range"  -> new InlineStatRow("Attack Range",  value, 0.0, 5.0);
            default -> null;
        };
    }

    private static String formatStatValue(double value) {
        double rounded = Math.round(value * 10.0d) / 10.0d;
        return AttributeModifiersComponent.DECIMAL_FORMAT.format(rounded);
    }

    private static void drawInlineStatRow(DrawContext context, TextRenderer tr, InlineStatRow stat,
                                          int contentLeft, int contentRight, int y,
                                          TooltipTheme theme, int valueColumnW) {
        int labelColumnW = statLabelColumnWidth(tr);
        String valueText = formatStatValue(stat.value());
        int valueW = tr.getWidth(valueText);
        int valueX = contentRight - valueW;

        context.drawText(tr,
                Text.literal(stat.label()).setStyle(Style.EMPTY.withColor(
                        TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                contentLeft, y, theme.body(), true);
        context.drawText(tr,
                Text.literal(valueText).setStyle(Style.EMPTY.withColor(
                        TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                valueX, y, theme.sectionHeader(), true);

        int barStart = contentLeft + labelColumnW + 8;
        int barEnd = (contentRight - valueColumnW) - 8;
        if (barEnd - barStart < STAT_BAR_MIN_WIDTH) {
            barStart = Math.max(contentLeft + labelColumnW + 4, barEnd - STAT_BAR_MIN_WIDTH);
        }
        if (barEnd <= barStart + 2) {
            return;
        }

        int barY = y + (tr.fontHeight - STAT_BAR_HEIGHT) / 2 + 1;
        int darkBar = TooltipPainter.lerpColor(theme.bgBottom(), 0xFF000000, 0.35f);
        context.fill(barStart, barY, barEnd, barY + STAT_BAR_HEIGHT, darkBar);

        float t = (float) ((stat.value() - stat.min()) / (stat.max() - stat.min()));
        t = Math.max(0.0f, Math.min(1.0f, t));
        int fillEnd = barStart + Math.round((barEnd - barStart) * t);
        int fillColor = statFillColor(t);
        if (fillEnd > barStart) {
            context.fill(barStart, barY, fillEnd, barY + STAT_BAR_HEIGHT, fillColor);
        }
    }

    private static int statFillColor(float t) {
        int red = 0xFFC9564A;
        int orange = 0xFFE3A54C;
        int green = 0xFF6FCB63;
        if (t <= 0.5f) {
            return TooltipPainter.lerpColor(red, orange, t / 0.5f);
        }
        return TooltipPainter.lerpColor(orange, green, (t - 0.5f) / 0.5f);
    }

    private static int statRowHeight(InlineStatRow stat, int lineHeight) {
        if (stat != null && "Attack Range".equals(stat.label())) {
            return lineHeight + 2;
        }
        return lineHeight;
    }

    private static int statRowMinWidth(TextRenderer tr, InlineStatRow stat, int valueColumnW) {
        return statLabelColumnWidth(tr) + 8 + STAT_BAR_MIN_WIDTH + 8 + valueColumnW;
    }

    private static int inlineStatValueColumnWidth(TextRenderer tr,
                                                  List<InlineStatRow> bodyStats,
                                                  List<InlineStatRow> extraStats) {
        int w = statValueColumnWidth(tr);
        for (InlineStatRow stat : bodyStats) {
            if (stat != null) w = Math.max(w, tr.getWidth(formatStatValue(stat.value())));
        }
        for (InlineStatRow stat : extraStats) {
            if (stat != null) w = Math.max(w, tr.getWidth(formatStatValue(stat.value())));
        }
        return w;
    }

    private static int statLabelColumnWidth(TextRenderer tr) {
        return tr.getWidth(STAT_LABEL_REFERENCE);
    }

    private static int statValueColumnWidth(TextRenderer tr) {
        int w = 0;
        w = Math.max(w, tr.getWidth(formatStatValue(0.0)));
        w = Math.max(w, tr.getWidth(formatStatValue(5.0)));
        w = Math.max(w, tr.getWidth(formatStatValue(10.0)));
        w = Math.max(w, tr.getWidth(formatStatValue(13.5)));
        w = Math.max(w, tr.getWidth(formatStatValue(32.0)));
        return w;
    }

    private static AbilitySectionData prepareAbilitySection(ItemStack stack, List<String> abilityLines) {
        List<String> lines = new ArrayList<>(abilityLines);
        String header = DEFAULT_ABILITY_HEADER;

        if (!stack.isIn(SIMPLY_SWORDS_COMPAT_TAG)) {
            return new AbilitySectionData(header, lines);
        }

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null || raw.isBlank()) continue;

            String content = raw.startsWith(ModernTooltipModel.SECTION_MARKER)
                    ? raw.substring(ModernTooltipModel.SECTION_MARKER.length())
                    : raw;
            content = content.replace('\u00A0', ' ').trim();

            Matcher matcher = UNIQUE_EFFECT_PATTERN.matcher(content);
            if (!matcher.matches()) continue;

            String abilityName = matcher.group(1).trim();
            if (!abilityName.isEmpty()) {
                header = abilityName;
                lines.remove(i);
            }
            break;
        }

        return new AbilitySectionData(header, lines);
    }

    private static boolean isSimplyBowsStack(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id != null && "simplybows".equals(id.getNamespace());
    }

    /**
     * Returns the longest prefix of {@code title} that fits within {@code maxWidth} pixels
     * when rendered by {@code tr}, with {@code "..."} appended as a suffix.
     * If even the ellipsis alone exceeds {@code maxWidth}, returns {@code "..."}.
     */
    private static String truncateTitleToWidth(TextRenderer tr, String title, int maxWidth) {
        if (tr.getWidth(title) <= maxWidth) return title;
        String ellipsis = "...";
        int ellipsisW   = tr.getWidth(ellipsis);
        int maxContentW = maxWidth - ellipsisW;
        if (maxContentW <= 0) return ellipsis;
        int last = 0;
        for (int i = 0; i < title.length(); i++) {
            if (tr.getWidth(title.substring(0, i + 1)) > maxContentW) break;
            last = i + 1;
        }
        return title.substring(0, last) + ellipsis;
    }

    /**
     * Draws {@code text} at ({@code x}, {@code y}), coloring numeric substrings (integers and
     * decimals) with {@code numberColor} and all other characters with {@code baseColor}.
     *
     * <p>Uses {@link #NUMBER_SEGMENT_PATTERN} to split the string into alternating non-numeric
     * and numeric segments; each segment is rendered sequentially with the correct colour and
     * x-offset advanced by the rendered width of the previous segment.
     */
    private static void drawTextWithHighlightedNumbers(DrawContext context, TextRenderer tr,
                                                       String text, int x, int y,
                                                       int baseColor, int numberColor) {
        drawTextRangeWithNumberHighlight(context, tr, text, 0, text != null ? text.length() : 0,
                x, y, baseColor, numberColor);
    }

    private static boolean isSocketsSectionTitle(String sectionTitle) {
        if (sectionTitle == null) return false;
        String lower = sectionTitle.trim().toLowerCase(Locale.ROOT);
        return lower.equals("sockets") || lower.contains("socket");
    }

    private static boolean isSocketEntryLine(String trimmedLine) {
        return trimmedLine.startsWith("\u25C8") || trimmedLine.startsWith("\u25C7");
    }

    private static int drawAffixTextWithHighlights(DrawContext context, TextRenderer tr,
                                                    String text, int x, int y,
                                                    int baseColor, int numberColor,
                                                    int carryLeadingRomanColor,
                                                    boolean showBracketedText) {
        if (text == null || text.isEmpty()) return 0;

        int len = text.length();
        int[] colors = new int[len];
        java.util.Arrays.fill(colors, baseColor);

        Matcher numberMatcher = NUMBER_SEGMENT_PATTERN.matcher(text);
        while (numberMatcher.find()) {
            for (int i = numberMatcher.start(1); i < numberMatcher.end(1); i++) {
                colors[i] = numberColor;
            }
        }

        for (ColoredSpan span : findStatusEffectSpans(text)) {
            for (int i = Math.max(0, span.start()); i < Math.min(len, span.end()); i++) {
                colors[i] = span.color();
            }
        }

        if (carryLeadingRomanColor != 0) {
            paintLeadingRomanNumeral(colors, text, carryLeadingRomanColor);
        }

        boolean[] hidden = new boolean[len];
        for (BracketSpan span : findBracketSpans(text)) {
            for (int i = Math.max(0, span.start()); i < Math.min(len, span.end()); i++) {
                if (showBracketedText) {
                    colors[i] = withScaledAlpha(colors[i], 0.5f);
                } else {
                    hidden[i] = true;
                }
            }
        }

        int curX = x;
        int segStart = -1;
        int segColor = 0;
        for (int i = 0; i < len; i++) {
            if (hidden[i]) {
                if (segStart >= 0) {
                    curX = drawTextSegment(context, tr, text.substring(segStart, i), curX, y, segColor);
                    segStart = -1;
                }
                continue;
            }

            if (segStart < 0) {
                segStart = i;
                segColor = colors[i];
            } else if (colors[i] != segColor) {
                curX = drawTextSegment(context, tr, text.substring(segStart, i), curX, y, segColor);
                segStart = i;
                segColor = colors[i];
            }
        }
        if (segStart >= 0) {
            drawTextSegment(context, tr, text.substring(segStart), curX, y, segColor);
        }
        return trailingEffectColor(text);
    }

    private static int drawTextRangeWithNumberHighlight(DrawContext context, TextRenderer tr,
                                                         String text, int start, int end,
                                                         int x, int y,
                                                         int baseColor, int numberColor) {
        if (text == null || start >= end) return x;

        String segment = text.substring(start, end);
        Matcher m = NUMBER_SEGMENT_PATTERN.matcher(segment);
        int lastEnd = 0;
        int curX = x;

        while (m.find()) {
            if (m.start() > lastEnd) {
                curX = drawTextSegment(context, tr, segment.substring(lastEnd, m.start()), curX, y, baseColor);
            }
            curX = drawTextSegment(context, tr, m.group(1), curX, y, numberColor);
            lastEnd = m.end();
        }

        if (lastEnd < segment.length()) {
            curX = drawTextSegment(context, tr, segment.substring(lastEnd), curX, y, baseColor);
        }

        return curX;
    }

    private static int drawTextSegment(DrawContext context, TextRenderer tr, String seg, int x, int y, int color) {
        if (seg == null || seg.isEmpty()) return x;
        context.drawText(tr, Text.literal(seg), x, y, color, true);
        return x + tr.getWidth(seg);
    }

    private static List<BracketSpan> findBracketSpans(String text) {
        if (text == null || text.isEmpty()) return List.of();

        List<BracketSpan> spans = new ArrayList<>();
        int from = 0;
        while (from < text.length()) {
            int open = text.indexOf('[', from);
            if (open < 0) break;

            int close = text.indexOf(']', open + 1);
            if (close < 0) break;

            spans.add(new BracketSpan(open, close + 1));
            from = close + 1;
        }
        return spans;
    }

    private static int withScaledAlpha(int color, float factor) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) alpha = 0xFF;
        int scaled = Math.max(0, Math.min(255, Math.round(alpha * factor)));
        return (color & 0x00FFFFFF) | (scaled << 24);
    }

    private static void paintLeadingRomanNumeral(int[] colors, String text, int color) {
        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        int start = i;
        while (i < text.length() && isRomanNumeralChar(text.charAt(i))) i++;
        if (i <= start) return;
        if (i < text.length() && Character.isLetter(text.charAt(i))) return;

        for (int p = start; p < i; p++) {
            colors[p] = color;
        }
    }

    private static int trailingEffectColor(String text) {
        if (text == null || text.isEmpty()) return 0;

        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) end--;
        if (end <= 0) return 0;

        String lower = text.substring(0, end).toLowerCase(Locale.ROOT);
        for (EffectNameEntry entry : getStatusEffectEntries()) {
            String name = entry.lowerName();
            if (!lower.endsWith(name)) continue;

            int start = end - name.length();
            if (!isWordBoundary(text, start - 1) || !isWordBoundary(text, end)) continue;
            return entry.color();
        }

        return 0;
    }

    private static List<ColoredSpan> findStatusEffectSpans(String text) {
        if (text == null || text.isEmpty()) return List.of();

        String lower = text.toLowerCase(Locale.ROOT);
        List<ColoredSpan> spans = new ArrayList<>();
        for (EffectNameEntry entry : getStatusEffectEntries()) {
            int from = 0;
            while (true) {
                int idx = lower.indexOf(entry.lowerName(), from);
                if (idx < 0) break;
                int end = idx + entry.lowerName().length();
                from = idx + 1;

                if (!isWordBoundary(text, idx - 1) || !isWordBoundary(text, end)) {
                    continue;
                }

                int spanEnd = extendRomanNumeralSuffix(text, end);
                if (!overlapsExistingSpan(spans, idx, spanEnd)) {
                    spans.add(new ColoredSpan(idx, spanEnd, entry.color()));
                }
            }
        }

        if (spans.isEmpty()) return List.of();
        spans.sort(Comparator.comparingInt(ColoredSpan::start));
        return spans;
    }

    private static List<EffectNameEntry> getStatusEffectEntries() {
        List<EffectNameEntry> cached = STATUS_EFFECT_ENTRIES;
        if (cached != null) return cached;

        List<EffectNameEntry> built = new ArrayList<>();
        for (StatusEffect effect : Registries.STATUS_EFFECT) {
            String name = Text.translatable(effect.getTranslationKey()).getString();
            if (name == null) continue;
            String normalized = name.trim();
            if (normalized.isEmpty()) continue;

            built.add(new EffectNameEntry(normalized.toLowerCase(Locale.ROOT), colorForCategory(effect.getCategory())));
        }

        built.sort((a, b) -> Integer.compare(b.lowerName().length(), a.lowerName().length()));
        STATUS_EFFECT_ENTRIES = built;
        return built;
    }

    private static int colorForCategory(StatusEffectCategory category) {
        if (category == StatusEffectCategory.BENEFICIAL) return AFFIX_POSITIVE_COLOR;
        if (category == StatusEffectCategory.HARMFUL) return AFFIX_NEGATIVE_COLOR;
        return AFFIX_NEUTRAL_COLOR;
    }

    private static boolean overlapsExistingSpan(List<ColoredSpan> spans, int start, int end) {
        for (ColoredSpan span : spans) {
            if (start < span.end() && end > span.start()) return true;
        }
        return false;
    }

    private static boolean isWordBoundary(String text, int index) {
        if (index < 0 || index >= text.length()) return true;
        char c = text.charAt(index);
        return !Character.isLetterOrDigit(c);
    }

    private static int extendRomanNumeralSuffix(String text, int end) {
        int i = end;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        int romanStart = i;
        while (i < text.length() && isRomanNumeralChar(text.charAt(i))) i++;
        return i > romanStart ? i : end;
    }

    private static boolean isRomanNumeralChar(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'I', 'V', 'X', 'L', 'C', 'D', 'M' -> true;
            default -> false;
        };
    }

    private record InlineStatRow(String label, double value, double min, double max) {}

    private record EffectNameEntry(String lowerName, int color) {}

    private record ColoredSpan(int start, int end, int color) {}

    private record BracketSpan(int start, int end) {}

    private record AbilitySectionData(String header, List<String> lines) {}

    private TooltipRenderer() {}
}
