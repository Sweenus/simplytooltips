package net.sweenus.simplytooltips.client.studio;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.render.BorderDefinitionRegistry;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.MotifRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;
import net.sweenus.simplytooltips.client.render.TooltipPainter;
import net.sweenus.simplytooltips.client.studio.widget.ColorWheelPicker;
import net.sweenus.simplytooltips.client.studio.widget.DropdownWidget;
import net.sweenus.simplytooltips.client.studio.widget.StudioButton;
import net.sweenus.simplytooltips.client.studio.widget.ThemeListWidget;
import net.sweenus.simplytooltips.client.studio.widget.ToggleWidget;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The Theme Studio: preview any theme on any item, edit its values, and save the result.
 *
 * <p>Opened by {@code /simplytooltips}. Everything it writes lands in
 * {@code config/simplytooltips/}, so nothing here can damage the themes shipped in the mod jar —
 * built-in themes can be opened and copied but never overwritten.
 */
public class ThemeStudioScreen extends Screen {

    private static final List<String> ITEM_ANIM_STYLES =
            List.of("breathe_spin_bob", "spin", "bob", "breathe", "static");
    private static final List<String> TITLE_ANIM_STYLES =
            List.of("wave", "shimmer", "pulse", "flicker", "shiver", "quiver",
                    "breathe_spin_bob", "drop_bounce", "hinge_fall", "obfuscate", "static");
    private static final List<String> BORDER_SHAPES =
            List.of("diamond", "square", "circle", "cross", "none");

    /** Clears the z=400 that {@code TooltipRenderer} lifts the preview to; vanilla's popup value. */
    private static final float OVERLAY_Z = 1000.0f;

    private static final long DOUBLE_CLICK_MS = 250L;

    private static final int HEADER_H = 16;
    private static final int FOOTER_H = 20;

    private enum Tab { COLOURS, STYLE, TEXT }

    private final String initialItemId;

    private ThemeDraft draft;
    private final TooltipPreviewPane preview = new TooltipPreviewPane();

    private Tab tab = Tab.COLOURS;
    private int inspectorScroll;
    private ColorWheelPicker picker;

    private TextFieldWidget searchField;
    private TextFieldWidget itemField;
    private TextFieldWidget badgesField;
    private TextFieldWidget customKeysField;
    private TextFieldWidget saveAsField;
    private ThemeListWidget themeList;

    private final List<DropdownWidget> dropdowns = new ArrayList<>();
    private ToggleWidget shadowToggle;
    private StudioButton overrideButton;
    private StudioButton revertButton;
    private StudioButton saveAsButton;
    private StudioButton assignButton;
    private StudioButton saveConfirmButton;
    private StudioButton saveCancelButton;

    private boolean naming;
    private String status = "";
    private int statusColor = StudioTheme.TEXT_DIM;
    private long statusUntilMs;

    private List<String> suggestions = List.of();

    // Panel geometry, recomputed in init().
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int railW;
    private int inspectorW;
    private int stageX;
    private int stageW;
    private int contentY;
    private int contentH;
    private int inspectorX;
    private int stageBoxY;
    private int stageBoxH;
    private boolean panningPreview;

    /** Cached so a keystroke, not every frame, is what produces a new list for the renderer. */
    private List<String> badgeOverride;
    private long lastStageClickMs;

    public ThemeStudioScreen(String initialItemId) {
        super(Text.translatable("simplytooltips.studio.title"));
        this.initialItemId = initialItemId;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int margin = width < 500 ? 12 : 22;
        panelX = margin;
        panelY = margin;
        panelW = width - margin * 2;
        panelH = height - margin * 2;

        railW = Math.max(76, Math.min(96, panelW / 5));
        inspectorW = Math.max(150, Math.min(190, panelW * 30 / 100));
        stageX = panelX + 1 + railW + 1;
        stageW = panelW - 2 - railW - inspectorW - 2;
        inspectorX = stageX + stageW + 1;
        contentY = panelY + HEADER_H + 1;
        contentH = panelH - HEADER_H - FOOTER_H - 2;
        stageBoxY = contentY + 48;
        stageBoxH = contentY + contentH - stageBoxY - 6;

        String startKey = draft != null ? draft.sourceKey() : firstThemeKey();
        if (draft == null) draft = new ThemeDraft(startKey, ThemeRegistry.get(startKey));

        buildRail(startKey);
        buildStage();
        buildInspector();
        buildFooter();

        // Only choose a starting item on first open; a resize re-runs init() and must not
        // discard whatever the player is currently previewing.
        if (preview.stack().isEmpty()) {
            if (initialItemId != null && !initialItemId.isBlank()) {
                preview.setItem(initialItemId);
            } else {
                TooltipPreviewPane.heldStack().ifPresentOrElse(
                        preview::setStack, () -> preview.setItem("minecraft:diamond_sword"));
            }
        }
        itemField.setText(preview.itemId());
        suggestions = List.of();
        badgesField.setText(badgeOverride == null ? "" : String.join(", ", badgeOverride));

        // ScreenScrollMixin cancels the raw scroll callback while a scrollable tooltip is active.
        // It times out after 80ms, but clearing it here means the zoom wheel can never be eaten.
        net.sweenus.simplytooltips.client.render.ScrollState.setScrollableActive(false);

        syncActionButtons();
    }

