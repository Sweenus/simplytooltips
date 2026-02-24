package net.sweenus.simplytooltips.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import net.sweenus.simplytooltips.api.TooltipTheme;
import net.sweenus.simplytooltips.api.UpgradeRow;
import net.sweenus.simplytooltips.api.UpgradeRune;
import net.sweenus.simplytooltips.api.UpgradeSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Unique private static final int PADDING = 10;
    @Unique private static final int LINE_SPACING = 1;
    @Unique private static final int MAX_TEXT_WIDTH = 200;
    @Unique private static final int BORDER_STYLE_DEFAULT = 0;
    @Unique private static final int BORDER_STYLE_VINE = 1;
    @Unique private static final int BORDER_STYLE_BEE = 2;
    @Unique private static final int BORDER_STYLE_BLOSSOM = 3;
    @Unique private static final int BORDER_STYLE_BUBBLE = 4;
    @Unique private static final int BORDER_STYLE_EARTH = 5;
    @Unique private static final int BORDER_STYLE_ECHO = 6;
    @Unique private static final int BORDER_STYLE_ICE = 7;
    @Unique private static final int BORDER_STYLE_LIGHTNING = 8;
    @Unique private static final long TOOLTIP_ANIM_RESET_GAP_MS = 180L;
    @Unique private static final float UPGRADE_PIP_ANIM_START_SCALE = 0.28F;
    @Unique private static final float UPGRADE_PIP_ANIM_OVERSHOOT_SCALE = 1.40F;
    @Unique private static final long UPGRADE_PIP_ANIM_GROW_MS = 70L;
    @Unique private static final long UPGRADE_PIP_ANIM_SETTLE_MS = 85L;
    @Unique private static final long UPGRADE_PIP_ANIM_STAGGER_MS = 32L;

    @Unique
    private static final TooltipTheme SIMPLYTOOLTIPS_DEFAULT_THEME = new TooltipTheme(
            0xFFE2A834, 0xFF8A6A1E, 0xF02E2210, 0xF0181208,
            0xFFFFF0CC, 0xFFEEEEEE, 0xFF141008, 0xFFFFD5A0,
            0xFFE6ECF5, 0xFF8A6A1E, 0xFFE2A834, 0xFF2A1E0A,
            0xFF8A6A1E, 0xFF9D62CA, 0xFF5E8ACF, 0xFFDB5E71,
            0xFFE2A834, 0xFF3D3020, 0xFFC7D2E2
    );

    @Unique private static ItemStack simplytooltips$lastRealStack = ItemStack.EMPTY;
    @Unique private static String simplytooltips$tooltipAnimKey = "";
    @Unique private static long simplytooltips$tooltipAnimStartMs = 0L;
    @Unique private static long simplytooltips$tooltipAnimLastFrameMs = 0L;

    // --- Theme lookup helpers (available for providers / future use) ---

    @Unique
    private static TooltipTheme simplytooltips$getTheme(String themeKey) {
        return switch (themeKey) {
            case "vine" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFF7CCB88, 0xFF3A6A43, 0xF01D2F1F, 0xF0122016,
                    0xFFEAF9E9, 0xFFE8F3E8, 0xFF102114, 0xFFBDECBF,
                    0xFFD9EFDA, 0xFF4D8C57, 0xFF7CCB88, 0xFF173120,
                    0xFF4D8C57, 0xFF86D59A, 0xFF6FA9E6, 0xFFC67AB4,
                    0xFF8FD79B, 0xFF243629, 0xFFBBD8C0
            ));
            case "echo" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFF8F7CFF, 0xFF4A3D8A, 0xF01D1738, 0xF0120F24,
                    0xFFF2EDFF, 0xFFECE9FF, 0xFF130F22, 0xFFC9C0FF,
                    0xFFE2DDF8, 0xFF6357B3, 0xFF8F7CFF, 0xFF201A3F,
                    0xFF6357B3, 0xFF9E8BFF, 0xFF76AAFF, 0xFFCE84E9,
                    0xFF9C90FF, 0xFF282246, 0xFFC8C2EC
            ));
            case "bee" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFFF5C64E, 0xFF8A6B22, 0xF030280E, 0xF01C1808,
                    0xFFFFF2CF, 0xFFF7EECC, 0xFF181206, 0xFFFFE19A,
                    0xFFF2E9CF, 0xFFA7832F, 0xFFF5C64E, 0xFF2A210B,
                    0xFFA7832F, 0xFFE1B342, 0xFFC99A3A, 0xFFF08E53,
                    0xFFF5C64E, 0xFF2A2417, 0xFFD7CAA8
            ));
            case "blossom" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFFF1A3C7, 0xFF8E4D66, 0xF0321B27, 0xF01E1118,
                    0xFFFFEEF6, 0xFFF8EAF1, 0xFF1D1117, 0xFFF7C7DB,
                    0xFFF2DEE7, 0xFFB06A8A, 0xFFF1A3C7, 0xFF351A26,
                    0xFFB06A8A, 0xFFD998C1, 0xFFB19AE8, 0xFFE37EA7,
                    0xFFEFA9CB, 0xFF33202A, 0xFFDCC2CE
            ));
            case "bubble" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFF66D5E5, 0xFF2E7284, 0xF0132B33, 0xF00C1A20,
                    0xFFE8FAFF, 0xFFE7F4F8, 0xFF0D1A1E, 0xFFA7ECF7,
                    0xFFD8EDF2, 0xFF4697A8, 0xFF66D5E5, 0xFF13303A,
                    0xFF4697A8, 0xFF7AE1EA, 0xFF7DBBEB, 0xFF78CDE0,
                    0xFF78DCE8, 0xFF1B3138, 0xFFBCD7DD
            ));
            case "earth" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFFD0AF7A, 0xFF7B6138, 0xF0302518, 0xF01B140C,
                    0xFFFFF1DD, 0xFFF5EDDE, 0xFF181208, 0xFFEACD9E,
                    0xFFEDDFC8, 0xFF9A7A49, 0xFFD0AF7A, 0xFF2A2114,
                    0xFF9A7A49, 0xFFC79C75, 0xFFA09CCC, 0xFFD49870,
                    0xFFD8B783, 0xFF2F261B, 0xFFD2C2AA
            ));
            case "ice" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFF9BD9FF, 0xFF4D7894, 0xF0152733, 0xF00D1921,
                    0xFFF0FAFF, 0xFFEAF4FA, 0xFF0E181D, 0xFFC0E9FF,
                    0xFFDDEDF8, 0xFF6EA5C4, 0xFF9BD9FF, 0xFF1A2F3B,
                    0xFF6EA5C4, 0xFFA5DFFF, 0xFF8AB9F7, 0xFF9ECBEA,
                    0xFFA8E0FF, 0xFF22333E, 0xFFC8DCE9
            ));
            case "lightning" -> simplytooltips$withDefaultTextColors(new TooltipTheme(
                    0xFFE6EEFF, 0xFF6A7DAA, 0xF0141D2E, 0xF00B111D,
                    0xFFF4F7FF, 0xFFDCE8FF, 0xFF0A0F18, 0xFFD4E0FF,
                    0xFFE2E9FA, 0xFF859CD6, 0xFFE6EEFF, 0xFF152036,
                    0xFF859CD6, 0xFFB8C8FF, 0xFF8DB8FF, 0xFF9FB1FF,
                    0xFFC6D8FF, 0xFF232D40, 0xFFD4DEEF
            ));
            default -> SIMPLYTOOLTIPS_DEFAULT_THEME;
        };
    }

    @Unique
    private static int simplytooltips$getBorderStyle(String themeKey) {
        return switch (themeKey) {
            case "vine" -> BORDER_STYLE_VINE;
            case "bee" -> BORDER_STYLE_BEE;
            case "blossom" -> BORDER_STYLE_BLOSSOM;
            case "bubble" -> BORDER_STYLE_BUBBLE;
            case "earth" -> BORDER_STYLE_EARTH;
            case "echo" -> BORDER_STYLE_ECHO;
            case "ice" -> BORDER_STYLE_ICE;
            case "lightning" -> BORDER_STYLE_LIGHTNING;
            default -> BORDER_STYLE_DEFAULT;
        };
    }

    @Unique
    private static TooltipTheme simplytooltips$withDefaultTextColors(TooltipTheme base) {
        return new TooltipTheme(
                base.border(),
                base.borderInner(),
                base.bgTop(),
                base.bgBottom(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.name(),
                base.badgeBg(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.badgeCutout(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.sectionHeader(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.body(),
                base.separator(),
                base.diamondFrame(),
                base.diamondFrameInner(),
                base.footerDot(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.stringColor(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.frameColor(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.runeColor(),
                base.slotFilled(),
                base.slotEmpty(),
                SIMPLYTOOLTIPS_DEFAULT_THEME.hint()
        );
    }

    // --- Injection points ---

    @Inject(method = "drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void simplytooltips$drawModernTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack == null || stack.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        simplytooltips$lastRealStack = stack;

        List<Text> raw = Screen.getTooltipFromItem(client, stack);
        if (raw == null || raw.isEmpty()) return;

        Optional<TooltipProvider> provider = TooltipProviderRegistry.find(stack);
        if (provider.isEmpty()) return;

        simplytooltips$renderModernTooltip(
                (DrawContext) (Object) this, textRenderer, stack, raw, provider.get(),
                x, y, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        ci.cancel();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void simplytooltips$drawModernTooltipFromLines(TextRenderer textRenderer, List<Text> text, java.util.Optional<?> data, int x, int y, CallbackInfo ci) {
        simplytooltips$tryRenderFromLines(textRenderer, text, x, y, ci);
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void simplytooltips$drawModernTooltipFromSimpleLines(TextRenderer textRenderer, List<Text> text, int x, int y, CallbackInfo ci) {
        simplytooltips$tryRenderFromLines(textRenderer, text, x, y, ci);
    }

    @Unique
    private void simplytooltips$tryRenderFromLines(TextRenderer textRenderer, List<Text> text, int x, int y, CallbackInfo ci) {
        if (text == null || text.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        String title = text.get(0).getString();
        ItemStack resolved = simplytooltips$findRealStack(client, title);
        if (resolved.isEmpty()) return;

        Optional<TooltipProvider> provider = TooltipProviderRegistry.find(resolved);
        if (provider.isEmpty()) return;

        simplytooltips$renderModernTooltip(
                (DrawContext) (Object) this, textRenderer, resolved, text, provider.get(),
                x, y, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        ci.cancel();
    }

    @Unique
    private static ItemStack simplytooltips$findRealStack(MinecraftClient client, String title) {
        if (title == null || title.isBlank()) return ItemStack.EMPTY;

        // 1. Check cached stack from drawItemTooltip
        if (!simplytooltips$lastRealStack.isEmpty()
                && simplytooltips$lastRealStack.getName().getString().equals(title)) {
            return simplytooltips$lastRealStack;
        }

        // 2. Try to get the focused slot stack from HandledScreen
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            try {
                java.lang.reflect.Field focusedSlotField = null;
                for (java.lang.reflect.Field f : HandledScreen.class.getDeclaredFields()) {
                    if (f.getType() == Slot.class) {
                        focusedSlotField = f;
                        break;
                    }
                }
                if (focusedSlotField != null) {
                    focusedSlotField.setAccessible(true);
                    Slot slot = (Slot) focusedSlotField.get(handledScreen);
                    if (slot != null && slot.hasStack()) {
                        ItemStack slotStack = slot.getStack();
                        if (slotStack.getName().getString().equals(title)) {
                            return slotStack;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Check cursor stack (item held by mouse)
        if (client.player != null) {
            ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty() && cursorStack.getName().getString().equals(title)) {
                return cursorStack;
            }
        }

        // 4. Fallback: scan registry for name match (no component data)
        for (Item item : Registries.ITEM) {
            ItemStack candidate = new ItemStack(item);
            if (candidate.getName().getString().equals(title)) {
                return candidate;
            }
        }

        return ItemStack.EMPTY;
    }

    // --- Main tooltip renderer ---

    @Unique
    private static void simplytooltips$renderModernTooltip(
            DrawContext context, TextRenderer tr, ItemStack stack, List<Text> rawLines,
            TooltipProvider provider, int x, int y, int screenW, int screenH) {

        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, 400.0f);

        boolean altDown = false;
        try { altDown = Screen.hasAltDown(); } catch (Throwable ignored) {}

        ModernTooltipModel model = provider.build(stack, rawLines, altDown);

        TooltipTheme theme = model.theme();
        int borderStyle = model.borderStyle();
        List<String> badges = model.badges();

        long tooltipElapsedMs = simplytooltips$updateTooltipAnimAndGetElapsedMs(stack, model.animKeyExtra());

        List<String> wrappedAbility = simplytooltips$wrapStrings(model.abilityLines(), tr, MAX_TEXT_WIDTH);
        List<String> wrappedBody    = simplytooltips$wrapStrings(model.bodyLines(), tr, MAX_TEXT_WIDTH);
        List<String> wrappedExtra   = new ArrayList<>();
        for (Text t : model.extraLines()) {
            wrappedExtra.addAll(simplytooltips$wrapStrings(List.of(t.getString()), tr, MAX_TEXT_WIDTH));
        }

        // --- Layout calculations ---
        int lineHeight = tr.fontHeight + LINE_SPACING;
        int upgradeRowH = lineHeight + 3;
        int sectionGap = 4;
        int iconAreaW = 36;
        int headerH = PADDING + 16 + 6 + 12 + PADDING;
        int separatorH = 10;

        boolean hasAbility = !wrappedAbility.isEmpty();
        boolean hasBody    = !wrappedBody.isEmpty();
        boolean hasExtra   = !wrappedExtra.isEmpty();
        UpgradeSection upgradeSection = model.upgradeSection();
        boolean hasUpgrade = upgradeSection != null;
        List<String> wrappedRuneEffect = hasUpgrade
                ? simplytooltips$wrapStrings(upgradeSection.rune().effectLines(), tr, MAX_TEXT_WIDTH)
                : List.of();
        boolean hasBodyContent = hasAbility || hasUpgrade || hasBody || hasExtra;

        int textContentW = 0;
        textContentW = Math.max(textContentW, tr.getWidth(model.title()) + iconAreaW + 4);
        for (String s : wrappedAbility) textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedBody)    textContentW = Math.max(textContentW, tr.getWidth(s));
        for (String s : wrappedExtra)   textContentW = Math.max(textContentW, tr.getWidth(s));

        // Upgrade section widths
        if (hasUpgrade) {
            int slotsW = tr.getWidth("\u25C7 ") + tr.getWidth("Slots  ") + 2
                       + upgradeSection.totalSlots() * 7;
            textContentW = Math.max(textContentW, slotsW);
            for (UpgradeRow row : upgradeSection.rows()) {
                int iconW = tr.getWidth(row.icon() + " ");
                int lblW  = tr.getWidth(row.label() + "  ") + 2;
                int contentW = altDown
                    ? Math.min(iconW + lblW + tr.getWidth(row.altText()), MAX_TEXT_WIDTH)
                    : iconW + lblW + row.max() * 7;
                textContentW = Math.max(textContentW, contentW);
            }
            for (String line : wrappedRuneEffect)
                textContentW = Math.max(textContentW, tr.getWidth(line));
        }

        // Badge row width
        if (badges != null && !badges.isEmpty()) {
            int totalBadgeW = 0;
            for (int bi = 0; bi < badges.size(); bi++) {
                totalBadgeW += tr.getWidth(badges.get(bi)) + 6;
                if (bi < badges.size() - 1) totalBadgeW += 4;
            }
            textContentW = Math.max(textContentW, iconAreaW + totalBadgeW + 4);
        }

        textContentW = Math.max(textContentW, 150);

        int panelW = PADDING + textContentW + PADDING;

        int bodyH = 0;
        if (hasAbility) {
            bodyH += lineHeight + sectionGap + wrappedAbility.size() * lineHeight;
        }
        if (hasAbility && hasUpgrade) bodyH += separatorH;
        if (hasUpgrade) {
            bodyH += lineHeight + sectionGap; // "◆ Upgrades" header
            bodyH += upgradeRowH;             // Slots row
            bodyH += upgradeRowH * upgradeSection.rows().size(); // variable rows
            bodyH += lineHeight;              // spacer before rune
            bodyH += upgradeRowH + wrappedRuneEffect.size() * lineHeight;
        }
        if ((hasAbility || hasUpgrade) && hasBody) bodyH += separatorH;
        if (hasBody) {
            bodyH += wrappedBody.size() * lineHeight;
        }
        if (hasExtra) {
            bodyH += separatorH + wrappedExtra.size() * lineHeight;
        }

        int footerH = 14;
        int panelH = headerH + (hasBodyContent ? separatorH : 0) + bodyH + footerH;

        // Position tooltip
        int panelX = x + 12;
        int panelY = y - 12;
        if (panelX + panelW > screenW - 6) panelX = x - panelW - 12;
        if (panelX < 6) panelX = 6;
        if (panelY + panelH > screenH - 6) panelY = screenH - panelH - 6;
        if (panelY < 6) panelY = 6;

        // Draw background & border
        simplytooltips$drawGradientBackground(context, panelX, panelY, panelW, panelH, theme);
        simplytooltips$drawAmbientBackgroundMotif(context, panelX, panelY, panelW, panelH, borderStyle);
        simplytooltips$drawDecorativeBorder(context, panelX, panelY, panelW, panelH, theme, borderStyle);

        int cursorY = panelY + PADDING;

        // --- Header ---
        int iconFrameX = panelX + PADDING + 2;
        int iconFrameY = cursorY + 2;
        simplytooltips$drawDiamondFrame(context, iconFrameX, iconFrameY, 24, theme);

        long iconTimeMs = System.currentTimeMillis();
        float breatheScale = 1.0F + (float) Math.sin(iconTimeMs * 0.0042) * 0.055F;
        float spinDegrees  = (float) Math.sin(iconTimeMs * 0.0018) * 4.0F;
        float bobOffset    = (float) Math.sin(iconTimeMs * 0.0026 + 1.1) * 0.7F;

        context.getMatrices().push();
        float itemCenterX = iconFrameX + 12.0F;
        float itemCenterY = iconFrameY + 12.0F + bobOffset;
        context.getMatrices().translate(itemCenterX, itemCenterY, 0.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinDegrees));
        context.getMatrices().scale(breatheScale, breatheScale, 1.0F);
        context.getMatrices().translate(-8.0F, -8.0F, 0.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();

        int nameX = panelX + PADDING + iconAreaW;
        int nameY = cursorY + 4;
        simplytooltips$drawWaveText(context, tr, model.title(), nameX, nameY, theme.name(), iconTimeMs);

        int badgeY = cursorY + 4 + tr.fontHeight + 3;
        if (badges != null && !badges.isEmpty()) {
            int badgeX = nameX;
            for (int bi = 0; bi < badges.size(); bi++) {
                badgeX = simplytooltips$drawBadge(context, tr, badges.get(bi), badgeX, badgeY, theme);
                if (bi < badges.size() - 1) badgeX += 4;
            }
        }

        cursorY = panelY + headerH;

        // --- Separator after header ---
        if (hasBodyContent) {
            simplytooltips$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // --- Ability/Description section ---
        if (hasAbility) {
            context.drawText(tr,
                    Text.literal("\u25C6 Description").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                    panelX + PADDING, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;
            for (String line : wrappedAbility) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                        panelX + PADDING, cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // --- Separator between ability and upgrade section ---
        if (hasAbility && hasUpgrade) {
            simplytooltips$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // --- Upgrade section ---
        if (hasUpgrade) {
            int leftX = panelX + PADDING;
            context.drawText(tr,
                Text.literal("\u25C6 Upgrades").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.sectionHeader() & 0x00FFFFFF))),
                leftX, cursorY, theme.sectionHeader(), true);
            cursorY += lineHeight + sectionGap;

            int pipSequenceBase = 0;

            // Slots row (special fixed highlight)
            int slotLabelColor = simplytooltips$lerpColor(theme.body(), 0xFFFFFFFF, 0.10f);
            context.drawText(tr,
                Text.literal("\u25C7").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.slotFilled() & 0x00FFFFFF))),
                leftX, cursorY, theme.slotFilled(), false);
            int slotPipX = leftX + tr.getWidth("\u25C7 ");
            context.drawText(tr,
                Text.literal("Slots").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(slotLabelColor & 0x00FFFFFF))),
                slotPipX, cursorY, slotLabelColor, false);
            slotPipX += tr.getWidth("Slots  ") + 2;
            for (int i = 0; i < upgradeSection.totalSlots(); i++) {
                boolean isFilled = i < upgradeSection.usedSlots();
                int slotColor    = isFilled ? theme.slotFilled() : theme.slotEmpty();
                int topHighlight = isFilled ? 0xFFF5D060 : 0xFF4A4030;
                int seqIdx = isFilled ? (pipSequenceBase++) : -1;
                simplytooltips$drawAnimatedSquarePip(context, slotPipX, cursorY + 1, 5,
                    slotColor, topHighlight, tooltipElapsedMs, seqIdx, isFilled);
                slotPipX += 7;
            }
            cursorY += upgradeRowH;

            // Variable upgrade rows (String, Frame, etc.)
            for (UpgradeRow row : upgradeSection.rows()) {
                int labelColor = simplytooltips$lerpColor(row.pipColor(), 0xFFFFFFFF, 0.20f);
                int descColor  = simplytooltips$lerpColor(row.pipColor(), theme.body(), 0.50f);
                context.drawText(tr,
                    Text.literal(row.icon()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(row.pipColor() & 0x00FFFFFF))),
                    leftX, cursorY, row.pipColor(), false);
                int rowLabelX = leftX + tr.getWidth(row.icon() + " ");
                context.drawText(tr,
                    Text.literal(row.label()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(labelColor & 0x00FFFFFF))),
                    rowLabelX, cursorY, labelColor, false);
                int afterLabel = rowLabelX + tr.getWidth(row.label() + " ") + 2;
                if (altDown) {
                    context.drawText(tr,
                        Text.literal(row.altText()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(descColor & 0x00FFFFFF))),
                        afterLabel, cursorY, descColor, false);
                } else {
                    int rowPipX = afterLabel;
                    for (int i = 0; i < row.max(); i++) {
                        boolean filled = i < row.filled();
                        int pipColor = filled ? row.pipColor() : theme.slotEmpty();
                        int seqIdx   = filled ? (pipSequenceBase++) : -1;
                        simplytooltips$drawAnimatedPip(context, rowPipX, cursorY + 1,
                            pipColor, filled, tooltipElapsedMs, seqIdx);
                        rowPipX += 7;
                    }
                }
                cursorY += upgradeRowH;
            }

            cursorY += lineHeight; // spacer before rune

            // Rune row
            UpgradeRune rune = upgradeSection.rune();
            int runeLabelColor = simplytooltips$lerpColor(rune.runeColor(), 0xFFFFFFFF, 0.22f);
            int runeTextColor  = rune.isNone() ? 0xFF6A6060 : rune.runeColor();
            int runeDescColor  = rune.isNone() ? 0xFF6A6060
                : simplytooltips$lerpColor(rune.runeColor(), theme.body(), 0.45f);
            context.drawText(tr,
                Text.literal("\u25CE").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rune.runeColor() & 0x00FFFFFF))),
                leftX, cursorY, rune.runeColor(), false);
            int runeLabelX = leftX + tr.getWidth("\u25CE ");
            context.drawText(tr,
                Text.literal("Rune: ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeLabelColor & 0x00FFFFFF))),
                runeLabelX, cursorY, runeLabelColor, false);
            int runeValX = runeLabelX + tr.getWidth("Rune: ");
            context.drawText(tr,
                Text.literal(rune.runeName()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeTextColor & 0x00FFFFFF))),
                runeValX, cursorY, runeTextColor, false);
            cursorY += upgradeRowH;
            for (String effectLine : wrappedRuneEffect) {
                context.drawText(tr,
                    Text.literal(effectLine).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(runeDescColor & 0x00FFFFFF))),
                    leftX, cursorY, runeDescColor, false);
                cursorY += lineHeight;
            }
        }

        // --- Separator between (ability/upgrades) and body ---
        if ((hasAbility || hasUpgrade) && hasBody) {
            simplytooltips$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
        }

        // --- Body lines ---
        if (hasBody) {
            for (String line : wrappedBody) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.body() & 0x00FFFFFF))),
                        panelX + PADDING, cursorY, theme.body(), true);
                cursorY += lineHeight;
            }
        }

        // --- Extra lines (enchantments, mod-added lines, durability, etc.) ---
        if (hasExtra) {
            simplytooltips$drawSeparator(context, panelX + PADDING, cursorY, panelW - PADDING * 2, theme);
            cursorY += separatorH;
            int extraColor = simplytooltips$lerpColor(theme.body(), 0xFFB8C2CF, 0.30f);
            for (String line : wrappedExtra) {
                context.drawText(tr,
                        Text.literal(line).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(extraColor & 0x00FFFFFF))),
                        panelX + PADDING, cursorY, extraColor, true);
                cursorY += lineHeight;
            }
        }

        // --- Footer dots ---
        simplytooltips$drawFooterDots(context, panelX + panelW / 2, panelY + panelH - 8, theme);

        context.getMatrices().pop();
    }

    // --- Animation ---

    @Unique
    private static long simplytooltips$updateTooltipAnimAndGetElapsedMs(ItemStack stack, String animKeySuffix) {
        long now = System.currentTimeMillis();
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String key = itemId == null ? "unknown" : itemId.toString();
        if (animKeySuffix != null && !animKeySuffix.isEmpty()) key += animKeySuffix;

        boolean keyChanged = !key.equals(simplytooltips$tooltipAnimKey);
        boolean timedOut   = (now - simplytooltips$tooltipAnimLastFrameMs) > TOOLTIP_ANIM_RESET_GAP_MS;
        if (keyChanged || timedOut) {
            simplytooltips$tooltipAnimStartMs = now;
            simplytooltips$tooltipAnimKey = key;
        }
        simplytooltips$tooltipAnimLastFrameMs = now;
        return Math.max(0L, now - simplytooltips$tooltipAnimStartMs);
    }

    @Unique
    private static float simplytooltips$getUpgradePipScale(long tooltipElapsedMs, int pipIndex) {
        long localMs = tooltipElapsedMs - (long) pipIndex * UPGRADE_PIP_ANIM_STAGGER_MS;
        long totalAnimMs = UPGRADE_PIP_ANIM_GROW_MS + UPGRADE_PIP_ANIM_SETTLE_MS;

        if (localMs <= 0L) return UPGRADE_PIP_ANIM_START_SCALE;
        if (localMs >= totalAnimMs) return 1.0F;

        if (localMs < UPGRADE_PIP_ANIM_GROW_MS) {
            float t = localMs / (float) UPGRADE_PIP_ANIM_GROW_MS;
            return simplytooltips$lerpFloat(UPGRADE_PIP_ANIM_START_SCALE, UPGRADE_PIP_ANIM_OVERSHOOT_SCALE, simplytooltips$easeOutCubic(t));
        }

        float t = (localMs - UPGRADE_PIP_ANIM_GROW_MS) / (float) UPGRADE_PIP_ANIM_SETTLE_MS;
        return simplytooltips$lerpFloat(UPGRADE_PIP_ANIM_OVERSHOOT_SCALE, 1.0F, simplytooltips$easeOutQuad(t));
    }

    @Unique
    private static float simplytooltips$lerpFloat(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Unique
    private static float simplytooltips$easeOutQuad(float t) {
        float inv = 1.0F - t;
        return 1.0F - inv * inv;
    }

    @Unique
    private static float simplytooltips$easeOutCubic(float t) {
        float inv = 1.0F - t;
        return 1.0F - (inv * inv * inv);
    }

    // --- Pip drawing ---

    @Unique
    private static void simplytooltips$drawPip(DrawContext context, int x, int y, int color, boolean filled) {
        context.fill(x, y, x + 5, y + 5, color);
        if (filled) {
            int highlight = simplytooltips$lerpColor(color, 0xFFFFFFFF, 0.35f);
            context.fill(x, y, x + 5, y + 1, highlight);
        } else {
            context.fill(x, y, x + 5, y + 1, 0xFF4A4030);
        }
    }

    @Unique
    private static void simplytooltips$drawAnimatedPip(DrawContext context, int x, int y, int color, boolean filled, long tooltipElapsedMs, int pipIndex) {
        int topHighlight = filled ? simplytooltips$lerpColor(color, 0xFFFFFFFF, 0.35f) : 0xFF4A4030;
        simplytooltips$drawAnimatedSquarePip(context, x, y, 5, color, topHighlight, tooltipElapsedMs, pipIndex, filled);
    }

    @Unique
    private static void simplytooltips$drawAnimatedSquarePip(DrawContext context, int x, int y, int size, int bodyColor, int topHighlightColor, long tooltipElapsedMs, int pipIndex, boolean animate) {
        if (!animate) {
            context.fill(x, y, x + size, y + size, bodyColor);
            context.fill(x, y, x + size, y + 1, topHighlightColor);
            return;
        }

        float scale = simplytooltips$getUpgradePipScale(tooltipElapsedMs, pipIndex);
        if (Math.abs(scale - 1.0F) < 0.001F) {
            context.fill(x, y, x + size, y + size, bodyColor);
            context.fill(x, y, x + size, y + 1, topHighlightColor);
            return;
        }

        context.getMatrices().push();
        float cx = x + (size / 2.0F);
        float cy = y + (size / 2.0F);
        context.getMatrices().translate(cx, cy, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.getMatrices().translate(-(size / 2.0F), -(size / 2.0F), 0.0F);
        context.fill(0, 0, size, size, bodyColor);
        context.fill(0, 0, size, 1, topHighlightColor);
        context.getMatrices().pop();
    }

    // --- Background and border drawing ---

    @Unique
    private static void simplytooltips$drawGradientBackground(DrawContext context, int x, int y, int w, int h, TooltipTheme theme) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0.0F : (float) i / (float) (h - 1);
            int row = simplytooltips$lerpColor(theme.bgTop(), theme.bgBottom(), t);
            context.fill(x, y + i, x + w, y + i + 1, row);
        }
    }

    @Unique
    private static void simplytooltips$drawDecorativeBorder(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, int borderStyle) {
        context.fill(x, y, x + w, y + 1, theme.border());
        context.fill(x, y + h - 1, x + w, y + h, theme.border());
        context.fill(x, y, x + 1, y + h, theme.border());
        context.fill(x + w - 1, y, x + w, y + h, theme.border());

        context.fill(x + 1, y + 1, x + w - 1, y + 2, theme.borderInner());
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, theme.borderInner());
        context.fill(x + 1, y + 1, x + 2, y + h - 1, theme.borderInner());
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, theme.borderInner());

        simplytooltips$drawSmallDiamond(context, x + 6, y, theme.border());
        simplytooltips$drawSmallDiamond(context, x + w - 7, y, theme.border());
        simplytooltips$drawSmallDiamond(context, x + 6, y + h - 1, theme.border());
        simplytooltips$drawSmallDiamond(context, x + w - 7, y + h - 1, theme.border());

        simplytooltips$drawBorderPattern(context, x, y, w, h, theme, borderStyle);
    }

    @Unique
    private static void simplytooltips$drawAmbientBackgroundMotif(DrawContext context, int x, int y, int w, int h, int borderStyle) {
        if (w < 40 || h < 40) return;

        switch (borderStyle) {
            case BORDER_STYLE_BLOSSOM -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 6);
                for (int i = 0; i < 7; i++) {
                    double fallSpeed = 0.010 + i * 0.0012;
                    int travel = Math.max(10, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 19;
                    int py = minY + (int) (fallProgress % travel);
                    int lane = (i * 37 + i * i * 11) % spawnRange;
                    double swayA = Math.sin((time * 0.0009) + i * 2.1) * (w * 0.22);
                    double swayB = Math.sin((time * 0.0017) + i * 0.9) * 8.0;
                    int baseX = minX + lane + (int) (swayA + swayB);
                    int px = Math.max(minX, Math.min(maxX - 5, baseX));
                    int coarseSpin = (int) (fallProgress / 26.0);
                    int rotation = Math.floorMod(i * 3 + coarseSpin, 4);
                    simplytooltips$drawRotatedBlossomPetal(context, px, py, rotation);
                }
            }
            case BORDER_STYLE_ICE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 6);
                for (int i = 0; i < 8; i++) {
                    double fallSpeed = 0.010 + i * 0.0010;
                    int travel = Math.max(10, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 23;
                    int py = minY + (int) (fallProgress % travel);
                    int lane = (i * 41 + i * i * 7) % spawnRange;
                    double swayA = Math.sin((time * 0.0010) + i * 1.6) * (w * 0.08);
                    double swayB = Math.sin((time * 0.0021) + i * 0.8) * 2.0;
                    int baseX = minX + lane + (int) (swayA + swayB);
                    int px = Math.max(minX, Math.min(maxX - 4, baseX));
                    int rotation = Math.floorMod(i + (int) (fallProgress / 36.0), 4);
                    int sizeVariant = ((i * 17) + 5) % 3;
                    simplytooltips$drawRotatedSnowflake(context, px, py, rotation, sizeVariant);
                }
            }
            case BORDER_STYLE_VINE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 6);
                for (int i = 0; i < 6; i++) {
                    double fallSpeed = 0.009 + i * 0.0011;
                    int travel = Math.max(10, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 27;
                    int py = minY + (int) (fallProgress % travel);
                    int lane = (i * 43 + i * i * 13) % spawnRange;
                    double swayA = Math.sin((time * 0.0011) + i * 2.0) * (w * 0.20);
                    double swayB = Math.sin((time * 0.0024) + i * 1.3) * 7.0;
                    int baseX = minX + lane + (int) (swayA + swayB);
                    int px = Math.max(minX, Math.min(maxX - 5, baseX));
                    int rotation = Math.floorMod(i * 2 + (int) (fallProgress / 30.0), 4);
                    simplytooltips$drawRotatedVineLeaf(context, px, py, rotation);
                }
            }
            case BORDER_STYLE_EARTH -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spawnRange = Math.max(10, maxX - minX - 1);
                for (int i = 0; i < 16; i++) {
                    int speedBand = (i * 13 + 5) % 6;
                    double fallSpeed = 0.009 + speedBand * 0.0011 + i * 0.00015;
                    int travel = Math.max(8, maxY - minY);
                    double fallProgress = (time * fallSpeed) + i * 11;
                    int py = minY + (int) (fallProgress % travel);
                    int lane = (i * 29 + i * i * 5) % spawnRange;
                    double drift = Math.sin((time * 0.0016) + i * 1.1) * 2.0;
                    int sizeRoll = (i * 7 + 3) % 10;
                    int size = sizeRoll < 6 ? 1 : 2;
                    if (sizeRoll == 9) size = 3;
                    int px = Math.max(minX, Math.min(maxX - size, minX + lane + (int) drift));
                    int cA = ((i + ((int) fallProgress / 24)) & 1) == 0 ? 0x1CB08C66 : 0x168A6A4B;
                    int cB = 0x126E583F;
                    if (size == 1) {
                        context.fill(px, py, px + 1, py + 1, cA);
                    } else if (size == 2) {
                        context.fill(px, py, px + 2, py + 2, cA);
                        context.fill(px + 1, py, px + 2, py + 1, cB);
                    } else {
                        context.fill(px, py, px + 3, py + 2, cA);
                        context.fill(px + 1, py + 1, px + 3, py + 2, cB);
                    }
                }
            }
            case BORDER_STYLE_BUBBLE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spawnRange = Math.max(12, (maxX - minX) - 5);
                for (int i = 0; i < 8; i++) {
                    int speedSeed = ((i * 17) + 3) % 5;
                    double speedOffset = (speedSeed - 2) * 0.00008;
                    double riseSpeed = (0.008 + i * 0.0009) + speedOffset;
                    int travel = Math.max(10, maxY - minY);
                    double riseProgress = (time * riseSpeed) + i * 21;
                    int py = maxY - (int) (riseProgress % travel);
                    int lane = (i * 31 + i * i * 9) % spawnRange;
                    double wobble = Math.sin((time * 0.0015) + i * 1.4) * 6.0;
                    int px = Math.max(minX, Math.min(maxX - 4, minX + lane + (int) wobble));
                    context.fill(px, py, px + 2, py + 2, 0x148EE7F8);
                    context.fill(px + 1, py + 1, px + 4, py + 4, 0x0EC8F7FF);
                    context.fill(px + 1, py, px + 2, py + 1, 0x1AF2FDFF);
                }
            }
            case BORDER_STYLE_BEE -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spanX = Math.max(10, maxX - minX - 2);
                int spanY = Math.max(10, maxY - minY - 2);
                for (int i = 0; i < 7; i++) {
                    int baseX = minX + ((i * 47 + i * i * 7) % spanX);
                    int baseY = minY + ((i * 29 + i * i * 5) % spanY);
                    int px = baseX + (int) (Math.sin((time * 0.0030) + i * 1.8) * 5.0);
                    int py = baseY + (int) (Math.sin((time * 0.0024) + i * 1.2) * 3.0);
                    px = Math.max(minX, Math.min(maxX - 2, px));
                    py = Math.max(minY, Math.min(maxY - 2, py));
                    context.fill(px, py, px + 1, py + 1, 0x20F2C74A);
                    context.fill(px + 1, py, px + 2, py + 1, 0x167A5A22);
                }
            }
            case BORDER_STYLE_ECHO -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spanX = Math.max(10, maxX - minX - 3);
                int spanY = Math.max(10, maxY - minY - 3);
                for (int i = 0; i < 7; i++) {
                    int laneX = minX + ((i * 37 + i * i * 9) % spanX);
                    int laneY = minY + ((i * 23 + i * i * 11) % spanY);
                    int px = laneX + (int) (Math.sin((time * 0.0019) + i * 1.5) * 7.0);
                    int py = laneY + (int) (Math.sin((time * 0.0022) + i * 1.1) * 4.0);
                    px = Math.max(minX, Math.min(maxX - 3, px));
                    py = Math.max(minY, Math.min(maxY - 3, py));
                    int pulse = (((int) (time / 240L) + i) & 1);
                    int runeA = pulse == 0 ? 0x18B59AFF : 0x12B59AFF;
                    int runeB = pulse == 0 ? 0x167C67D9 : 0x107C67D9;
                    context.fill(px, py + 1, px + 3, py + 2, runeA);
                    context.fill(px + 1, py, px + 2, py + 3, runeB);
                }
            }
            case BORDER_STYLE_LIGHTNING -> {
                long time = System.currentTimeMillis();
                int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
                int spanX = Math.max(16, maxX - minX - 8);
                int travel = Math.max(14, maxY - minY - 8);
                int maxBoltDepth = 120;
                int safeTravel = Math.max(1, travel - maxBoltDepth);
                final long cycleMs = 3200L;
                final long activeMs = 1200L;
                final int forkStep = 7;

                for (int i = 0; i < 2; i++) {
                    long cycleIndex = time / cycleMs;
                    long jitter = Math.floorMod((cycleIndex * 197L) + (i * 521L), cycleMs / 2);
                    long offset = i * 113L + jitter;
                    long phase = Math.floorMod(time + offset, cycleMs);
                    if (phase >= activeMs) continue;

                    // Strike appears quickly, then fades over a slightly longer window.
                    float t = phase / (float) activeMs;
                    float fade = t < 0.12F
                            ? (t / 0.12F)
                            : Math.max(0.0F, 1.0F - ((t - 0.12F) / 0.88F));

                    long strikeIndex = (time + offset) / cycleMs;
                    int seed = (int) (strikeIndex * 1103515245L + i * 12345L + 0x6E5F3A1D);
                    int rand0 = seed;
                    int rand1 = seed * 1664525 + 1013904223;
                    int rand2 = rand1 * 1664525 + 1013904223;
                    int rand3 = rand2 * 1664525 + 1013904223;

                    int lane = Math.floorMod(rand0, spanX);
                    int y0 = minY + Math.floorMod(rand1, safeTravel);
                    int x0 = Math.max(minX + 1, Math.min(maxX - 5, minX + lane));

                    int s1 = 4 + Math.floorMod(rand2, 3);
                    int s2 = 4 + Math.floorMod(rand2 >>> 3, 3);
                    int s3 = 4 + Math.floorMod(rand2 >>> 6, 3);
                    int s4 = 4 + Math.floorMod(rand2 >>> 9, 3);
                    int s5 = 4 + Math.floorMod(rand2 >>> 12, 3);
                    int s6 = 4 + Math.floorMod(rand2 >>> 15, 3);

                    // Per-segment drift to allow much stronger diagonal variation.
                    int d1 = Math.floorMod(rand3, 9) - 4;
                    int d2 = Math.floorMod(rand3 >>> 2, 9) - 4;
                    int d3 = Math.floorMod(rand3 >>> 4, 9) - 4;
                    int d4 = Math.floorMod(rand3 >>> 6, 9) - 4;
                    int d5 = Math.floorMod(rand3 >>> 8, 9) - 4;
                    int d6 = Math.floorMod(rand3 >>> 10, 9) - 4;
                    int nonZero = (d1 != 0 ? 1 : 0) + (d2 != 0 ? 1 : 0) + (d3 != 0 ? 1 : 0)
                            + (d4 != 0 ? 1 : 0) + (d5 != 0 ? 1 : 0) + (d6 != 0 ? 1 : 0);
                    int diagonalPower = Math.abs(d1) + Math.abs(d2) + Math.abs(d3) + Math.abs(d4) + Math.abs(d5) + Math.abs(d6);
                    if (nonZero < 5 || diagonalPower < 18) {
                        d1 = 4; d2 = -4; d3 = 3; d4 = -3; d5 = 4; d6 = -4;
                    }

                    int x1 = x0 + d1;
                    int y1 = Math.min(maxY, y0 + s1);
                    int x2 = x1 + d2;
                    int y2 = Math.min(maxY, y1 + s2);
                    int x3 = x2 + d3;
                    int y3 = Math.min(maxY, y2 + s3);
                    int x4 = x3 + d4;
                    int y4 = Math.min(maxY, y3 + s4);
                    int x5 = x4 + d5;
                    int y5 = Math.min(maxY, y4 + s5);
                    int x6 = x5 + d6;
                    int y6 = Math.min(maxY, y5 + s6);

                    int trunkA = (int) (0x22 * fade);
                    int branchA = (int) (0x16 * fade);
                    int sparkA = (int) (0x36 * fade);
                    int trunkColor = (trunkA << 24) | 0x00DFF0FF;
                    int branchColor = (branchA << 24) | 0x00AFCCFF;
                    int sparkColor = (sparkA << 24) | 0x00FFFFFF;

                    // Trunk segments (static during this strike).
                    context.fill(x0, y0, x0 + 1, y1, trunkColor);
                    context.fill(Math.min(x1, x0), y1 - 1, Math.max(x1, x0) + 1, y1, trunkColor);
                    context.fill(x1, y1, x1 + 1, y2, trunkColor);
                    context.fill(Math.min(x2, x1), y2 - 1, Math.max(x2, x1) + 1, y2, trunkColor);
                    context.fill(x2, y2, x2 + 1, y3, trunkColor);
                    context.fill(Math.min(x3, x2), y3 - 1, Math.max(x3, x2) + 1, y3, trunkColor);
                    context.fill(x3, y3, x3 + 1, y4, trunkColor);
                    context.fill(Math.min(x4, x3), y4 - 1, Math.max(x4, x3) + 1, y4, trunkColor);
                    context.fill(x4, y4, x4 + 1, y5, trunkColor);
                    context.fill(Math.min(x5, x4), y5 - 1, Math.max(x5, x4) + 1, y5, trunkColor);
                    context.fill(x5, y5, x5 + 1, y6, trunkColor);

                    // Fork branches in random directions (fewer roots, much larger trees).
                    int forkDirA0 = Math.floorMod(rand3 >>> 12, 9) - 4;
                    int forkDirA1 = Math.floorMod(rand3 >>> 14, 9) - 4;
                    int forkDirA2 = Math.floorMod(rand3 >>> 16, 9) - 4;
                    int forkDirA3 = Math.floorMod(rand3 >>> 6, 9) - 4;
                    int forkDirA4 = Math.floorMod(rand3 >>> 8, 9) - 4;
                    int forkDirB0 = Math.floorMod(rand3 >>> 18, 9) - 4;
                    int forkDirB1 = Math.floorMod(rand3 >>> 20, 9) - 4;
                    int forkDirB2 = Math.floorMod(rand3 >>> 22, 9) - 4;
                    int forkDirB3 = Math.floorMod(rand3 >>> 9, 9) - 4;
                    int forkDirB4 = Math.floorMod(rand3 >>> 11, 9) - 4;
                    if (forkDirA0 == 0) forkDirA0 = ((rand3 >>> 24) & 1) == 0 ? -1 : 1;
                    if (forkDirB0 == 0) forkDirB0 = ((rand3 >>> 25) & 1) == 0 ? -1 : 1;
                    int subForkDirA0 = Math.floorMod(rand3 >>> 26, 9) - 4;
                    int subForkDirA1 = Math.floorMod(rand3 >>> 28, 9) - 4;
                    int subForkDirA2 = Math.floorMod(rand2 >>> 17, 9) - 4;
                    int subForkDirA3 = Math.floorMod(rand2 >>> 19, 9) - 4;
                    int subForkDirB0 = Math.floorMod(rand2 >>> 1, 9) - 4;
                    int subForkDirB1 = Math.floorMod(rand2 >>> 3, 9) - 4;
                    int subForkDirB2 = Math.floorMod(rand2 >>> 5, 9) - 4;
                    int subForkDirB3 = Math.floorMod(rand2 >>> 7, 9) - 4;
                    int sweepA0 = Math.floorMod(rand0 >>> 6, 5) - 2;
                    int sweepA1 = Math.floorMod(rand0 >>> 8, 5) - 2;
                    int sweepA2 = Math.floorMod(rand0 >>> 10, 5) - 2;
                    int sweepA3 = Math.floorMod(rand0 >>> 12, 5) - 2;
                    int sweepA4 = Math.floorMod(rand0 >>> 14, 5) - 2;
                    int sweepB0 = Math.floorMod(rand1 >>> 6, 5) - 2;
                    int sweepB1 = Math.floorMod(rand1 >>> 8, 5) - 2;
                    int sweepB2 = Math.floorMod(rand1 >>> 10, 5) - 2;
                    int sweepB3 = Math.floorMod(rand1 >>> 12, 5) - 2;
                    int sweepB4 = Math.floorMod(rand1 >>> 14, 5) - 2;
                    if (subForkDirA0 == 0) subForkDirA0 = 1;
                    if (subForkDirB0 == 0) subForkDirB0 = -1;

                    // Primary branch A (very large).
                    float aStart = 0.04F, aEnd = 0.98F;
                    float aLife = t <= aStart ? 0.0F : Math.max(0.0F, (aEnd - t) / (aEnd - aStart));
                    if (aLife > 0.0F) {
                        int aAlpha = (int) (branchA * aLife);
                        int aColor = (aAlpha << 24) | 0x00A5C7FF;

                        int forkFromX = x1;
                        int forkFromY = y1;
                        int bx1 = Math.max(minX, Math.min(maxX - 1, forkFromX + forkDirA0 + sweepA0));
                        int by1 = Math.min(maxY, forkFromY + forkStep);
                        int bx2 = Math.max(minX, Math.min(maxX - 1, bx1 + forkDirA1 + sweepA1));
                        int by2 = Math.min(maxY, by1 + forkStep);
                        int bx3 = Math.max(minX, Math.min(maxX - 1, bx2 + forkDirA2 + sweepA2));
                        int by3 = Math.min(maxY, by2 + forkStep);
                        int bx4 = Math.max(minX, Math.min(maxX - 1, bx3 + forkDirA3 + sweepA3));
                        int by4 = Math.min(maxY, by3 + forkStep);
                        int bx5 = Math.max(minX, Math.min(maxX - 1, bx4 + forkDirA4 + sweepA4));
                        int by5 = Math.min(maxY, by4 + forkStep);
                        context.fill(forkFromX, forkFromY, forkFromX + 1, by1, aColor);
                        context.fill(Math.min(bx1, forkFromX), by1 - 1, Math.max(bx1, forkFromX) + 1, by1, aColor);
                        context.fill(bx1, by1, bx1 + 1, by2, aColor);
                        context.fill(Math.min(bx2, bx1), by2 - 1, Math.max(bx2, bx1) + 1, by2, aColor);
                        context.fill(bx2, by2, bx2 + 1, by3, aColor);
                        context.fill(Math.min(bx3, bx2), by3 - 1, Math.max(bx3, bx2) + 1, by3, aColor);
                        context.fill(bx3, by3, bx3 + 1, by4, aColor);
                        context.fill(Math.min(bx4, bx3), by4 - 1, Math.max(bx4, bx3) + 1, by4, aColor);
                        context.fill(bx4, by4, bx4 + 1, by5, aColor);

                        // Secondary fork from branch A with its own lifetime.
                        float aaStart = 0.08F, aaEnd = 0.92F;
                        float aaLife = t <= aaStart ? 0.0F : Math.max(0.0F, (aaEnd - t) / (aaEnd - aaStart));
                        if (aaLife > 0.0F) {
                            int aaAlpha = (int) (branchA * 0.95F * aaLife);
                            int aaColor = (aaAlpha << 24) | 0x0097BEFF;
                            int ab1x = Math.max(minX, Math.min(maxX - 1, bx2 + subForkDirA0 + sweepA1));
                            int ab1y = Math.min(maxY, by2 + forkStep);
                            int ab2x = Math.max(minX, Math.min(maxX - 1, ab1x + subForkDirA1 + sweepA2));
                            int ab2y = Math.min(maxY, ab1y + forkStep);
                            int ab3x = Math.max(minX, Math.min(maxX - 1, ab2x + subForkDirA2 + sweepA3));
                            int ab3y = Math.min(maxY, ab2y + forkStep);
                            int ab4x = Math.max(minX, Math.min(maxX - 1, ab3x + subForkDirA3 + sweepA4));
                            int ab4y = Math.min(maxY, ab3y + forkStep);
                            context.fill(bx2, by2, bx2 + 1, ab1y, aaColor);
                            context.fill(Math.min(ab1x, bx2), ab1y - 1, Math.max(ab1x, bx2) + 1, ab1y, aaColor);
                            context.fill(ab1x, ab1y, ab1x + 1, ab2y, aaColor);
                            context.fill(Math.min(ab2x, ab1x), ab2y - 1, Math.max(ab2x, ab1x) + 1, ab2y, aaColor);
                            context.fill(ab2x, ab2y, ab2x + 1, ab3y, aaColor);
                            context.fill(Math.min(ab3x, ab2x), ab3y - 1, Math.max(ab3x, ab2x) + 1, ab3y, aaColor);
                            context.fill(ab3x, ab3y, ab3x + 1, ab4y, aaColor);

                            // Tertiary fork from secondary fork.
                            float aaaStart = 0.18F, aaaEnd = 0.84F;
                            float aaaLife = t <= aaaStart ? 0.0F : Math.max(0.0F, (aaaEnd - t) / (aaaEnd - aaaStart));
                            if (aaaLife > 0.0F) {
                                int aaaAlpha = (int) (branchA * 0.72F * aaaLife);
                                int aaaColor = (aaaAlpha << 24) | 0x008CB6FF;
                                int ac1x = Math.max(minX, Math.min(maxX - 1, ab2x + (subForkDirA0 == 0 ? 1 : subForkDirA0) + sweepA0));
                                int ac1y = Math.min(maxY, ab2y + forkStep);
                                int ac2x = Math.max(minX, Math.min(maxX - 1, ac1x + (subForkDirA1 == 0 ? -1 : subForkDirA1) + sweepA1));
                                int ac2y = Math.min(maxY, ac1y + forkStep);
                                context.fill(ab2x, ab2y, ab2x + 1, ac1y, aaaColor);
                                context.fill(Math.min(ac1x, ab2x), ac1y - 1, Math.max(ac1x, ab2x) + 1, ac1y, aaaColor);
                                context.fill(ac1x, ac1y, ac1x + 1, ac2y, aaaColor);
                                context.fill(ac2x - 1, ac2y, ac2x + 1, ac2y + 1, sparkColor);
                            }

                            context.fill(ab4x - 1, ab4y, ab4x + 1, ab4y + 1, sparkColor);
                        }

                        context.fill(bx5 - 1, by5, bx5 + 1, by5 + 1, sparkColor);
                    }

                    // Primary branch B (reduced spawn count; larger when present).
                    boolean spawnBranchB = ((rand0 >>> 4) & 7) == 0;
                    float bStart = 0.12F, bEnd = 0.98F;
                    float bLife = t <= bStart ? 0.0F : Math.max(0.0F, (bEnd - t) / (bEnd - bStart));
                    if (spawnBranchB && bLife > 0.0F) {
                        int bAlpha = (int) (branchA * bLife);
                        int bColor = (bAlpha << 24) | 0x00A5C7FF;

                        int cx1 = Math.max(minX, Math.min(maxX - 1, x3 + forkDirB0 + sweepB0));
                        int cy1 = Math.min(maxY, y4 + forkStep);
                        int cx2 = Math.max(minX, Math.min(maxX - 1, cx1 + forkDirB1 + sweepB1));
                        int cy2 = Math.min(maxY, cy1 + forkStep);
                        int cx3 = Math.max(minX, Math.min(maxX - 1, cx2 + forkDirB2 + sweepB2));
                        int cy3 = Math.min(maxY, cy2 + forkStep);
                        int cx4 = Math.max(minX, Math.min(maxX - 1, cx3 + forkDirB3 + sweepB3));
                        int cy4 = Math.min(maxY, cy3 + forkStep);
                        int cx5 = Math.max(minX, Math.min(maxX - 1, cx4 + forkDirB4 + sweepB4));
                        int cy5 = Math.min(maxY, cy4 + forkStep);
                        context.fill(x4, y4, x4 + 1, cy1, bColor);
                        context.fill(Math.min(cx1, x4), cy1 - 1, Math.max(cx1, x4) + 1, cy1, bColor);
                        context.fill(cx1, cy1, cx1 + 1, cy2, bColor);
                        context.fill(Math.min(cx2, cx1), cy2 - 1, Math.max(cx2, cx1) + 1, cy2, bColor);
                        context.fill(cx2, cy2, cx2 + 1, cy3, bColor);
                        context.fill(Math.min(cx3, cx2), cy3 - 1, Math.max(cx3, cx2) + 1, cy3, bColor);
                        context.fill(cx3, cy3, cx3 + 1, cy4, bColor);
                        context.fill(Math.min(cx4, cx3), cy4 - 1, Math.max(cx4, cx3) + 1, cy4, bColor);
                        context.fill(cx4, cy4, cx4 + 1, cy5, bColor);

                        // Secondary fork from branch B with separate lifetime.
                        float bbStart = 0.16F, bbEnd = 0.94F;
                        float bbLife = t <= bbStart ? 0.0F : Math.max(0.0F, (bbEnd - t) / (bbEnd - bbStart));
                        if (bbLife > 0.0F) {
                            int bbAlpha = (int) (branchA * 0.88F * bbLife);
                            int bbColor = (bbAlpha << 24) | 0x0090B6FF;
                            int cb1x = Math.max(minX, Math.min(maxX - 1, cx2 + subForkDirB0 + sweepB1));
                            int cb1y = Math.min(maxY, cy2 + forkStep);
                            int cb2x = Math.max(minX, Math.min(maxX - 1, cb1x + subForkDirB1 + sweepB2));
                            int cb2y = Math.min(maxY, cb1y + forkStep);
                            int cb3x = Math.max(minX, Math.min(maxX - 1, cb2x + subForkDirB2 + sweepB3));
                            int cb3y = Math.min(maxY, cb2y + forkStep);
                            int cb4x = Math.max(minX, Math.min(maxX - 1, cb3x + subForkDirB3 + sweepB4));
                            int cb4y = Math.min(maxY, cb3y + forkStep);
                            context.fill(cx2, cy2, cx2 + 1, cb1y, bbColor);
                            context.fill(Math.min(cb1x, cx2), cb1y - 1, Math.max(cb1x, cx2) + 1, cb1y, bbColor);
                            context.fill(cb1x, cb1y, cb1x + 1, cb2y, bbColor);
                            context.fill(Math.min(cb2x, cb1x), cb2y - 1, Math.max(cb2x, cb1x) + 1, cb2y, bbColor);
                            context.fill(cb2x, cb2y, cb2x + 1, cb3y, bbColor);
                            context.fill(Math.min(cb3x, cb2x), cb3y - 1, Math.max(cb3x, cb2x) + 1, cb3y, bbColor);
                            context.fill(cb3x, cb3y, cb3x + 1, cb4y, bbColor);

                            // Tertiary fork from secondary B.
                            float bbbStart = 0.24F, bbbEnd = 0.88F;
                            float bbbLife = t <= bbbStart ? 0.0F : Math.max(0.0F, (bbbEnd - t) / (bbbEnd - bbbStart));
                            if (bbbLife > 0.0F) {
                                int bbbAlpha = (int) (branchA * 0.68F * bbbLife);
                                int bbbColor = (bbbAlpha << 24) | 0x0083AEFF;
                                int cc1x = Math.max(minX, Math.min(maxX - 1, cb2x + (subForkDirB0 == 0 ? -1 : subForkDirB0) + sweepB0));
                                int cc1y = Math.min(maxY, cb2y + forkStep);
                                int cc2x = Math.max(minX, Math.min(maxX - 1, cc1x + (subForkDirB1 == 0 ? 1 : subForkDirB1) + sweepB1));
                                int cc2y = Math.min(maxY, cc1y + forkStep);
                                context.fill(cb2x, cb2y, cb2x + 1, cc1y, bbbColor);
                                context.fill(Math.min(cc1x, cb2x), cc1y - 1, Math.max(cc1x, cb2x) + 1, cc1y, bbbColor);
                                context.fill(cc1x, cc1y, cc1x + 1, cc2y, bbbColor);
                                context.fill(cc2x - 1, cc2y, cc2x + 1, cc2y + 1, sparkColor);
                            }

                            context.fill(cb4x - 1, cb4y, cb4x + 1, cb4y + 1, sparkColor);
                        }

                        context.fill(cx5 - 1, cy5, cx5 + 1, cy5 + 1, sparkColor);
                    }

                    context.fill(x6 - 1, y6, x6 + 1, y6 + 1, sparkColor);
                }
            }
            default -> {}
        }
    }

    @Unique
    private static void simplytooltips$drawRotatedBlossomPetal(DrawContext context, int x, int y, int rotation) {
        int petalA = 0x14F3B1D2, petalB = 0x10E38BB8, petalC = 0x0CF6C6DE, core = 0x16FFF4BE;
        switch (rotation) {
            case 1 -> {
                context.fill(x + 1, y, x + 3, y + 4, petalA);
                context.fill(x, y + 1, x + 2, y + 5, petalB);
                context.fill(x + 2, y, x + 4, y + 3, petalC);
                context.fill(x + 1, y + 2, x + 2, y + 3, core);
            }
            case 2 -> {
                context.fill(x + 1, y, x + 5, y + 2, petalA);
                context.fill(x, y + 1, x + 4, y + 3, petalB);
                context.fill(x + 2, y + 2, x + 5, y + 4, petalC);
                context.fill(x + 2, y + 1, x + 3, y + 2, core);
            }
            case 3 -> {
                context.fill(x + 2, y, x + 4, y + 4, petalA);
                context.fill(x + 1, y + 1, x + 3, y + 5, petalB);
                context.fill(x, y + 1, x + 3, y + 3, petalC);
                context.fill(x + 2, y + 2, x + 3, y + 3, core);
            }
            default -> {
                context.fill(x, y, x + 4, y + 2, petalA);
                context.fill(x + 1, y + 1, x + 5, y + 3, petalB);
                context.fill(x, y + 2, x + 3, y + 4, petalC);
                context.fill(x + 2, y + 1, x + 3, y + 2, core);
            }
        }
    }

    @Unique
    private static void simplytooltips$drawRotatedSnowflake(DrawContext context, int x, int y, int rotation, int sizeVariant) {
        int outer = 0x12D8F2FF, inner = 0x18E7F8FF, core = 0x20FFFFFF;
        if ((sizeVariant & 1) == 0) {
            context.fill(x + 1, y, x + 2, y + 1, outer);
            context.fill(x, y + 1, x + 3, y + 2, inner);
            context.fill(x + 1, y + 2, x + 2, y + 3, outer);
            context.fill(x + 1, y + 1, x + 2, y + 2, (rotation & 1) == 0 ? core : 0x18FFFFFF);
        } else {
            context.fill(x + 1, y, x + 3, y + 1, outer);
            context.fill(x, y + 1, x + 4, y + 3, inner);
            context.fill(x + 1, y + 3, x + 3, y + 4, outer);
            context.fill(x + 1, y + 1, x + 3, y + 3, (rotation & 1) == 0 ? core : 0x18FFFFFF);
        }
    }

    @Unique
    private static void simplytooltips$drawRotatedVineLeaf(DrawContext context, int x, int y, int rotation) {
        int leafA = 0x1A84C47E, leafB = 0x1470B16A, vein = 0x1A3F7E45;
        switch (rotation) {
            case 1 -> {
                context.fill(x + 1, y, x + 3, y + 4, leafA);
                context.fill(x, y + 1, x + 2, y + 5, leafB);
                context.fill(x + 1, y + 1, x + 2, y + 4, vein);
            }
            case 2 -> {
                context.fill(x + 1, y, x + 5, y + 2, leafA);
                context.fill(x, y + 1, x + 4, y + 3, leafB);
                context.fill(x + 1, y + 1, x + 4, y + 2, vein);
            }
            case 3 -> {
                context.fill(x + 2, y, x + 4, y + 4, leafA);
                context.fill(x + 1, y + 1, x + 3, y + 5, leafB);
                context.fill(x + 2, y + 1, x + 3, y + 4, vein);
            }
            default -> {
                context.fill(x, y, x + 4, y + 2, leafA);
                context.fill(x + 1, y + 1, x + 5, y + 3, leafB);
                context.fill(x + 1, y + 1, x + 4, y + 2, vein);
            }
        }
    }

    @Unique
    private static void simplytooltips$drawBorderPattern(DrawContext context, int x, int y, int w, int h, TooltipTheme theme, int borderStyle) {
        switch (borderStyle) {
            case BORDER_STYLE_VINE -> {
                int leafA = 0xFF79BE77, leafB = 0xFF5EA661, stem = 0xFF3E7A44;
                for (int px = x + 9, i = 0; px < x + w - 10; px += 11, i++) {
                    boolean flip = (i & 1) == 0;
                    int c = flip ? leafA : leafB;
                    context.fill(px, y + 1, px + 2, y + 2, c);
                    if (flip) { context.fill(px + 1, y + 2, px + 3, y + 3, c); }
                    else       { context.fill(px - 1, y + 2, px + 1, y + 3, c); }
                    context.fill(px, y + h - 3, px + 2, y + h - 2, c);
                    if (flip) { context.fill(px - 1, y + h - 2, px + 1, y + h - 1, c); }
                    else       { context.fill(px + 1, y + h - 2, px + 3, y + h - 1, c); }
                    if (i % 3 == 0) {
                        context.fill(px, y + 3, px + 1, y + 5, stem);
                        context.fill(px, y + h - 5, px + 1, y + h - 3, stem);
                    }
                }
            }
            case BORDER_STYLE_BEE -> {
                int honey = 0xFFE8B847, wax = 0xFFF4D77B, outline = 0xFF6A4A1C;
                for (int px = x + 8; px < x + w - 10; px += 12) {
                    context.fill(px, y + 1, px + 3, y + 3, honey);
                    context.fill(px + 1, y, px + 2, y + 1, wax);
                    context.fill(px, y + 1, px + 1, y + 2, outline);
                    context.fill(px + 2, y + 2, px + 3, y + 3, outline);
                    context.fill(px, y + h - 3, px + 3, y + h - 1, honey);
                    context.fill(px + 1, y + h - 1, px + 2, y + h, wax);
                    context.fill(px, y + h - 2, px + 1, y + h - 1, outline);
                    context.fill(px + 2, y + h - 3, px + 3, y + h - 2, outline);
                }
                context.fill(x + 3, y + 4, x + 5, y + 5, 0x99DDF7FF);
                context.fill(x + w - 5, y + 4, x + w - 3, y + 5, 0x99DDF7FF);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 4, 0x99DDF7FF);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 4, 0x99DDF7FF);
            }
            case BORDER_STYLE_BLOSSOM -> {
                int petal = 0xFFF3B1D2, core = 0xFFFFF4BE;
                for (int px = x + 12; px < x + w - 12; px += 16) {
                    context.fill(px, y + 1, px + 1, y + 4, petal);
                    context.fill(px - 1, y + 2, px + 2, y + 3, petal);
                    context.fill(px, y + 2, px + 1, y + 3, core);
                    context.fill(px, y + h - 4, px + 1, y + h - 1, petal);
                    context.fill(px - 1, y + h - 3, px + 2, y + h - 2, petal);
                    context.fill(px, y + h - 3, px + 1, y + h - 2, core);
                }
            }
            case BORDER_STYLE_BUBBLE -> {
                int crest = 0xFF8EE7F8, foam = 0xFFC8F7FF;
                for (int px = x + 6, step = 0; px < x + w - 6; px += 8, step++) {
                    int dy = (step % 2 == 0) ? 0 : 1;
                    context.fill(px, y + 1 + dy, px + 5, y + 2 + dy, crest);
                    context.fill(px, y + h - 2 - dy, px + 5, y + h - 1 - dy, crest);
                }
                for (int px = x + 12; px < x + w - 12; px += 18) {
                    context.fill(px, y + 2, px + 2, y + 4, foam);
                    context.fill(px + 1, y + 3, px + 3, y + 5, crest);
                    context.fill(px, y + h - 5, px + 2, y + h - 3, foam);
                    context.fill(px + 1, y + h - 4, px + 3, y + h - 2, crest);
                }
            }
            case BORDER_STYLE_EARTH -> {
                int rockDark = simplytooltips$lerpColor(theme.border(), 0xFF3A2E22, 0.55f);
                int rockMid  = simplytooltips$lerpColor(theme.border(), 0xFF6E5A40, 0.32f);
                int dust     = simplytooltips$lerpColor(theme.borderInner(), 0xFFCAB28D, 0.22f);
                for (int px = x + 8, i = 0; px < x + w - 9; px += 10, i++) {
                    int wChunk = (i % 3 == 0) ? 3 : 2;
                    context.fill(px, y + 1, px + wChunk, y + 2, rockMid);
                    context.fill(px + 1, y + 2, px + wChunk + 1, y + 3, rockDark);
                    context.fill(px, y + h - 3, px + wChunk, y + h - 2, rockMid);
                    context.fill(px - 1, y + h - 2, px + wChunk - 1, y + h - 1, rockDark);
                }
                for (int px = x + 14; px < x + w - 14; px += 18) {
                    context.fill(px, y + 2, px + 1, y + 4, dust);
                    context.fill(px + 1, y + 3, px + 2, y + 4, rockDark);
                    context.fill(px, y + h - 4, px + 1, y + h - 2, dust);
                    context.fill(px - 1, y + h - 3, px, y + h - 2, rockDark);
                }
                context.fill(x + 3, y + 3, x + 5, y + 5, rockMid);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 5, rockMid);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 3, rockMid);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, rockMid);
            }
            case BORDER_STYLE_ECHO -> {
                int runeA = 0xFFB59AFF, runeB = 0xFF7C67D9;
                for (int px = x + 10, i = 0; px < x + w - 10; px += 16, i++) {
                    if ((i & 1) == 0) {
                        context.fill(px, y + 1, px + 1, y + 4, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 2, runeB);
                        context.fill(px + 1, y + 3, px + 2, y + 4, runeB);
                        context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 3, runeB);
                        context.fill(px + 1, y + h - 2, px + 2, y + h - 1, runeB);
                    } else {
                        context.fill(px, y + 2, px + 3, y + 3, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                        context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
                    }
                }
            }
            case BORDER_STYLE_ICE -> {
                int ice = 0xFFBFE9FF;
                for (int px = x + 10; px < x + w - 10; px += 12) {
                    context.fill(px, y, px + 1, y + 3, ice);
                    context.fill(px - 1, y + 1, px + 2, y + 2, ice);
                    context.fill(px, y + h - 3, px + 1, y + h, ice);
                    context.fill(px - 1, y + h - 2, px + 2, y + h - 1, ice);
                }
            }
            case BORDER_STYLE_LIGHTNING -> {
                int boltA = 0xFFE7F1FF;
                int boltB = 0xFF9CB8F8;
                int spark = 0xFF74A8FF;
                for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
                    if ((i & 1) == 0) {
                        context.fill(px, y + 1, px + 1, y + 3, boltA);
                        context.fill(px + 1, y + 2, px + 2, y + 4, boltB);
                        context.fill(px + 2, y + 2, px + 4, y + 3, spark);

                        context.fill(px, y + h - 4, px + 1, y + h - 2, boltA);
                        context.fill(px + 1, y + h - 5, px + 2, y + h - 3, boltB);
                        context.fill(px + 2, y + h - 4, px + 4, y + h - 3, spark);
                    } else {
                        context.fill(px + 2, y + 1, px + 3, y + 3, boltA);
                        context.fill(px + 1, y + 2, px + 2, y + 4, boltB);
                        context.fill(px, y + 2, px + 1, y + 3, spark);

                        context.fill(px + 2, y + h - 4, px + 3, y + h - 2, boltA);
                        context.fill(px + 1, y + h - 5, px + 2, y + h - 3, boltB);
                        context.fill(px, y + h - 4, px + 1, y + h - 3, spark);
                    }
                }

                context.fill(x + 3, y + 3, x + 5, y + 4, spark);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, spark);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, spark);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, spark);
            }
            default -> {}
        }
    }

    @Unique
    private static void simplytooltips$drawSmallDiamond(DrawContext context, int cx, int cy, int color) {
        context.fill(cx, cy - 1, cx + 1, cy, color);
        context.fill(cx - 1, cy, cx + 2, cy + 1, color);
        context.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    @Unique
    private static void simplytooltips$drawSeparator(DrawContext context, int x, int y, int width, TooltipTheme theme) {
        int lineY = y + 4;
        int midX  = x + width / 2;
        context.fill(x + 4, lineY, midX - 5, lineY + 1, theme.separator());
        context.fill(midX + 5, lineY, x + width - 4, lineY + 1, theme.separator());
        simplytooltips$drawSmallDiamond(context, midX, lineY, theme.border());
    }

    @Unique
    private static void simplytooltips$drawDiamondFrame(DrawContext context, int x, int y, int size, TooltipTheme theme) {
        int cx = x + size / 2, cy = y + size / 2, half = size / 2;
        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrameInner());
        }
        for (int dy = -half; dy <= half; dy++) {
            int span = half - Math.abs(dy);
            context.fill(cx - span, cy + dy, cx - span + 1, cy + dy + 1, theme.diamondFrame());
            context.fill(cx + span, cy + dy, cx + span + 1, cy + dy + 1, theme.diamondFrame());
        }
        context.fill(cx, cy - half, cx + 1, cy - half + 1, theme.diamondFrame());
        context.fill(cx, cy + half, cx + 1, cy + half + 1, theme.diamondFrame());
    }

    @Unique
    private static int simplytooltips$drawBadge(DrawContext context, TextRenderer tr, String label, int x, int y, TooltipTheme theme) {
        int textW = tr.getWidth(label);
        int badgePadH = 3;
        int badgeH = tr.fontHeight;
        int badgeW = textW + badgePadH * 2;
        context.fill(x, y, x + badgeW, y + badgeH, theme.badgeBg());
        context.drawText(tr,
                Text.literal(label).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.badgeCutout() & 0x00FFFFFF))),
                x + badgePadH, y + 1, theme.badgeCutout(), false);
        return x + badgeW;
    }

    @Unique
    private static void simplytooltips$drawFooterDots(DrawContext context, int cx, int y, TooltipTheme theme) {
        simplytooltips$drawSmallDiamond(context, cx - 8, y, theme.footerDot());
        simplytooltips$drawSmallDiamond(context, cx,     y, theme.footerDot());
        simplytooltips$drawSmallDiamond(context, cx + 8, y, theme.footerDot());
    }

    @Unique
    private static void simplytooltips$drawWaveText(DrawContext context, TextRenderer tr, String text, int x, int y, int color, long timeMs) {
        if (text == null || text.isEmpty()) return;

        final int length = text.length();
        final double amplitude = 1.4;
        final double spread = 2.2;
        final long idleMs = 1700L;
        final long activeMs = 520L + (long) (length * 85L);
        final long cycleMs = activeMs + idleMs;

        long cyclePos = Math.floorMod(timeMs, cycleMs);
        boolean active = cyclePos < activeMs;

        double activeProgress = active ? (double) cyclePos / (double) activeMs : 1.0;
        double startPeak = -spread;
        double endPeak   = (length - 1) + spread;
        double peakPos   = startPeak + (endPeak - startPeak) * activeProgress;

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String ch = String.valueOf(c);
            double distance = Math.abs(i - peakPos);
            double influence = distance < spread
                    ? 0.5 * (1.0 + Math.cos(Math.PI * (distance / spread)))
                    : 0.0;
            int yOffset = (int) Math.round(-influence * amplitude);
            context.drawText(tr,
                    Text.literal(ch).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))),
                    cursorX, y + yOffset, color, true);
            cursorX += tr.getWidth(ch);
        }
    }

    @Unique
    private static List<String> simplytooltips$wrapStrings(List<String> lines, TextRenderer tr, int maxWidth) {
        List<String> wrapped = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null || raw.isEmpty()) {
                wrapped.add(" ");
                continue;
            }
            String[] words = raw.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (tr.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                    wrapped.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) wrapped.add(current.toString());
        }
        return wrapped;
    }

    @Unique
    private static int simplytooltips$lerpColor(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF, aR = (a >>> 16) & 0xFF, aG = (a >>> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >>> 16) & 0xFF, bG = (b >>> 8) & 0xFF, bB = b & 0xFF;
        return ((int)(aA + (bA - aA) * t) << 24) | ((int)(aR + (bR - aR) * t) << 16) |
               ((int)(aG + (bG - aG) * t) << 8)  |  (int)(aB + (bB - aB) * t);
    }
}
