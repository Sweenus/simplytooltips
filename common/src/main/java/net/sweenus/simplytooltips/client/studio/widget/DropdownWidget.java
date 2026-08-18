package net.sweenus.simplytooltips.client.studio.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.client.studio.StudioTheme;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Label plus a value box that opens a scrolling option list.
 *
 * <p>The theme's style fields are plain strings the renderer resolves with a silently-defaulting
 * switch, so a typo would look like a working theme that quietly ignores the value. Offering only
 * registered keys removes that whole class of mistake.
 */
public class DropdownWidget extends ClickableWidget {

    private static final int MAX_VISIBLE = 9;

    private final Supplier<List<String>> options;
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    private boolean open;
    private int scroll;

    /** Label row plus value box. Height covers both; {@link #BOX_H} is the clickable box alone. */
    public static final int HEIGHT = 22;
    public static final int BOX_H = 12;

    public DropdownWidget(int x, int y, int width, Text label,
                          Supplier<List<String>> options, Supplier<String> getter, Consumer<String> setter) {
        super(x, y, width, HEIGHT, label);
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    private int valueX() {
        return getX();
    }

    private int valueW() {
        return getWidth();
    }

    private int boxY() {
        return getY() + HEIGHT - BOX_H;
    }

    /** Height of the popup, so the screen can draw it above everything else. */
    public int popupHeight() {
        return Math.min(MAX_VISIBLE, options.get().size()) * StudioTheme.ROW_H + 2;
    }

    /** The widget spans label plus box, but only the box is clickable. */
    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return active && visible && StudioTheme.inside(mouseX, mouseY, getX(), boxY(), getWidth(), BOX_H);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        open = !open;
        if (open) {
            List<String> opts = options.get();
            int index = opts.indexOf(getter.get());
            scroll = Math.max(0, Math.min(index - MAX_VISIBLE / 2, Math.max(0, opts.size() - MAX_VISIBLE)));
        }
    }

    /** Consumes a click inside the open popup. Returns true if the click was handled. */
    public boolean clickPopup(double mouseX, double mouseY) {
        if (!open) return false;
        List<String> opts = options.get();
        int rows = Math.min(MAX_VISIBLE, opts.size());
        int px = valueX();
        int py = boxY() + BOX_H;

        if (!StudioTheme.inside(mouseX, mouseY, px, py, valueW(), rows * StudioTheme.ROW_H + 2)) {
            open = false;
            return true;
        }
        int row = (int) ((mouseY - py - 1) / StudioTheme.ROW_H);
        int index = scroll + row;
        if (index >= 0 && index < opts.size()) setter.accept(opts.get(index));
        open = false;
        return true;
    }

    public boolean scrollPopup(double amount) {
        if (!open) return false;
        List<String> opts = options.get();
        int max = Math.max(0, opts.size() - MAX_VISIBLE);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(amount)));
        return true;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int px = valueX();
        int pw = valueW();
        int by = boxY();

        context.drawText(client.textRenderer,
                StudioTheme.trim(client.textRenderer, getMessage().getString(), pw),
                getX(), getY(), StudioTheme.TEXT_BODY, false);

        StudioTheme.card(context, px, by, pw, BOX_H, StudioTheme.FIELD,
                open || isHovered() ? StudioTheme.ACCENT_DIM : StudioTheme.FIELD_EDGE);
        context.drawText(client.textRenderer,
                StudioTheme.trim(client.textRenderer, getter.get(), pw - 14),
                px + 4, by + 2, StudioTheme.TEXT_PRIMARY, false);
        context.drawText(client.textRenderer, open ? "^" : "v", px + pw - 8, by + 2,
                StudioTheme.TEXT_DIM, false);
    }

    /** Drawn separately by the screen, after every other widget, so the list is never covered. */
    public void renderPopup(DrawContext context, int mouseX, int mouseY) {
        if (!open) return;
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> opts = options.get();
        int rows = Math.min(MAX_VISIBLE, opts.size());
        int px = valueX();
        int pw = valueW();
        int py = boxY() + BOX_H;
        int ph = rows * StudioTheme.ROW_H + 2;

        StudioTheme.card(context, px, py, pw, ph, 0xFF0C0E13, StudioTheme.ACCENT_DIM);

        String current = getter.get();
        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= opts.size()) break;
            String option = opts.get(index);
            int ry = py + 1 + i * StudioTheme.ROW_H;
            boolean hot = StudioTheme.inside(mouseX, mouseY, px + 1, ry, pw - 2, StudioTheme.ROW_H);
            if (hot) context.fill(px + 1, ry, px + pw - 1, ry + StudioTheme.ROW_H, StudioTheme.HOVER);
            boolean selected = option.equals(current);
            if (selected) context.fill(px + 1, ry, px + 3, ry + StudioTheme.ROW_H, StudioTheme.ACCENT);
            context.drawText(client.textRenderer,
                    StudioTheme.trim(client.textRenderer, option, pw - 12),
                    px + 6, ry + 2,
                    selected ? StudioTheme.ACCENT : StudioTheme.TEXT_BODY, false);
        }

        if (opts.size() > MAX_VISIBLE) {
            int trackH = ph - 2;
            int thumbH = Math.max(6, trackH * rows / opts.size());
            int thumbY = py + 1 + (trackH - thumbH) * scroll / Math.max(1, opts.size() - rows);
            context.fill(px + pw - 3, thumbY, px + pw - 2, thumbY + thumbH, StudioTheme.ACCENT_DIM);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
