package net.sweenus.simplytooltips.client.studio.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.studio.StudioTheme;

/**
 * Hue-wheel colour picker: hue by angle, saturation by radius, with value and alpha bars beside it
 * and a hex field underneath.
 *
 * <p>The wheel is rasterised once into a texture at full saturation-value and tinted by the current
 * value when drawn, so moving the cursor costs nothing. It is not a widget — the screen owns it as
 * a popup so it can float above every other control.
 */
public final class ColorWheelPicker {

    public static final int WHEEL = 62;
    private static final int BAR_W = 9;
    private static final int GAP = 5;
    private static final int PAD = 6;
    private static final int FOOT = 26;

    public static final int WIDTH = PAD * 2 + WHEEL + GAP + BAR_W * 2 + GAP;
    public static final int HEIGHT = PAD * 2 + WHEEL + FOOT;

    private static final Identifier WHEEL_TEXTURE = Identifier.of(SimplyTooltips.MOD_ID, "studio/color_wheel");
    private static boolean wheelUploaded;

    private final String label;
    private final int originalArgb;
    private final java.util.function.IntConsumer onChange;

    private float hue;
    private float saturation;
    private float value;
    private int alpha;

    private int x;
    private int y;
    private int drag = DRAG_NONE;

    private static final int DRAG_NONE = 0;
    private static final int DRAG_WHEEL = 1;
    private static final int DRAG_VALUE = 2;
    private static final int DRAG_ALPHA = 3;

    public ColorWheelPicker(String label, int argb, java.util.function.IntConsumer onChange) {
        this.label = label;
        this.originalArgb = argb;
        this.onChange = onChange;
        setArgb(argb);
    }

    public String label() {
        return label;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int argb() {
        int rgb = hsvToRgb(hue, saturation, value);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public void setArgb(int argb) {
        alpha = (argb >>> 24) & 0xFF;
        float[] hsv = rgbToHsv(argb & 0x00FFFFFF);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    // ---- interaction --------------------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!StudioTheme.inside(mouseX, mouseY, x, y, WIDTH, HEIGHT)) return false;

        int wx = x + PAD;
        int wy = y + PAD;
        int vx = wx + WHEEL + GAP;
        int ax = vx + BAR_W + GAP;

        if (StudioTheme.inside(mouseX, mouseY, wx, wy, WHEEL, WHEEL)) {
            drag = DRAG_WHEEL;
        } else if (StudioTheme.inside(mouseX, mouseY, vx, wy, BAR_W, WHEEL)) {
            drag = DRAG_VALUE;
        } else if (StudioTheme.inside(mouseX, mouseY, ax, wy, BAR_W, WHEEL)) {
            drag = DRAG_ALPHA;
        } else {
            return true;
        }
        mouseDragged(mouseX, mouseY);
        return true;
    }

    public void mouseDragged(double mouseX, double mouseY) {
        if (drag == DRAG_NONE) return;
        int wx = x + PAD;
        int wy = y + PAD;

        switch (drag) {
            case DRAG_WHEEL -> {
                double cx = wx + WHEEL / 2.0;
                double cy = wy + WHEEL / 2.0;
                double dx = mouseX - cx;
                double dy = mouseY - cy;
                double radius = WHEEL / 2.0;
                double dist = Math.sqrt(dx * dx + dy * dy);
                hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360.0) % 360.0) / 360.0f;
                saturation = (float) Math.min(1.0, dist / radius);
            }
            case DRAG_VALUE -> value = 1.0f - clamp01((float) (mouseY - wy) / WHEEL);
            case DRAG_ALPHA -> alpha = Math.round(255 * (1.0f - clamp01((float) (mouseY - wy) / WHEEL)));
            default -> { }
        }
        onChange.accept(argb());
    }

    public void mouseReleased() {
        drag = DRAG_NONE;
    }

    /** True only while a wheel or bar is actually being dragged, so other drags are not swallowed. */
    public boolean isDragging() {
        return drag != DRAG_NONE;
    }

    // ---- drawing ------------------------------------------------------------------------------

    public void render(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        ensureWheel(client);

        StudioTheme.card(context, x, y, WIDTH, HEIGHT, 0xFF0C0E13, StudioTheme.ACCENT_DIM);

        int wx = x + PAD;
        int wy = y + PAD;
        int vx = wx + WHEEL + GAP;
        int ax = vx + BAR_W + GAP;

        context.drawTexture(WHEEL_TEXTURE, wx, wy, 0.0F, 0.0F, WHEEL, WHEEL, WHEEL, WHEEL);
        // The texture is full-value; darkening it in place is what makes the value bar read as one.
        int shade = Math.round((1.0f - value) * 255);
        if (shade > 0) context.fill(wx, wy, wx + WHEEL, wy + WHEEL, (shade << 24));

        drawWheelCursor(context, wx, wy);
        drawValueBar(context, vx, wy);
        drawAlphaBar(context, ax, wy);

        int footY = y + PAD + WHEEL + 5;
        StudioTheme.swatch(context, x + PAD + 1, footY + 1, 16, 8, originalArgb);
        StudioTheme.swatch(context, x + PAD + 19, footY + 1, 16, 8, argb());

        String hex = StudioTheme.hex(argb());
        context.drawText(client.textRenderer, hex,
                x + WIDTH - PAD - client.textRenderer.getWidth(hex), footY + 1,
                StudioTheme.TEXT_PRIMARY, false);
        context.drawText(client.textRenderer,
                StudioTheme.trim(client.textRenderer, label, WIDTH - PAD * 2),
                x + PAD, footY + 12, StudioTheme.TEXT_DIM, false);
    }