    /** The rectangle the preview is drawn into - the same one {@link #drawStage} passes the pane. */
    private boolean overStage(double mouseX, double mouseY) {
        return StudioTheme.inside(mouseX, mouseY, stageX + 10, stageBoxY + 4, stageW - 20, stageBoxH - 8);
    }

    private static String firstThemeKey() {
        List<String> keys = ThemeRegistry.keys();
        return keys.contains("default") ? "default" : (keys.isEmpty() ? "default" : keys.get(0));
    }

    // ---- construction -------------------------------------------------------------------------

    private void buildRail(String startKey) {
        searchField = flatField(panelX + 9, contentY + 8, railW - 18, Text.translatable("simplytooltips.studio.search"));
        searchField.setPlaceholder(Text.translatable("simplytooltips.studio.search"));
        searchField.setChangedListener(text -> themeList.setFilter(text));
        addDrawableChild(searchField);

        int listY = contentY + 22;
        themeList = new ThemeListWidget(panelX + 3, listY, railW - 4, contentH - 22, startKey, this::selectTheme);
        addDrawableChild(themeList);
    }

    private void buildStage() {
        int labelW = textRenderer.getWidth("BADGES") + 6;
        int fieldX = stageX + 8 + labelW;
        int fieldW = stageW - 16 - labelW - 14;

        itemField = flatField(fieldX + 4, contentY + 7, fieldW - 8, Text.translatable("simplytooltips.studio.item"));
        itemField.setMaxLength(256);
        itemField.setChangedListener(this::onItemTyped);
        addDrawableChild(itemField);

        addDrawableChild(new StudioButton(fieldX + fieldW + 2, contentY + 4, 12, 12,
                Text.literal("H"), false,
                () -> TooltipPreviewPane.heldStack().ifPresentOrElse(stack -> {
                    preview.setStack(stack);
                    itemField.setText(preview.itemId());
                    suggestions = List.of();
                    setStatus(Text.translatable("simplytooltips.studio.status.held").getString(), StudioTheme.OK);
                }, () -> setStatus(Text.translatable("simplytooltips.studio.status.no_held").getString(), StudioTheme.DANGER))));

        badgesField = flatField(fieldX + 4, contentY + 21, fieldW - 8, Text.translatable("simplytooltips.studio.badges"));
        badgesField.setMaxLength(256);
        badgesField.setPlaceholder(Text.translatable("simplytooltips.studio.badges.hint"));
        badgesField.setChangedListener(this::onBadgesTyped);
        addDrawableChild(badgesField);

        addDrawableChild(new StudioButton(stageX + 8, contentY + 32, 12, 12,
                Text.literal("<"), false, () -> themeList.cycle(-1)));
        addDrawableChild(new StudioButton(stageX + stageW - 20, contentY + 32, 12, 12,
                Text.literal(">"), false, () -> themeList.cycle(1)));
    }

