package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplytooltips.api.*;
import net.sweenus.simplytooltips.client.render.motif.BackgroundMotif;

import java.util.ArrayList;
import java.util.List;

/**
 * Main tooltip rendering pipeline.
 * Orchestrates layout calculations and delegates all drawing to the specialised helper classes.
 */
public class TooltipRenderer {

    private static final int PADDING       = 10;
    private static final int LINE_SPACING  = 1;
    private static final int MAX_TEXT_WIDTH = 200;

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

        // Resolve theme + motif: themeKey overrides the model's inline theme/borderStyle
        final TooltipTheme theme;
        final int          borderStyle;
        final String       resolvedMotifKey;
        if (model.themeKey() != null) {
            ThemeDefinition def = ThemeRegistry.get(model.themeKey());
            theme            = def.colors();
            String motif     = def.motif();
            borderStyle      = borderStyleFor(motif);
            resolvedMotifKey = "none".equals(motif) ? null : motif;
        } else {
            theme            = model.theme();
            borderStyle      = model.borderStyle();
            resolvedMotifKey = motifKeyFor(borderStyle);
        }

        List<String> badges = model.badges();

        long tooltipElapsedMs = TooltipAnimator.updateAndGetElapsed(stack, model.animKeyExtra());

        // ---- Text wrapping ----
        List<String> wrappedAbility = TooltipPainter.wrapStrings(model.abilityLines(), tr, MAX_TEXT_WIDTH);
        List<String> wrappedBody    = TooltipPainter.wrapStrings(model.bodyLines(), tr, MAX_TEXT_WIDTH);
        List<String> wrappedExtra   = new ArrayList<>();
        for (Text t : model.extraLines()) {
            wrappedExtra.addAll(TooltipPainter.wrapStrings(List.of(t.getString()), tr, MAX_TEXT_WIDTH));
        }

        // ---- Layout ----
        int lineHeight  = tr.fontHeight + LINE_SPACING;
        int upgradeRowH = lineHeight + 3;
        int sectionGap  = 4;
        int iconAreaW   = 36;
        int headerH     = PADDING + 16 + 6 + 12 + PADDING;
        int separatorH  = 10;

        boolean hasAbility = !wrappedAbility.isEmpty();
        boolean hasBody    = !wrappedBody.isEmpty();
        boolean hasExtra   = !wrappedExtra.isEmpty();

        UpgradeSection upgradeSection = model.upgradeSection();
        boolean hasUpgrade = upgradeSection != null;
        List<String> wrappedRuneEffect = hasUpgrade
                ? TooltipPainter.wrapStrings(upgradeSection.rune().effectLines(), tr, MAX_TEXT_WIDTH)
                : List.of();
        boolean hasBodyContent = hasAbility || hasUpgrade || hasBody || hasExtra;

