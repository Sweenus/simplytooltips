package net.sweenus.simplytooltips.client.studio.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.client.studio.StudioTheme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Label plus a small sliding switch, for the theme's one boolean. */
public class ToggleWidget extends ClickableWidget {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ToggleWidget(int x, int y, int width, Text label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, 12, label);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean on = getter.getAsBoolean();

        if (isHovered()) context.fill(getX() - 2, getY(), getX() + getWidth() + 2, getY() + getHeight(), StudioTheme.HOVER);

        int switchW = 16;
        int switchX = getX() + getWidth() - switchW;
        int switchY = getY() + 2;

        String label = StudioTheme.trim(client.textRenderer, getMessage().getString(), getWidth() - switchW - 6);
        context.drawText(client.textRenderer, label, getX(), getY() + 2,
                on ? StudioTheme.TEXT_PRIMARY : StudioTheme.TEXT_BODY, false);

        StudioTheme.card(context, switchX, switchY, switchW, 8,
                on ? StudioTheme.PRIMARY_FILL : StudioTheme.FIELD,
                on ? StudioTheme.ACCENT : StudioTheme.BUTTON_EDGE);
        int knobX = on ? switchX + switchW - 6 : switchX + 2;
        context.fill(knobX, switchY + 2, knobX + 4, switchY + 6,
                on ? StudioTheme.ACCENT : StudioTheme.TEXT_DIM);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