    private void buildInspector() {
        dropdowns.clear();
        int rowX = inspectorX + 7;
        int rowW = inspectorW - 14;
        int y = contentY + 22;

        // Label above box, so neither a long field name nor a long value has to be truncated.
        int pitch = DropdownWidget.HEIGHT + 2;

        dropdowns.add(new DropdownWidget(rowX, y, rowW, Text.literal("motif"),
                MotifRegistry::keys, draft::motif, value -> { draft.setMotif(value); onDraftChanged(); }));
        dropdowns.add(new DropdownWidget(rowX, y + pitch, rowW, Text.literal("borderStyle"),
                BorderDefinitionRegistry::styleKeys, draft::borderStyle,
                value -> { draft.setBorderStyle(value); onDraftChanged(); }));
        dropdowns.add(new DropdownWidget(rowX, y + pitch * 2, rowW, Text.literal("itemAnimStyle"),
                () -> ITEM_ANIM_STYLES, draft::itemAnimStyle,
                value -> { draft.setItemAnimStyle(value); onDraftChanged(); }));
        dropdowns.add(new DropdownWidget(rowX, y + pitch * 3, rowW, Text.literal("titleAnimStyle"),
                () -> TITLE_ANIM_STYLES, draft::titleAnimStyle,
                value -> { draft.setTitleAnimStyle(value); onDraftChanged(); }));
        dropdowns.add(new DropdownWidget(rowX, y + pitch * 4, rowW, Text.literal("itemBorderShape"),
                () -> BORDER_SHAPES, draft::itemBorderShape,
                value -> { draft.setItemBorderShape(value); onDraftChanged(); }));

        // "motif" must be registered before the motif dropdown can offer it, so a motif added by
        // another mod shows up here without this screen knowing anything about it.
        for (DropdownWidget dropdown : dropdowns) addDrawableChild(dropdown);

        shadowToggle = new ToggleWidget(rowX, y + pitch * 5 + 2, rowW, Text.literal("textShadow"),
                draft::textShadow, value -> { draft.setTextShadow(value); onDraftChanged(); });
        addDrawableChild(shadowToggle);

        customKeysField = flatField(rowX + 4, y + 18, rowW - 8, Text.translatable("simplytooltips.studio.custom_keys"));
        customKeysField.setMaxLength(512);
        customKeysField.setText(String.join(", ", draft.customTextKeys()));
        customKeysField.setChangedListener(text -> {
            draft.setCustomTextKeys(splitList(text));
            onDraftChanged();
        });
        addDrawableChild(customKeysField);

        applyTabVisibility();
    }

    private void buildFooter() {
        int footerY = panelY + panelH - FOOTER_H;
        int buttonY = footerY + 4;

        Text assign = Text.translatable("simplytooltips.studio.assign");
        assignButton = new StudioButton(panelX + 8, buttonY, StudioButton.widthFor(assign), 12, assign, false, this::assignToItem);
        addDrawableChild(assignButton);

        Text override = Text.translatable("simplytooltips.studio.override");
        Text saveAs = Text.translatable("simplytooltips.studio.save_as");
        Text revert = Text.translatable("simplytooltips.studio.revert");

        int rightX = panelX + panelW - 8;
        int overrideW = StudioButton.widthFor(override);
        rightX -= overrideW;
        overrideButton = new StudioButton(rightX, buttonY, overrideW, 12, override, false, this::overrideTheme)
                .withDisabledReason(() -> ThemeRegistry.isBuiltIn(draft.sourceKey())
                        ? Text.translatable("simplytooltips.studio.override.builtin")
                        : Text.translatable("simplytooltips.studio.override.clean"));
        addDrawableChild(overrideButton);

        int saveW = StudioButton.widthFor(saveAs);
        rightX -= saveW + 6;
        saveAsButton = new StudioButton(rightX, buttonY, saveW, 12, saveAs, true, this::beginNaming);
        addDrawableChild(saveAsButton);

        int revertW = StudioButton.widthFor(revert);
        rightX -= revertW + 6;
        revertButton = new StudioButton(rightX, buttonY, revertW, 12, revert, false, () -> {
            draft.revert();
            syncInspectorFields();
            onDraftChanged();
            setStatus(Text.translatable("simplytooltips.studio.status.reverted").getString(), StudioTheme.TEXT_DIM);
        });
        addDrawableChild(revertButton);

        Text save = Text.translatable("simplytooltips.studio.save");
        Text cancel = Text.translatable("simplytooltips.studio.cancel");
        int cancelW = StudioButton.widthFor(cancel);
        int confirmW = StudioButton.widthFor(save);

        saveCancelButton = new StudioButton(panelX + panelW - 8 - cancelW, buttonY, cancelW, 12, cancel, false, this::cancelNaming);
        saveConfirmButton = new StudioButton(panelX + panelW - 14 - cancelW - confirmW, buttonY, confirmW, 12, save, true, this::commitNaming);
        addDrawableChild(saveConfirmButton);
        addDrawableChild(saveCancelButton);

        saveAsField = flatField(panelX + 12, buttonY + 3, panelW - 40 - cancelW - confirmW, Text.translatable("simplytooltips.studio.new_name"));
        saveAsField.setMaxLength(48);
        saveAsField.setPlaceholder(Text.translatable("simplytooltips.studio.new_name"));
        addDrawableChild(saveAsField);

        setNaming(false);
    }

