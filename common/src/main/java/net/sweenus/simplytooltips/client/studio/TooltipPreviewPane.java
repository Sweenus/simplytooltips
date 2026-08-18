package net.sweenus.simplytooltips.client.studio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.api.ThemeDefinition;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import net.sweenus.simplytooltips.client.render.TooltipRenderState;
import net.sweenus.simplytooltips.client.render.TooltipRenderer;

import java.util.List;
import java.util.Optional;

/**
 * Draws a live tooltip for an arbitrary item under an arbitrary theme.
 *
 * <p>Uses the same recipe the batch exporter does - {@link Screen#getTooltipFromItem} for the raw
 * lines and {@link TooltipProviderRegistry#find} for the provider — so the preview goes through the
 * real render path rather than an approximation of it. The theme is forced through
 * {@link TooltipRenderState}, which leaves the item's actual mapping untouched.
 *
 * <p>A tall tooltip is measured first and scaled down to fit, because the alternative is silently
 * cropping the very thing the player opened the Studio to look at.
 */
public final class TooltipPreviewPane {

    private ItemStack stack = ItemStack.EMPTY;
    private List<Text> rawLines = List.of();
    private TooltipProvider provider;
    private String error;

    private final PreviewViewport viewport = new PreviewViewport();

    private List<String> forcedBadges;

    private long shownAtMs = System.currentTimeMillis();
    private int lastPanelW;
    private int lastPanelH;
    private float lastScale = 1.0f;

    /**
     * Points the preview at {@code itemId}. Returns false and records a message if the id is not a
     * real item, which the screen shows under the field rather than failing silently.
     */
    public boolean setItem(String itemId) {
        error = null;
        Identifier id = Identifier.tryParse(itemId == null ? "" : itemId.trim());
        if (id == null) {
            error = "Not a valid item id";
            return false;
        }
        if (!Registries.ITEM.containsId(id)) {
            error = "No such item is registered";
            return false;
        }
        ItemStack candidate = new ItemStack(Registries.ITEM.get(id));
        if (candidate.isEmpty() || candidate.getItem() == Items.AIR) {
            error = "No such item is registered";
            return false;
        }
        setStack(candidate);
        return true;
    }

    public void setStack(ItemStack newStack) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.stack = newStack.copy();
        this.error = null;
        this.rawLines = Screen.getTooltipFromItem(client, this.stack);
        this.provider = TooltipProviderRegistry.find(this.stack).orElse(null);
        if (this.provider == null) error = "No tooltip provider handles this item";
        restartAnimation();
    }

    /** Replays the entry animation, so switching theme shows the same motion a fresh hover would. */
    public void restartAnimation() {
        shownAtMs = System.currentTimeMillis();
    }

    public PreviewViewport viewport() {
        return viewport;
    }

    /**
     * Overrides the badges drawn on the preview. Pass {@code null} (or an empty list) to fall back to
     * whatever the item actually resolves to.
     *
     * <p>The list instance is kept as-is and compared by identity inside the renderer's model cache,
     * so callers must hand over a new list only when the badges really changed.
     */
    public void setForcedBadges(List<String> badges) {
        this.forcedBadges = badges == null || badges.isEmpty() ? null : badges;
    }

    public ItemStack stack() {
        return stack;
    }

    public boolean hasItem() {
        return !stack.isEmpty() && provider != null;
    }

    public String error() {
        return error;
    }

    public String itemId() {
        return stack.isEmpty() ? "" : Registries.ITEM.getId(stack.getItem()).toString();
    }

    public int panelWidth() {
        return lastPanelW;
    }

    public int panelHeight() {
        return lastPanelH;
    }

    public float scale() {
        return lastScale;
    }

    /** Measures the panel this item and theme would produce, without drawing anything. */
    public void measure(DrawContext context, ThemeDefinition forced) {
        if (!hasItem()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        TooltipRenderState.State measure =
                TooltipRenderState.State.preview(true, forced, forcedBadges, 0, 0, elapsed());
        TooltipRenderState.run(measure, () -> TooltipRenderer.render(
                context, client.textRenderer, stack, rawLines, provider, 0, 0, 32_768, 32_768));
        lastPanelW = measure.panelWidth();
        lastPanelH = measure.panelHeight();
    }

    /** Measures, then draws centred inside the given rectangle, scaled down only if it must be. */
    public void render(DrawContext context, ThemeDefinition forced, int x, int y, int w, int h) {
        if (!hasItem()) return;
        MinecraftClient client = MinecraftClient.getInstance();

        measure(context, forced);
        if (lastPanelW <= 0 || lastPanelH <= 0) return;

        viewport.setBounds(lastPanelW, lastPanelH, w, h);
        float scale = viewport.scale();
        lastScale = scale;

        context.getMatrices().push();
        context.getMatrices().translate(x + viewport.offsetX(), y + viewport.offsetY(), 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);

        TooltipRenderState.State render =
                TooltipRenderState.State.preview(false, forced, forcedBadges, 0, 0, elapsed());
        TooltipRenderState.run(render, () -> TooltipRenderer.render(
                context, client.textRenderer, stack, rawLines, provider, 0, 0, 32_768, 32_768));

        context.getMatrices().pop();
    }

    private long elapsed() {
        return System.currentTimeMillis() - shownAtMs;
    }

    /** Item ids matching {@code query}, for the field's suggestion list. */
    public static List<String> suggest(String query, int limit) {
        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        if (needle.isEmpty()) return List.of();
        return Registries.ITEM.getIds().stream()
                .map(Identifier::toString)
                .filter(id -> id.contains(needle))
                .sorted((a, b) -> {
                    boolean pa = a.startsWith(needle);
                    boolean pb = b.startsWith(needle);
                    if (pa != pb) return pa ? -1 : 1;
                    return a.compareTo(b);
                })
                .limit(limit)
                .toList();
    }

    /** Convenience for the "use held item" button. */
    public static Optional<ItemStack> heldStack() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Optional.empty();
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) held = client.player.getOffHandStack();
        return held.isEmpty() ? Optional.empty() : Optional.of(held);
    }
}
