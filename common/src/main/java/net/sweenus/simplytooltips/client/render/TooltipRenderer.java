package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplytooltips.api.*;
import net.sweenus.simplytooltips.client.TooltipNavigationConfig;
import net.sweenus.simplytooltips.client.render.motif.BackgroundMotif;
import net.sweenus.simplytooltips.config.SimplyTooltipsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main tooltip rendering pipeline.
 * Orchestrates layout calculations and delegates all drawing to the specialised helper classes.
 */
public class TooltipRenderer {

    private static final Pattern INLINE_STAT_PATTERN = Pattern.compile(
            "^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s+(Attack Damage|Attack Speed|Attack Range)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final String STAT_LABEL_REFERENCE = "Attack Damage";
    private static final int STAT_BAR_MIN_WIDTH = 52;
    private static final int STAT_BAR_HEIGHT = 4;

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
        List<String> wrappedAbility = TooltipPainter.wrapStrings(model.abilityLines(), tr, maxTextWidth());
        List<String> wrappedBody    = TooltipPainter.wrapStrings(model.bodyLines(), tr, maxTextWidth());
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

        // ---- Tab and scroll state ----
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        ScrollState.notifyItem(itemId);

        List<TabState.Tab> availableTabs = new ArrayList<>();
        if (TooltipNavigationConfig.tooltipTabs()) {
            if (!wrappedAbility.isEmpty())                                    availableTabs.add(TabState.Tab.LORE);
            if (model.upgradeSection() != null)                               availableTabs.add(TabState.Tab.FORGE);
            if (!wrappedBody.isEmpty() || !wrappedExtra.isEmpty())            availableTabs.add(TabState.Tab.STATS);
            TabState.notifyItem(itemId, availableTabs);
        }

        boolean tabsActive = TooltipNavigationConfig.tooltipTabs() && TabState.multiTab();
        boolean drawLore   = !tabsActive || TabState.activeTab() == TabState.Tab.LORE;
        boolean drawForge  = !tabsActive || TabState.activeTab() == TabState.Tab.FORGE;
        boolean drawStats  = !tabsActive || TabState.activeTab() == TabState.Tab.STATS;

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
        boolean hasBody    = !wrappedBody.isEmpty();
        boolean hasExtra   = !wrappedExtra.isEmpty();

        UpgradeSection upgradeSection = model.upgradeSection();
        boolean hasUpgrade = upgradeSection != null;
        List<String> wrappedRuneEffect = hasUpgrade
                ? TooltipPainter.wrapStrings(upgradeSection.rune().effectLines(), tr, maxTextWidth())
                : List.of();
        boolean hasBodyContent = (hasAbility && drawLore)
                || (hasUpgrade && drawForge)
                || ((hasBody || hasExtra) && drawStats);

        // Panel width
        int textContentW = 0;
        textContentW = Math.max(textContentW, tr.getWidth(model.title()) + iconAreaW + 4);
        for (String s : wrappedAbility) {
            // Strip SECTION_MARKER and add "◆ " prefix for accurate width measurement
            String measured = s.startsWith(ModernTooltipModel.SECTION_MARKER)
                    ? "\u25C6 " + s.substring(ModernTooltipModel.SECTION_MARKER.length()) : s;
            textContentW = Math.max(textContentW, tr.getWidth(measured));
        }
        for (int i = 0; i < wrappedBody.size(); i++) {
            InlineStatRow stat = bodyStats.get(i);
            if (stat != null) {
                int statW = statRowMinWidth(tr, stat);
                textContentW = Math.max(textContentW, statW);
            } else {
                textContentW = Math.max(textContentW, tr.getWidth(wrappedBody.get(i)));
            }
        }
        for (int i = 0; i < wrappedExtra.size(); i++) {
            InlineStatRow stat = extraStats.get(i);
            if (stat != null) {
                int statW = statRowMinWidth(tr, stat);
                textContentW = Math.max(textContentW, statW);
            } else {
                textContentW = Math.max(textContentW, tr.getWidth(wrappedExtra.get(i)));
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
            for (String line : wrappedAbility) {
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    bodyH += separatorH + lineHeight + sectionGap; // separator + sub-header
                } else {
                    bodyH += lineHeight;
                }
            }
        }
        if (hasAbility && drawLore && hasUpgrade && drawForge) bodyH += separatorH;
        if (hasUpgrade && drawForge) {
            bodyH += lineHeight + sectionGap;              // "◆ Upgrades" header
            bodyH += upgradeRowH;                          // Slots row
            bodyH += upgradeRowH * upgradeSection.rows().size();
            bodyH += lineHeight;                           // spacer before rune
            bodyH += upgradeRowH + wrappedRuneEffect.size() * lineHeight;
        }
        if ((hasAbility && drawLore || hasUpgrade && drawForge) && hasBody && drawStats) bodyH += separatorH;
        if (hasBody && drawStats)  bodyH += wrappedBody.size() * lineHeight;
        if (hasExtra && drawStats) bodyH += separatorH + wrappedExtra.size() * lineHeight;

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
        switch (titleAnimStyle != null ? titleAnimStyle : "wave") {
            case "shimmer" -> TooltipPainter.drawShimmerText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
            case "pulse"   -> TooltipPainter.drawPulseText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
            case "flicker" -> TooltipPainter.drawFlickerText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
            case "shiver", "shivering" -> TooltipPainter.drawShiverText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
            case "quiver"  -> TooltipPainter.drawQuiverText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
            case "breathe_spin_bob" -> TooltipPainter.drawBreatheSpinBobText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
            case "drop_bounce" -> TooltipPainter.drawDropBounceText(context, tr, model.title(), nameX, nameY, theme.name(), tooltipElapsedMs);
            case "hinge_fall" -> {
                // Clip title animation to tooltip bounds so off-panel motion stays hidden.
                context.enableScissor(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1);
                TooltipPainter.drawHingeFallText(context, tr, model.title(), nameX, nameY, theme.name(), tooltipElapsedMs);
                context.disableScissor();
            }
            case "obfuscate" -> TooltipPainter.drawObfuscateText(context, tr, model.title(), nameX, nameY, theme.name(), tooltipElapsedMs);
            case "static"  -> context.drawText(tr, Text.literal(model.title()).setStyle(
                                  Style.EMPTY.withColor(TextColor.fromRgb(theme.name() & 0x00FFFFFF))),
                                  nameX, nameY, theme.name(), true);
            default        -> TooltipPainter.drawWaveText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);
        }

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
                    Text.literal("\u25C6 Description").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                    panelX + padding(), cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;
            for (String line : wrappedAbility) {
                if (line.startsWith(ModernTooltipModel.SECTION_MARKER)) {
                    // Sub-section header: separator + "◆ " label in sectionHeader colour
                    String headerText = "\u25C6 " + line.substring(ModernTooltipModel.SECTION_MARKER.length());
                    TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
                    cursorY += separatorH;
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
                }
            }
        }

        // ---- Separator between ability and upgrades ----
        if (hasAbility && drawLore && hasUpgrade && drawForge) {
            TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
            cursorY += separatorH;
        }

        // ---- Upgrade section ----
        if (hasUpgrade && drawForge) {
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

            // Rune row
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
            for (String effectLine : wrappedRuneEffect) {
                context.drawText(tr,
                        Text.literal(effectLine).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(runeDescColor & 0x00FFFFFF))),
                        leftX, cursorY, runeDescColor, false);
                cursorY += lineHeight;
            }
        }

        // ---- Separator between (ability/upgrades) and body ----
        if ((hasAbility && drawLore || hasUpgrade && drawForge) && hasBody && drawStats) {
            TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
            cursorY += separatorH;
        }

        // ---- Body lines ----
        if (hasBody && drawStats) {
            int contentLeft = panelX + padding();
            int contentRight = panelX + panelW - padding();
            for (int i = 0; i < wrappedBody.size(); i++) {
                String line = wrappedBody.get(i);
                InlineStatRow stat = bodyStats.get(i);
                if (stat != null) {
                    drawInlineStatRow(context, tr, stat, contentLeft, contentRight, cursorY, theme);
                } else {
                    context.drawText(tr,
                            Text.literal(line).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                            contentLeft, cursorY, theme.body(), true);
                }
                cursorY += lineHeight;
            }
        }

        // ---- Extra lines (enchantments, durability, etc.) ----
        if (hasExtra && drawStats) {
            TooltipPainter.drawSeparator(context, panelX + padding(), cursorY, panelW - padding() * 2, theme);
            cursorY += separatorH;
            int extraColor = TooltipPainter.lerpColor(theme.body(), 0xFFB8C2CF, 0.30f);
            int contentLeft = panelX + padding();
            int contentRight = panelX + panelW - padding();
            for (int i = 0; i < wrappedExtra.size(); i++) {
                String line = wrappedExtra.get(i);
                InlineStatRow stat = extraStats.get(i);
                if (stat != null) {
                    drawInlineStatRow(context, tr, stat, contentLeft, contentRight, cursorY, theme);
                } else {
                    context.drawText(tr,
                            Text.literal(line).setStyle(Style.EMPTY.withColor(
                                    TextColor.fromRgb(extraColor & 0x00FFFFFF))),
                            contentLeft, cursorY, extraColor, true);
                }
                cursorY += lineHeight;
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
            TooltipPainter.drawTabDots(context, panelX + panelW / 2, panelY + panelH - 8,
                    TabState.tabs(), TabState.activeTab(), theme);
        } else {
            TooltipPainter.drawFooterDots(context, panelX + panelW / 2, panelY + panelH - 8, theme);
        }

        // Record whether this frame had an active scroll region (used by MOUSE_SCROLLED to consume the event).
        ScrollState.setScrollableActive(scrollActive);

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
            default          -> TooltipBorderStyle.DEFAULT;
        };
    }

    /**
     * Resolves the {@link ThemeDefinition} to use for this tooltip using the 4-level priority chain:
     * <ol>
     *   <li>Provider-supplied {@code model.themeKey()} (e.g. Simply Bows bow themes)</li>
     *   <li>Per-item or per-tag mapping from {@link ItemThemeRegistry}</li>
     *   <li>Vanilla item rarity (common / uncommon / rare / epic)</li>
     *   <li>Registry default (golden fallback if a rarity key is somehow missing)</li>
     * </ol>
     */
    private static ThemeDefinition resolveTheme(ItemStack stack, ModernTooltipModel model) {
        // 1. Provider-supplied override
        if (model.themeKey() != null)
            return ThemeRegistry.get(model.themeKey());

        // 2. Data-driven item / tag mapping
        String dataKey = ItemThemeRegistry.resolveForStack(stack);
        if (dataKey != null)
            return ThemeRegistry.get(dataKey);

        // 3. Rarity fallback
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
            case "attack damage" -> new InlineStatRow("Attack Damage", value, 0.0, 14.0);
            case "attack speed"  -> new InlineStatRow("Attack Speed",  value, 0.0, 3.0);
            case "attack range"  -> new InlineStatRow("Attack Range",  value, 0.0, 5.0);
            default -> null;
        };
    }

    private static String formatStatValue(double value) {
        return AttributeModifiersComponent.DECIMAL_FORMAT.format(value);
    }

    private static void drawInlineStatRow(DrawContext context, TextRenderer tr, InlineStatRow stat,
                                          int contentLeft, int contentRight, int y, TooltipTheme theme) {
        int labelColumnW = statLabelColumnWidth(tr);
        int valueColumnW = statValueColumnWidth(tr);
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

    private static int statRowMinWidth(TextRenderer tr, InlineStatRow stat) {
        return statLabelColumnWidth(tr) + 8 + STAT_BAR_MIN_WIDTH + 8 + statValueColumnWidth(tr);
    }

    private static int statLabelColumnWidth(TextRenderer tr) {
        return tr.getWidth(STAT_LABEL_REFERENCE);
    }

    private static int statValueColumnWidth(TextRenderer tr) {
        int w = 0;
        w = Math.max(w, tr.getWidth(formatStatValue(0.0)));
        w = Math.max(w, tr.getWidth(formatStatValue(5.0)));
        w = Math.max(w, tr.getWidth(formatStatValue(10.0)));
        w = Math.max(w, tr.getWidth(formatStatValue(32.0)));
        return w;
    }

    private record InlineStatRow(String label, double value, double min, double max) {}

    private TooltipRenderer() {}
}
