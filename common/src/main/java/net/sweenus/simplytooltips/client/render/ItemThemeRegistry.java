package net.sweenus.simplytooltips.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.SimplyTooltips;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.*;

/**
 * Maps item IDs and item tags to theme keys and/or badge label lists, loaded from
 * {@code assets/simplytooltips/item_themes/<name>.json}.
 *
 * <p>Multiple files are supported: {@code items} entries are merged (later files win),
 * {@code tags} entries are concatenated in load order (first match wins at resolve time).
 *
 * <h3>JSON format</h3>
 * Item values may be a plain theme-key string <em>or</em> an object with optional
 * {@code theme} and {@code badges} fields:
 * <pre>{@code
 * {
 *   "items": {
 *     "minecraft:iron_sword": "lightning",
 *     "minecraft:netherite_sword": { "theme": "lightning", "badges": ["SWORD", "NETHERITE"] }
 *   },
 *   "tags": [
 *     { "tag": "minecraft:swords",   "theme": "lightning", "badges": ["SWORD"] },
 *     { "tag": "minecraft:pickaxes", "theme": "earth" }
 *   ]
 * }
 * }</pre>
 *
 * <p>Call {@link #loadAll(ResourceManager)} from platform-specific reload listeners.
 * Resource packs can add/override mappings by placing files at the same path.
 */
public final class ItemThemeRegistry {

    private static final Gson GSON = new Gson();

    /** Exact item-ID → theme key. */
    private static final Map<Identifier, String>       ITEM_THEMES = new HashMap<>();
    /** Exact item-ID → badge list (may be present without a theme entry). */
    private static final Map<Identifier, List<String>> ITEM_BADGES = new HashMap<>();

    /** Ordered tag → (theme key?, badge list?) entries.  First match wins at resolve time. */
    private static final List<TagEntry> TAG_ENTRIES = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the theme key for the given stack, or {@code null} if no mapping exists.
     *
     * <p>Priority: exact item-ID match → first matching tag.
     */
    public static @Nullable String resolveForStack(ItemStack stack) {
        String itemTheme = resolveItemThemeForStack(stack);
        if (itemTheme != null) return itemTheme;
        return resolveTagThemeForStack(stack);
    }

    /** Returns only an exact item-ID theme match, or {@code null}. */
    public static @Nullable String resolveItemThemeForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        Identifier id = Registries.ITEM.getId(stack.getItem());
        return ITEM_THEMES.get(id);
    }

    /** Returns only the first matching tag theme, or {@code null}. */
    public static @Nullable String resolveTagThemeForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        for (TagEntry entry : TAG_ENTRIES) {
            if (entry.themeKey() != null && stack.isIn(entry.tag()))
                return entry.themeKey();
        }

        return null;
    }

    /**
     * Returns the badge list for the given stack, or {@code null} if no override exists.
     *
     * <p>Priority: exact item-ID match → first matching tag that has a badges entry.
     */
    public static @Nullable List<String> resolveBadgesForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        Identifier id = Registries.ITEM.getId(stack.getItem());

        List<String> itemBadges = ITEM_BADGES.get(id);
        if (itemBadges != null) return itemBadges;

        for (TagEntry entry : TAG_ENTRIES) {
            if (entry.badges() != null && stack.isIn(entry.tag()))
                return entry.badges();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    /**
     * Scans {@code assets/simplytooltips/item_themes/} for {@code *.json} files and
     * rebuilds the internal maps.  Safe to call repeatedly (clears maps first).
     */
    public static void loadAll(ResourceManager manager) {
        ITEM_THEMES.clear();
        ITEM_BADGES.clear();
        TAG_ENTRIES.clear();

        Map<Identifier, net.minecraft.resource.Resource> resources = manager.findResources(
                "item_themes",
                id -> id.getNamespace().equals(SimplyTooltips.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) continue;

                // --- "items" section ---
                // Each value is either a plain theme-key string or an object:
                //   { "theme": "optional_key", "badges": ["A", "B"] }
                if (json.has("items") && json.get("items").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> itemEntry
                            : json.getAsJsonObject("items").entrySet()) {

                        Identifier itemId = Identifier.tryParse(itemEntry.getKey());
                        if (itemId == null) continue;

                        JsonElement val = itemEntry.getValue();
                        if (val.isJsonPrimitive()) {
                            // Plain string → theme key only
                            ITEM_THEMES.put(itemId, val.getAsString());
                        } else if (val.isJsonObject()) {
                            JsonObject obj = val.getAsJsonObject();
                            if (obj.has("theme")) {
                                ITEM_THEMES.put(itemId, obj.get("theme").getAsString());
                            }
                            List<String> badges = parseBadgeArray(obj);
                            if (badges != null) {
                                ITEM_BADGES.put(itemId, badges);
                            }
                        }
                    }
                }

                // --- "tags" section ---
                // Each entry: { "tag": "ns:id", "theme": "optional_key", "badges": ["A"] }
                if (json.has("tags") && json.get("tags").isJsonArray()) {
                    for (JsonElement el : json.getAsJsonArray("tags")) {
                        if (!el.isJsonObject()) continue;
                        JsonObject tagObj = el.getAsJsonObject();
                        if (!tagObj.has("tag")) continue;

                        Identifier tagId = Identifier.tryParse(tagObj.get("tag").getAsString());
                        if (tagId == null) continue;

                        String themeKey = tagObj.has("theme")
                                ? tagObj.get("theme").getAsString() : null;
                        List<String> badges = parseBadgeArray(tagObj);

                        if (themeKey != null || badges != null) {
                            TAG_ENTRIES.add(new TagEntry(
                                    TagKey.of(RegistryKeys.ITEM, tagId), themeKey, badges));
                        }
                    }
                }

            } catch (Exception e) {
                SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to load item_themes '{}': {}",
                        entry.getKey(), e.getMessage());
            }
        }

        SimplyTooltips.LOGGER.info("[SimplyTooltips] Loaded {} item theme(s), {} item badge override(s), {} tag entries",
                ITEM_THEMES.size(), ITEM_BADGES.size(), TAG_ENTRIES.size());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Reads the {@code "badges"} string array from a JSON object, or returns {@code null}. */
    private static @Nullable List<String> parseBadgeArray(JsonObject obj) {
        if (!obj.has("badges") || !obj.get("badges").isJsonArray()) return null;
        JsonArray arr = obj.getAsJsonArray("badges");
        List<String> list = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            if (el.isJsonPrimitive()) list.add(el.getAsString());
        }
        return list.isEmpty() ? null : Collections.unmodifiableList(list);
    }

    private record TagEntry(TagKey<Item> tag, @Nullable String themeKey, @Nullable List<String> badges) {}

    private ItemThemeRegistry() {}
}