    private void drawWheelCursor(DrawContext context, int wx, int wy) {
        double angle = Math.toRadians(hue * 360.0);
        double radius = saturation * (WHEEL / 2.0);
        int cx = (int) Math.round(wx + WHEEL / 2.0 + Math.cos(angle) * radius);
        int cy = (int) Math.round(wy + WHEEL / 2.0 + Math.sin(angle) * radius);
        int ring = value > 0.55f ? 0xFF101010 : 0xFFFFFFFF;
        context.fill(cx - 2, cy - 3, cx + 3, cy - 2, ring);
        context.fill(cx - 2, cy + 2, cx + 3, cy + 3, ring);
        context.fill(cx - 3, cy - 2, cx - 2, cy + 2, ring);
        context.fill(cx + 2, cy - 2, cx + 3, cy + 2, ring);
    }

    private void drawValueBar(DrawContext context, int bx, int by) {
        int pureRgb = hsvToRgb(hue, saturation, 1.0f);
        for (int i = 0; i < WHEEL; i++) {
            float v = 1.0f - i / (float) (WHEEL - 1);
            context.fill(bx, by + i, bx + BAR_W, by + i + 1, 0xFF000000 | scaleRgb(pureRgb, v));
        }
        StudioTheme.card(context, bx - 1, by - 1, BAR_W + 2, WHEEL + 2, 0x00000000, StudioTheme.BUTTON_EDGE);
        drawBarCursor(context, bx, by, 1.0f - value);
    }

    private void drawAlphaBar(DrawContext context, int bx, int by) {
        int rgb = hsvToRgb(hue, saturation, value);
        for (int i = 0; i < WHEEL; i++) {
            boolean light = (i / 4) % 2 == 0;
            context.fill(bx, by + i, bx + BAR_W, by + i + 1, light ? 0xFF6A6A6A : 0xFF3C3C3C);
            int a = Math.round(255 * (1.0f - i / (float) (WHEEL - 1)));
            context.fill(bx, by + i, bx + BAR_W, by + i + 1, (a << 24) | rgb);
        }
        StudioTheme.card(context, bx - 1, by - 1, BAR_W + 2, WHEEL + 2, 0x00000000, StudioTheme.BUTTON_EDGE);
        drawBarCursor(context, bx, by, 1.0f - alpha / 255.0f);
    }

    private void drawBarCursor(DrawContext context, int bx, int by, float fraction) {
        int cy = by + Math.round(clamp01(fraction) * (WHEEL - 1));
        context.fill(bx - 2, cy, bx + BAR_W + 2, cy + 1, 0xFFFFFFFF);
        context.fill(bx - 2, cy - 1, bx + BAR_W + 2, cy, 0xFF101010);
        context.fill(bx - 2, cy + 1, bx + BAR_W + 2, cy + 2, 0xFF101010);
    }

    /** Rasterises the hue/saturation disc once per session. */
    private static void ensureWheel(MinecraftClient client) {
        if (wheelUploaded) return;

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, WHEEL, WHEEL, false);
        double radius = WHEEL / 2.0;
        for (int py = 0; py < WHEEL; py++) {
            for (int px = 0; px < WHEEL; px++) {
                double dx = px + 0.5 - radius;
                double dy = py + 0.5 - radius;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > radius) {
                    image.setColor(px, py, 0);
                    continue;
                }
                float h = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360.0) % 360.0) / 360.0f;
                float s = (float) Math.min(1.0, dist / radius);
                // Feather the last pixel of the rim so the disc does not read as a jagged polygon.
                int a = dist > radius - 1.0 ? (int) Math.round(255 * (radius - dist)) : 255;
                int argb = (Math.max(0, Math.min(255, a)) << 24) | hsvToRgb(h, s, 1.0f);
                image.setColor(px, py, ColorHelper.Abgr.toAbgr(argb));
            }
        }

        client.getTextureManager().registerTexture(WHEEL_TEXTURE, new NativeImageBackedTexture(image));
        wheelUploaded = true;
    }

    // ---- colour maths -------------------------------------------------------------------------

    private static int scaleRgb(int rgb, float factor) {
        int r = Math.round(((rgb >> 16) & 0xFF) * factor);
        int g = Math.round(((rgb >> 8) & 0xFF) * factor);
        int b = Math.round((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    public static int hsvToRgb(float h, float s, float v) {
        float hh = (h % 1.0f) * 6.0f;
        int sector = (int) Math.floor(hh);
        float f = hh - sector;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));

        float r;
        float g;
        float b;
        switch (Math.floorMod(sector, 6)) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;

        float h;
        if (d == 0.0f) h = 0.0f;
        else if (max == r) h = ((g - b) / d) % 6.0f;
        else if (max == g) h = (b - r) / d + 2.0f;
        else h = (r - g) / d + 4.0f;

        h = ((h / 6.0f) % 1.0f + 1.0f) % 1.0f;
        return new float[]{h, max == 0.0f ? 0.0f : d / max, max};
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
