package net.sweenus.simplytooltips.client.studio.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;
import net.sweenus.simplytooltips.client.studio.StudioTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** The searchable theme rail. A gold dot marks a theme backed by an editable file on disk. */
public class ThemeListWidget extends ClickableWidget {

    private final Consumer<String> onSelect;
    private final List<String> filtered = new ArrayList<>();

    private String filter = "";
    private String selected;
    private int scroll;

    public ThemeListWidget(int x, int y, int width, int height, String selected, Consumer<String> onSelect) {
        super(x, y, width, height, Text.literal("Themes"));
        this.selected = selected;
        this.onSelect = onSelect;
        refresh();
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        refresh();
    }

    /** Rebuilds the visible list and keeps the selection on screen. */
    public void refresh() {
        filtered.clear();
        for (String key : ThemeRegistry.keys()) {
            if (filter.isEmpty() || key.toLowerCase(Locale.ROOT).contains(filter)) filtered.add(key);
        }
        clampScroll();
        scrollToSelected();
    }

    public String selected() {
        return selected;
    }

    public void setSelected(String key) {
        this.selected = key;
        scrollToSelected();
    }

    private int visibleRows() {
        return Math.max(1, getHeight() / StudioTheme.ROW_H);
    }

    private void clampScroll() {
        int max = Math.max(0, filtered.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll));
    }

    private void scrollToSelected() {
        int index = filtered.indexOf(selected);
        if (index < 0) return;
        if (index < scroll) scroll = index;
        else if (index >= scroll + visibleRows()) scroll = index - visibleRows() + 1;
        clampScroll();
    }

    /** Moves the selection by {@code delta} entries, which is what the cycle arrows and ←/→ use. */
    public void cycle(int delta) {
        if (filtered.isEmpty()) return;
        int index = filtered.indexOf(selected);
        int next = index < 0 ? 0 : Math.floorMod(index + delta, filtered.size());
        selected = filtered.get(next);
        scrollToSelected();
        onSelect.accept(selected);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int row = (int) ((mouseY - getY()) / StudioTheme.ROW_H);
        int index = scroll + row;
        if (index < 0 || index >= filtered.size()) return;
        selected = filtered.get(index);
        onSelect.accept(selected);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        scroll -= (int) Math.signum(vertical);
        clampScroll();
        return true;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int rows = visibleRows();

        if (filtered.isEmpty()) {
            context.drawText(client.textRenderer, "no matches", getX() + 6, getY() + 4,
                    StudioTheme.TEXT_DIM, false);
            return;
        }

        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= filtered.size()) break;
            String key = filtered.get(index);
            int ry = getY() + i * StudioTheme.ROW_H;
            boolean isSelected = key.equals(selected);
            boolean hot = StudioTheme.inside(mouseX, mouseY, getX(), ry, getWidth(), StudioTheme.ROW_H);

            if (isSelected) {
                context.fill(getX(), ry, getX() + getWidth(), ry + StudioTheme.ROW_H, StudioTheme.ACCENT_SOFT);
                context.fill(getX(), ry, getX() + 2, ry + StudioTheme.ROW_H, StudioTheme.ACCENT);
            } else if (hot) {
                context.fill(getX(), ry, getX() + getWidth(), ry + StudioTheme.ROW_H, StudioTheme.HOVER);
            }

            boolean editable = ThemeRegistry.isUserTheme(key);
            int labelW = getWidth() - 12 - (editable ? 6 : 0);
            context.drawText(client.textRenderer,
                    StudioTheme.trim(client.textRenderer, key, labelW),
                    getX() + 6, ry + 2,
                    isSelected ? StudioTheme.TEXT_PRIMARY : StudioTheme.TEXT_BODY, false);

            if (editable) {
                context.fill(getX() + getWidth() - 7, ry + 5, getX() + getWidth() - 5, ry + 7, StudioTheme.ACCENT);
            }
        }

        if (filtered.size() > rows) {
            int trackH = getHeight();
            int thumbH = Math.max(8, trackH * rows / filtered.size());
            int thumbY = getY() + (trackH - thumbH) * scroll / Math.max(1, filtered.size() - rows);
            context.fill(getX() + getWidth() - 2, thumbY, getX() + getWidth() - 1, thumbY + thumbH,
                    StudioTheme.ACCENT_DIM);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
