package net.sweenus.simplytooltips.client.studio.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.client.studio.StudioTheme;

/** Flat card-styled button: no vanilla stone texture, one gold accent for the primary action. */
public class StudioButton extends ClickableWidget {

    private final Runnable onPress;
    private final boolean primary;
    private java.util.function.Supplier<Text> disabledReason = () -> null;

    public StudioButton(int x, int y, int width, int height, Text message, boolean primary, Runnable onPress) {
        super(x, y, width, height, message);
        this.primary = primary;
        this.onPress = onPress;
    }

    /** Explains why the button is greyed out, shown on hover. */
    public StudioButton withDisabledReason(java.util.function.Supplier<Text> reason) {
        this.disabledReason = reason;
        return this;
    }

    public Text disabledReason() {
        return active ? null : disabledReason.get();
    }

    public static int widthFor(Text label) {
        return MinecraftClient.getInstance().textRenderer.getWidth(label) + 14;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (active) onPress.run();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hot = isHovered() && active;
        int fill = !active ? StudioTheme.PANEL
                : primary ? StudioTheme.PRIMARY_FILL
                : hot ? StudioTheme.BUTTON_HOT : StudioTheme.BUTTON;
        int edge = !active ? StudioTheme.BUTTON_EDGE
                : primary ? StudioTheme.ACCENT
                : hot ? StudioTheme.ACCENT_DIM : StudioTheme.BUTTON_EDGE;

        StudioTheme.card(context, getX(), getY(), getWidth(), getHeight(), fill, edge);

        int color = !active ? StudioTheme.TEXT_OFF
                : primary ? StudioTheme.ACCENT
                : hot ? StudioTheme.TEXT_PRIMARY : StudioTheme.TEXT_BODY;

        MinecraftClient client = MinecraftClient.getInstance();
        String label = StudioTheme.trim(client.textRenderer, getMessage().getString(), getWidth() - 4);
        int textX = getX() + (getWidth() - client.textRenderer.getWidth(label)) / 2;
        int textY = getY() + (getHeight() - client.textRenderer.fontHeight) / 2 + 1;
        context.drawText(client.textRenderer, label, textX, textY, color, false);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