        // Panel width
        int textContentW = 0;
        textContentW = Math.max(textContentW, tr.getWidth(model.title()) + iconAreaW + 4);
        for (String s : wrappedAbility) textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedBody)    textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedExtra)   textContentW = Math.max(textContentW, tr.getWidth(s));

        if (hasUpgrade) {
            int slotsW = tr.getWidth("\u25C7 ") + tr.getWidth("Slots  ") + 2
                       + upgradeSection.totalSlots() * 7;
            textContentW = Math.max(textContentW, slotsW);
            for (UpgradeRow row : upgradeSection.rows()) {
                int iconW    = tr.getWidth(row.icon() + " ");
                int lblW     = tr.getWidth(row.label() + "  ") + 2;
                int contentW = altDown
                    ? Math.min(iconW + lblW + tr.getWidth(row.altText()), MAX_TEXT_WIDTH)
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

        textContentW = Math.max(textContentW, 150);
        int panelW   = PADDING + textContentW + PADDING;

        // Panel height
        int bodyH = 0;
        if (hasAbility) {
            bodyH += lineHeight + sectionGap + wrappedAbility.size() * lineHeight;
        }
        if (hasAbility && hasUpgrade) bodyH += separatorH;
        if (hasUpgrade) {
            bodyH += lineHeight + sectionGap;              // "◆ Upgrades" header
            bodyH += upgradeRowH;                          // Slots row
            bodyH += upgradeRowH * upgradeSection.rows().size();
            bodyH += lineHeight;                           // spacer before rune
            bodyH += upgradeRowH + wrappedRuneEffect.size() * lineHeight;
        }
        if ((hasAbility || hasUpgrade) && hasBody) bodyH += separatorH;
        if (hasBody)  bodyH += wrappedBody.size() * lineHeight;
        if (hasExtra) bodyH += separatorH + wrappedExtra.size() * lineHeight;

        int footerH = 14;
        int panelH  = headerH + (hasBodyContent ? separatorH : 0) + bodyH + footerH;

        // Panel position (with screen-edge clamping)
        int panelX = x + 12;
        int panelY = y - 12;
        if (panelX + panelW > screenW - 6) panelX = x - panelW - 12;
        if (panelX < 6) panelX = 6;
        if (panelY + panelH > screenH - 6) panelY = screenH - panelH - 6;
        if (panelY < 6) panelY = 6;

        // ---- Draw background and border ----
        TooltipPainter.drawGradientBackground(context, panelX, panelY, panelW, panelH, theme);

        BackgroundMotif motif = MotifRegistry.get(resolvedMotifKey);
        if (motif != null) motif.draw(context, panelX, panelY, panelW, panelH, System.currentTimeMillis());

        BorderRenderer.drawDecorativeBorder(context, panelX, panelY, panelW, panelH, theme, borderStyle);

        int cursorY = panelY + PADDING;

        // ---- Header: item icon ----
        int iconFrameX = panelX + PADDING + 2;
        int iconFrameY = cursorY + 2;
        TooltipPainter.drawDiamondFrame(context, iconFrameX, iconFrameY, 24, theme);

        long  iconTimeMs    = System.currentTimeMillis();
        float breatheScale  = 1.0F + (float) Math.sin(iconTimeMs * 0.0042) * 0.055F;
        float spinDegrees   = (float) Math.sin(iconTimeMs * 0.0018) * 4.0F;
        float bobOffset     = (float) Math.sin(iconTimeMs * 0.0026 + 1.1) * 0.7F;

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
        int nameX = panelX + PADDING + iconAreaW;
        int nameY = cursorY + 4;
        TooltipPainter.drawWaveText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);

        int badgeY = cursorY + 4 + tr.fontHeight + 3;
        if (badges != null && !badges.isEmpty()) {
            int badgeX = nameX;
            for (int bi = 0; bi < badges.size(); bi++) {
                badgeX = TooltipPainter.drawBadge(context, tr, badges.get(bi), badgeX, badgeY, theme);
                if (bi < badges.size() - 1) badgeX += 4;
            }
        }

        cursorY = panelY + headerH;

        // ---- Separator after header ----
        if (hasBodyContent) {
            TooltipPainter.drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // ---- Ability / Description section ----
        if (hasAbility) {
            context.drawText(tr,
                    Text.literal("\u25C6 Description").setStyle(Style.EMPTY.withColor(
                            TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                    panelX + PADDING, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;
            for (String line : wrappedAbility) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                        panelX + PADDING, cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // ---- Separator between ability and upgrades ----
        if (hasAbility && hasUpgrade) {
            TooltipPainter.drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // ---- Upgrade section ----
        if (hasUpgrade) {
            int leftX = panelX + PADDING;
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
        if ((hasAbility || hasUpgrade) && hasBody) {
            TooltipPainter.drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // ---- Body lines ----
        if (hasBody) {
            for (String line : wrappedBody) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                        panelX + PADDING, cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // ---- Extra lines (enchantments, durability, etc.) ----
        if (hasExtra) {
            TooltipPainter.drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
            int extraColor = TooltipPainter.lerpColor(theme.body(), 0xFFB8C2CF, 0.30f);
            for (String line : wrappedExtra) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(
                                TextColor.fromRgb(extraColor & 0x00FFFFFF))),
                        panelX + PADDING, cursorY, extraColor, true);
                cursorY += lineHeight;
            }
        }

        // ---- Footer dots ----
        TooltipPainter.drawFooterDots(context, panelX + panelW / 2, panelY + panelH - 8, theme);

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
            default          -> TooltipBorderStyle.DEFAULT;
        };
    }

    private TooltipRenderer() {}
}