    private TextFieldWidget flatField(int x, int y, int w, Text label) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, Math.max(10, w), 8, label);
        field.setDrawsBackground(false);
        field.setEditableColor(StudioTheme.TEXT_PRIMARY);
        return field;
    }

    // ---- state --------------------------------------------------------------------------------

    private void selectTheme(String key) {
        draft.load(key, ThemeRegistry.get(key));
        inspectorScroll = 0;
        picker = null;
        syncInspectorFields();
        preview.restartAnimation();
        syncActionButtons();
    }

    private void syncInspectorFields() {
        customKeysField.setText(String.join(", ", draft.customTextKeys()));
    }

    private void onDraftChanged() {
        syncActionButtons();
    }

    private void syncActionButtons() {
        boolean dirty = draft.isDirty();
        boolean editable = ThemeRegistry.isUserTheme(draft.sourceKey()) && !ThemeRegistry.isBuiltIn(draft.sourceKey());
        overrideButton.active = editable && dirty;
        revertButton.active = dirty;
        assignButton.active = preview.hasItem();
    }

    private void onBadgesTyped(String text) {
        List<String> parsed = splitList(text);
        badgeOverride = parsed.isEmpty() ? null : parsed;
        preview.setForcedBadges(badgeOverride);
    }

    private void onItemTyped(String text) {
        suggestions = TooltipPreviewPane.suggest(text, 7);
        if (suggestions.size() == 1 && suggestions.get(0).equals(text.trim())) suggestions = List.of();
        preview.setItem(text);
    }

    private void setStatus(String message, int color) {
        status = message;
        statusColor = color;
        statusUntilMs = System.currentTimeMillis() + 4000L;
    }

    private static List<String> splitList(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // ---- actions ------------------------------------------------------------------------------

    private void assignToItem() {
        if (!preview.hasItem()) {
            setStatus(Text.translatable("simplytooltips.studio.status.no_item").getString(), StudioTheme.DANGER);
            return;
        }
        try {
            UserDataStore.saveItemMapping(preview.itemId(), draft.sourceKey(),
                    badgeOverride == null ? List.of() : badgeOverride);
            setStatus(Text.translatable("simplytooltips.studio.status.assigned",
                    preview.itemId(), draft.sourceKey()).getString(), StudioTheme.OK);
        } catch (Exception e) {
            SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to save item mapping", e);
            setStatus(Text.translatable("simplytooltips.studio.status.save_failed", String.valueOf(e.getMessage())).getString(),
                    StudioTheme.DANGER);
        }
    }

    private void overrideTheme() {
        saveTheme(draft.sourceKey());
    }

    private void beginNaming() {
        setNaming(true);
        saveAsField.setText(ThemeRegistry.isBuiltIn(draft.sourceKey()) ? draft.sourceKey() + "_copy" : draft.sourceKey());
        setFocused(saveAsField);
        saveAsField.setFocused(true);
    }

    private void cancelNaming() {
        setNaming(false);
    }

    private void commitNaming() {
        String key = UserDataStore.sanitizeKey(saveAsField.getText());
        if (!UserDataStore.isValidKey(key)) {
            setStatus(Text.translatable("simplytooltips.studio.status.bad_key").getString(), StudioTheme.DANGER);
            return;
        }
        if (ThemeRegistry.isBuiltIn(key)) {
            setStatus(Text.translatable("simplytooltips.studio.status.builtin_taken", key).getString(), StudioTheme.DANGER);
            return;
        }
        setNaming(false);
        saveTheme(key);
    }

    private void saveTheme(String key) {
        try {
            UserDataStore.saveTheme(key, draft.toDefinition());
            draft.markSaved(key);
            themeList.refresh();
            themeList.setSelected(key);
            setStatus(Text.translatable("simplytooltips.studio.status.saved", key).getString(), StudioTheme.OK);
        } catch (Exception e) {
            SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to save theme '{}'", key, e);
            setStatus(Text.translatable("simplytooltips.studio.status.save_failed", String.valueOf(e.getMessage())).getString(),
                    StudioTheme.DANGER);
        }
        syncActionButtons();
    }

    private void setNaming(boolean value) {
        naming = value;
        saveAsField.setVisible(value);
        saveConfirmButton.visible = value;
        saveCancelButton.visible = value;
        assignButton.visible = !value;
        overrideButton.visible = !value;
        saveAsButton.visible = !value;
        revertButton.visible = !value;
    }

    private void applyTabVisibility() {
        for (DropdownWidget dropdown : dropdowns) {
            dropdown.visible = tab == Tab.STYLE;
            if (tab != Tab.STYLE) dropdown.close();
        }
        shadowToggle.visible = tab == Tab.STYLE;
        customKeysField.setVisible(tab == Tab.TEXT);
        if (tab != Tab.COLOURS) picker = null;
    }

    // ---- input --------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (picker != null && picker.mouseClicked(mouseX, mouseY)) return true;
        if (picker != null) picker = null;

        for (DropdownWidget dropdown : dropdowns) {
            if (dropdown.isOpen() && dropdown.clickPopup(mouseX, mouseY)) return true;
        }

        if (!suggestions.isEmpty() && clickSuggestion(mouseX, mouseY)) return true;

        // Close button.
        if (StudioTheme.inside(mouseX, mouseY, panelX + panelW - 14, panelY + 3, 11, 11)) {
            close();
            return true;
        }

        if (clickInspectorTab(mouseX, mouseY)) return true;
        if (tab == Tab.COLOURS && clickColorRow(mouseX, mouseY)) return true;

        if (preview.hasItem() && overStage(mouseX, mouseY)) {
            long now = System.currentTimeMillis();
            if (now - lastStageClickMs < DOUBLE_CLICK_MS) {
                preview.viewport().reset();
                lastStageClickMs = 0L;
            } else {
                lastStageClickMs = now;
                panningPreview = true;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickSuggestion(double mouseX, double mouseY) {
        int sx = itemField.getX() - 4;
        int sw = itemField.getWidth() + 8;
        int sy = itemField.getY() + 12;
        if (!StudioTheme.inside(mouseX, mouseY, sx, sy, sw, suggestions.size() * StudioTheme.ROW_H)) return false;
        int index = (int) ((mouseY - sy) / StudioTheme.ROW_H);
        if (index < 0 || index >= suggestions.size()) return false;
        itemField.setText(suggestions.get(index));
        suggestions = List.of();
        return true;
    }

    private boolean clickInspectorTab(double mouseX, double mouseY) {
        if (mouseY < contentY + 4 || mouseY > contentY + 16) return false;
        int tx = inspectorX + 7;
        for (Tab candidate : Tab.values()) {
            String label = tabLabel(candidate);
            int tw = textRenderer.getWidth(label);
            if (StudioTheme.inside(mouseX, mouseY, tx, contentY + 4, tw, 12)) {
                tab = candidate;
                inspectorScroll = 0;
                applyTabVisibility();
                return true;
            }
            tx += tw + 9;
        }
        return false;
    }

    private boolean clickColorRow(double mouseX, double mouseY) {
        int rowsTop = contentY + 22;
        if (!StudioTheme.inside(mouseX, mouseY, inspectorX, rowsTop, inspectorW, contentY + contentH - rowsTop)) {
            return false;
        }
        int index = inspectorScroll + (int) ((mouseY - rowsTop) / StudioTheme.ROW_H);
        if (index < 0 || index >= ThemeJson.COLOR_KEYS.length) return false;

        String key = ThemeJson.COLOR_KEYS[index];
        picker = new ColorWheelPicker(key, draft.color(key), argb -> {
            draft.setColor(key, argb);
            onDraftChanged();
        });
        // Anchor inside the panel: a picker that opened off the bottom edge would be unusable.
        int px = Math.max(panelX + 4, inspectorX - ColorWheelPicker.WIDTH - 2);
        int py = Math.min(panelY + panelH - ColorWheelPicker.HEIGHT - 4,
                Math.max(panelY + 4, (int) mouseY - ColorWheelPicker.HEIGHT / 2));
        picker.setPosition(px, py);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Only claim the drag if the picker actually started one, or a drag beginning anywhere else
        // would be swallowed while the picker happens to be open.
        if (picker != null && picker.isDragging()) {
            picker.mouseDragged(mouseX, mouseY);
            return true;
        }
        if (panningPreview) {
            preview.viewport().pan(deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panningPreview = false;
        if (picker != null) picker.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (DropdownWidget dropdown : dropdowns) {
            if (dropdown.isOpen() && dropdown.scrollPopup(vertical)) return true;
        }
        if (preview.hasItem() && overStage(mouseX, mouseY)) {
            preview.viewport().zoomAt((int) Math.signum(vertical),
                    mouseX - (stageX + 10), mouseY - (stageBoxY + 4));
            return true;
        }
        if (tab == Tab.COLOURS
                && StudioTheme.inside(mouseX, mouseY, inspectorX, contentY, inspectorW, contentH)) {
            int visible = Math.max(1, (contentY + contentH - (contentY + 22)) / StudioTheme.ROW_H);
            int max = Math.max(0, ThemeJson.COLOR_KEYS.length - visible);
            inspectorScroll = Math.max(0, Math.min(max, inspectorScroll - (int) Math.signum(vertical)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (picker != null) {
                picker = null;
                return true;
            }
            for (DropdownWidget dropdown : dropdowns) {
                if (dropdown.isOpen()) {
                    dropdown.close();
                    return true;
                }
            }
            if (naming) {
                cancelNaming();
                return true;
            }
        }

        if (naming && keyCode == GLFW.GLFW_KEY_ENTER) {
            commitNaming();
            return true;
        }

        boolean typing = getFocused() instanceof TextFieldWidget field && field.isFocused();
        if (!typing && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            themeList.cycle(keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---- drawing ------------------------------------------------------------------------------

    /**
     * All of the panel chrome, drawn here rather than in {@link #render} for one specific reason:
     * {@code Screen.render} calls {@code renderBackground} first, and vanilla's implementation runs
     * the menu blur post-process over the whole main framebuffer. Anything painted before that call
     * returns gets blurred along with the world. Drawing inside the override, after
     * {@code super.renderBackground}, keeps the blur on the world and off the Studio.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        context.fill(0, 0, width, height, StudioTheme.SCRIM);

        StudioTheme.card(context, panelX, panelY, panelW, panelH, StudioTheme.PANEL, StudioTheme.PANEL_EDGE);
        drawHeader(context, mouseX, mouseY);
        drawRail(context);
        drawStage(context, mouseX, mouseY);
        drawFooter(context);

        drawFieldChrome(context);

        context.enableScissor(inspectorX - 1, contentY, inspectorX + inspectorW, contentY + contentH);
        drawInspector(context, mouseX, mouseY);
        if (tab == Tab.COLOURS) drawColorRows(context, mouseX, mouseY);
        context.disableScissor();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, OVERLAY_Z);

        drawZoomBadge(context);
        drawSuggestions(context, mouseX, mouseY);
        for (DropdownWidget dropdown : dropdowns) dropdown.renderPopup(context, mouseX, mouseY);
        if (picker != null) picker.render(context, mouseX, mouseY);
        drawDisabledHint(context, mouseX, mouseY);

        context.getMatrices().pop();
    }

    private void drawHeader(DrawContext context, int mouseX, int mouseY) {
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + HEADER_H, StudioTheme.HEADER);
        StudioTheme.hLine(context, panelX + 1, panelY + HEADER_H, panelW - 2, StudioTheme.PANEL_EDGE);

        String brand = "SIMPLY TOOLTIPS";
        context.drawText(textRenderer, brand, panelX + 8, panelY + 5, StudioTheme.ACCENT, false);
        context.drawText(textRenderer, Text.translatable("simplytooltips.studio.title").getString(),
                panelX + 8 + textRenderer.getWidth(brand) + 8, panelY + 5, StudioTheme.TEXT_DIM, false);

        boolean hot = StudioTheme.inside(mouseX, mouseY, panelX + panelW - 14, panelY + 3, 11, 11);
        context.drawText(textRenderer, "x", panelX + panelW - 11, panelY + 5,
                hot ? StudioTheme.TEXT_PRIMARY : StudioTheme.TEXT_DIM, false);
    }

    private void drawRail(DrawContext context) {
        context.fill(panelX + 1, contentY, panelX + 1 + railW, contentY + contentH, StudioTheme.RAIL);
        StudioTheme.vLine(context, panelX + 1 + railW, contentY, contentH, StudioTheme.PANEL_EDGE);
        StudioTheme.card(context, panelX + 5, contentY + 5, railW - 10, 12, StudioTheme.FIELD, StudioTheme.FIELD_EDGE);
    }

    private void drawStage(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, "ITEM", stageX + 8, contentY + 7, StudioTheme.TEXT_DIM, false);
        context.drawText(textRenderer, "BADGES", stageX + 8, contentY + 21, StudioTheme.TEXT_DIM, false);

        boolean userTheme = ThemeRegistry.isUserTheme(themeList.selected());
        int tagW = userTheme ? textRenderer.getWidth("user") + 6 : 0;
        String key = StudioTheme.trim(textRenderer, themeList.selected(), stageW - 44 - tagW);
        int nameW = textRenderer.getWidth(key);
        context.drawText(textRenderer, key, stageX + (stageW - nameW - tagW) / 2, contentY + 35,
                StudioTheme.TEXT_PRIMARY, false);
        if (userTheme) {
            context.drawText(textRenderer, "user", stageX + (stageW + nameW - tagW) / 2 + 6, contentY + 35,
                    StudioTheme.ACCENT, false);
        }

        int sy = stageBoxY;
        int sh = stageBoxH;
        StudioTheme.card(context, stageX + 6, sy, stageW - 12, sh, StudioTheme.STAGE, StudioTheme.STAGE_EDGE);
        StudioTheme.stageGrid(context, stageX + 6, sy, stageW - 12, sh);

        if (preview.hasItem()) {
            context.enableScissor(stageX + 7, sy + 1, stageX + stageW - 7, sy + sh - 1);
            preview.render(context, draft.toDefinition(), stageX + 10, sy + 4, stageW - 20, sh - 8);
            context.disableScissor();

        } else {
            String message = preview.error() != null
                    ? preview.error()
                    : Text.translatable("simplytooltips.studio.no_preview").getString();
            context.drawText(textRenderer, StudioTheme.trim(textRenderer, message, stageW - 24),
                    stageX + 12, sy + sh / 2 - 4, StudioTheme.TEXT_DIM, false);
        }
    }

    private void drawInspector(DrawContext context, int mouseX, int mouseY) {
        StudioTheme.vLine(context, inspectorX - 1, contentY, contentH, StudioTheme.PANEL_EDGE);

        int tx = inspectorX + 7;
        for (Tab candidate : Tab.values()) {
            String label = tabLabel(candidate);
            int tw = textRenderer.getWidth(label);
            boolean selected = candidate == tab;
            boolean hot = StudioTheme.inside(mouseX, mouseY, tx, contentY + 4, tw, 12);
            context.drawText(textRenderer, label, tx, contentY + 6,
                    selected ? StudioTheme.ACCENT : (hot ? StudioTheme.TEXT_BODY : StudioTheme.TEXT_DIM), false);
            if (selected) StudioTheme.hLine(context, tx, contentY + 16, tw, StudioTheme.ACCENT);
            tx += tw + 9;
        }
        StudioTheme.hLine(context, inspectorX + 6, contentY + 16, inspectorW - 12, StudioTheme.PANEL_EDGE);

        if (tab == Tab.TEXT) {
            int textW = inspectorW - 14;
            context.drawText(textRenderer,
                    StudioTheme.trim(textRenderer,
                            Text.translatable("simplytooltips.studio.custom_keys").getString(), textW),
                    inspectorX + 7, contentY + 24, StudioTheme.TEXT_BODY, false);

            List<String> hint = TooltipPainter.wrapStrings(
                    List.of(Text.translatable("simplytooltips.studio.custom_keys.hint").getString()),
                    textRenderer, textW);
            int hy = contentY + 54;
            for (String line : hint) {
                context.drawText(textRenderer, line, inspectorX + 7, hy, StudioTheme.TEXT_DIM, false);
                hy += textRenderer.fontHeight + 1;
            }
        }
    }

    /**
     * Zoom readout, drawn with the overlays rather than with the stage.
     *
     * <p>It has to clear the preview's z=400, and it sits directly on top of the tooltip once you
     * zoom in — so it carries its own chip, otherwise dim grey text would vanish against a
     * light-page theme like {@code tome}.
     */
    private void drawZoomBadge(DrawContext context) {
        if (!preview.hasItem()) return;

        String note = preview.viewport().percent() + "%"
                + (preview.viewport().isFit() ? "" : "  dbl-click to fit");
        int noteW = textRenderer.getWidth(note);
        int bx = stageX + stageW - 10 - (noteW + 8);
        int by = stageBoxY + stageBoxH - 14;

        StudioTheme.card(context, bx, by, noteW + 8, 12, StudioTheme.PANEL, StudioTheme.PANEL_EDGE);
        context.drawText(textRenderer, note, bx + 4, by + 2, StudioTheme.TEXT_BODY, false);
    }

    private void drawColorRows(DrawContext context, int mouseX, int mouseY) {
        int rowsTop = contentY + 22;
        int visible = Math.max(1, (contentY + contentH - rowsTop) / StudioTheme.ROW_H);

        for (int i = 0; i < visible; i++) {
            int index = inspectorScroll + i;
            if (index >= ThemeJson.COLOR_KEYS.length) break;
            String key = ThemeJson.COLOR_KEYS[index];
            int argb = draft.color(key);
            int ry = rowsTop + i * StudioTheme.ROW_H;

            boolean hot = StudioTheme.inside(mouseX, mouseY, inspectorX, ry, inspectorW, StudioTheme.ROW_H);
            boolean open = picker != null && picker.label().equals(key);
            if (open) context.fill(inspectorX, ry, inspectorX + inspectorW, ry + StudioTheme.ROW_H, StudioTheme.ACCENT_SOFT);
            else if (hot) context.fill(inspectorX, ry, inspectorX + inspectorW, ry + StudioTheme.ROW_H, StudioTheme.HOVER);

            StudioTheme.swatch(context, inspectorX + 8, ry + 2, 9, 8, argb);

            String hex = StudioTheme.hex(argb);
            int hexX = inspectorX + inspectorW - 8 - textRenderer.getWidth(hex);
            int labelX = inspectorX + 21;
            context.drawText(textRenderer, StudioTheme.trimMiddle(textRenderer, key, hexX - 6 - labelX),
                    labelX, ry + 2, open ? StudioTheme.ACCENT : StudioTheme.TEXT_BODY, false);
            context.drawText(textRenderer, hex, hexX, ry + 2, StudioTheme.TEXT_DIM, false);
        }

        if (ThemeJson.COLOR_KEYS.length > visible) {
            int trackH = contentY + contentH - rowsTop;
            int thumbH = Math.max(8, trackH * visible / ThemeJson.COLOR_KEYS.length);
            int thumbY = rowsTop + (trackH - thumbH) * inspectorScroll
                    / Math.max(1, ThemeJson.COLOR_KEYS.length - visible);
            context.fill(inspectorX + inspectorW - 3, thumbY, inspectorX + inspectorW - 2, thumbY + thumbH,
                    StudioTheme.ACCENT_DIM);
        }
    }

    /** Text fields draw no background of their own, so the cards go on underneath them. */
    private void drawFieldChrome(DrawContext context) {
        card(context, searchField);
        card(context, itemField);
        card(context, badgesField);
        if (customKeysField.isVisible()) card(context, customKeysField);
        if (naming) card(context, saveAsField);
    }

    private void card(DrawContext context, TextFieldWidget field) {
        if (!field.isVisible()) return;
        StudioTheme.card(context, field.getX() - 4, field.getY() - 3, field.getWidth() + 8, 12,
                StudioTheme.FIELD, field.isFocused() ? StudioTheme.ACCENT_DIM : StudioTheme.FIELD_EDGE);
    }

    private void drawSuggestions(DrawContext context, int mouseX, int mouseY) {
        if (suggestions.isEmpty() || !itemField.isFocused()) return;
        int sx = itemField.getX() - 4;
        int sw = itemField.getWidth() + 8;
        int sy = itemField.getY() + 12;
        int sh = suggestions.size() * StudioTheme.ROW_H + 2;

        StudioTheme.card(context, sx, sy, sw, sh, 0xFF0C0E13, StudioTheme.ACCENT_DIM);
        for (int i = 0; i < suggestions.size(); i++) {
            int ry = sy + 1 + i * StudioTheme.ROW_H;
            boolean hot = StudioTheme.inside(mouseX, mouseY, sx + 1, ry, sw - 2, StudioTheme.ROW_H);
            if (hot) context.fill(sx + 1, ry, sx + sw - 1, ry + StudioTheme.ROW_H, StudioTheme.HOVER);
            context.drawText(textRenderer,
                    StudioTheme.trim(textRenderer, suggestions.get(i), sw - 10),
                    sx + 5, ry + 2, hot ? StudioTheme.TEXT_PRIMARY : StudioTheme.TEXT_BODY, false);
        }
    }

    private void drawFooter(DrawContext context) {
        int footerY = panelY + panelH - FOOTER_H;
        StudioTheme.hLine(context, panelX + 1, footerY, panelW - 2, StudioTheme.PANEL_EDGE);

        if (naming) {
            context.drawText(textRenderer, Text.translatable("simplytooltips.studio.new_name").getString(),
                    panelX + 8, footerY + 7, StudioTheme.TEXT_DIM, false);
            return;
        }

        if (System.currentTimeMillis() < statusUntilMs && !status.isEmpty()) {
            int available = revertButton.getX() - (assignButton.getX() + assignButton.getWidth()) - 12;
            context.drawText(textRenderer, StudioTheme.trim(textRenderer, status, Math.max(0, available)),
                    assignButton.getX() + assignButton.getWidth() + 8, footerY + 7, statusColor, false);
        }
    }

    private void drawDisabledHint(DrawContext context, int mouseX, int mouseY) {
        for (StudioButton button : List.of(overrideButton, revertButton, assignButton)) {
            if (!button.visible || button.active || !button.isMouseOver(mouseX, mouseY)) continue;
            Text reason = button.disabledReason();
            if (reason == null) continue;
            String message = reason.getString();
            int w = textRenderer.getWidth(message) + 8;
            int tx = Math.max(panelX + 2, Math.min(mouseX - w / 2, panelX + panelW - w - 2));
            int ty = button.getY() - 14;
            StudioTheme.card(context, tx, ty, w, 12, 0xFF0C0E13, StudioTheme.ACCENT_DIM);
            context.drawText(textRenderer, message, tx + 4, ty + 2, StudioTheme.TEXT_BODY, false);
            return;
        }
    }

    private static String tabLabel(Tab tab) {
        return tab.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public void close() {
        ThemeRegistry.reloadUser();
        ItemThemeRegistry.reloadUser();
        super.close();
    }
}
