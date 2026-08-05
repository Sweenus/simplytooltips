package net.sweenus.simplytooltips.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.tooltip.NativeComponentFilter;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Maps item IDs and item tags to rendering eligibility, theme keys and/or badge label lists, loaded from
 * {@code assets/simplytooltips/item_themes/<name>.json}.
 *
 * <p>Multiple files are supported: {@code items} entries are merged (later files win),
 * {@code components} and {@code tags} entries are concatenated in load order
 * (first match wins at resolve time).
 *
 * <h3>JSON format</h3>
 * Item values may be a plain theme-key string <em>or</em> an object with optional
 * {@code enabled}, {@code theme}, {@code border} and {@code badges} fields. These are independent axes — an entry
 * may set only a border, leaving the theme to be resolved as usual:
 * <pre>{@code
 * {
 *   "items": {
 *     "minecraft:iron_sword": "lightning",
 *     "minecraft:netherite_sword": { "theme": "lightning", "badges": ["SWORD", "NETHERITE"] },
 *     "examplemod:unmodified_item": { "enabled": false }
 *   },
 *   "namespaces": {
 *     "create": { "enabled": false }
 *   },
 *   "tooltip_components": {
 *     "com.example.tooltip.WidgetComponent": { "enabled": false },
 *     "com.example.tooltip.*":               { "enabled": false }
 *   },
 *   "components": [
 *     { "component": "mod:rarity=mod:rare", "border": "rarity_rare", "badges": ["RARE"] },
 *     { "component": "mod:charged", "theme": "lightning" }
 *   ],
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
    /** Exact item-ID → border key (may be present without a theme entry). */
    private static final Map<Identifier, String>       ITEM_BORDERS = new HashMap<>();
    /** Exact item-ID → rendering eligibility. */
    private static final Map<Identifier, Boolean>      ITEM_ENABLED = new HashMap<>();
    /** Item registry namespace → rendering eligibility. */
    private static final Map<String, Boolean>           NAMESPACE_ENABLED = new HashMap<>();
    /** Native tooltip component class-name pattern → rendering eligibility. */
    private static final Map<String, Boolean>           TOOLTIP_COMPONENT_ENABLED = new HashMap<>();

    /** Ordered component → (value?, theme key?, border key?, badge list?) entries. First match wins. */
    private static final List<ComponentEntry> COMPONENT_ENTRIES = new ArrayList<>();

    /** Ordered tag → (theme key?, border key?, badge list?) entries.  First match wins at resolve time. */
    private static final List<TagEntry> TAG_ENTRIES = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the theme key for the given stack, or {@code null} if no mapping exists.
     *
     * <p>Priority: first matching component → exact item-ID match → first matching tag.
     */
    public static @Nullable String resolveForStack(ItemStack stack) {
        String componentTheme = resolveComponentThemeForStack(stack);
        if (componentTheme != null) return componentTheme;
        String itemTheme = resolveItemThemeForStack(stack);
        if (itemTheme != null) return itemTheme;
        return resolveTagThemeForStack(stack);
    }

    /** Returns {@code true} if the stack has a theme mapping via component, exact item id, or tag. */
    public static boolean hasThemeForStack(ItemStack stack) {
        return resolveComponentThemeForStack(stack) != null
                || resolveItemThemeForStack(stack) != null
                || resolveTagThemeForStack(stack) != null;
    }

    /**
     * Returns whether Simply Tooltips is allowed to render this stack according to data rules.
     * Exact item rules override namespace rules; absent rules default to enabled.
     */
    public static boolean isEnabledForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;

        Identifier id = Registries.ITEM.getId(stack.getItem());
        Boolean itemEnabled = ITEM_ENABLED.get(id);
        if (itemEnabled != null) return itemEnabled;

        Boolean namespaceEnabled = NAMESPACE_ENABLED.get(id.getNamespace());
        return namespaceEnabled == null || namespaceEnabled;
    }

    /** Returns only the first matching component theme, or {@code null}. */
    public static @Nullable String resolveComponentThemeForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        for (ComponentEntry entry : COMPONENT_ENTRIES) {
            if (entry.themeKey() != null && componentEntryMatches(stack, entry))
                return entry.themeKey();
        }

        return null;
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
     * <p>Priority: first matching component with badges → exact item-ID match
     * → first matching tag that has a badges entry.
     */
    public static @Nullable List<String> resolveBadgesForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        List<String> componentBadges = resolveComponentBadgesForStack(stack);
        if (componentBadges != null) return componentBadges;

        Identifier id = Registries.ITEM.getId(stack.getItem());

        List<String> itemBadges = ITEM_BADGES.get(id);
        if (itemBadges != null) return itemBadges;

        for (TagEntry entry : TAG_ENTRIES) {
            if (entry.badges() != null && stack.isIn(entry.tag()))
                return entry.badges();
        }

        return null;
    }

    /**
     * Returns the border key for the given stack, or {@code null} if no mapping exists.
     *
     * <p>Priority: first matching component with a border → exact item-ID match → first matching tag
     * that has a border entry. Mirrors {@link #resolveBadgesForStack(ItemStack)}: a border is an
     * independent axis, so an entry can restyle only the frame and leave the theme alone. That is how
     * rarity mappings work — {@code { "component": "apotheosis:rarity=apotheosis:mythic",
     * "border": "rarity_mythic" }} keeps the item's own theme and changes only its border.
     */
    public static @Nullable String resolveBorderForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        for (ComponentEntry entry : COMPONENT_ENTRIES) {
            if (entry.borderKey() != null && componentEntryMatches(stack, entry))
                return entry.borderKey();
        }

        String itemBorder = ITEM_BORDERS.get(Registries.ITEM.getId(stack.getItem()));
        if (itemBorder != null) return itemBorder;

        for (TagEntry entry : TAG_ENTRIES) {
            if (entry.borderKey() != null && stack.isIn(entry.tag()))
                return entry.borderKey();
        }

        return null;
    }

    /** Returns only the first matching component badge list, or {@code null}. */
    public static @Nullable List<String> resolveComponentBadgesForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        for (ComponentEntry entry : COMPONENT_ENTRIES) {
            if (entry.badges() != null && componentEntryMatches(stack, entry))
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
        ITEM_BORDERS.clear();
        ITEM_ENABLED.clear();
        NAMESPACE_ENABLED.clear();
        TOOLTIP_COMPONENT_ENABLED.clear();
        COMPONENT_ENTRIES.clear();
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
                //   { "enabled": false, "theme": "optional_key", "border": "optional_key", "badges": ["A", "B"] }
                if (json.has("items") && json.get("items").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> itemEntry
                            : json.getAsJsonObject("items").entrySet()) {

                        Identifier itemId = Identifier.tryParse(itemEntry.getKey());
                        if (itemId == null) continue;

                        JsonElement val = itemEntry.getValue();
                        if (val.isJsonPrimitive()) {
                            // Plain string → theme key only
                            String themeKey = readString(val);
                            if (themeKey != null) ITEM_THEMES.put(itemId, themeKey);
                        } else if (val.isJsonObject()) {
                            JsonObject obj = val.getAsJsonObject();
                            String themeKey = readString(obj.get("theme"));
                            if (themeKey != null) ITEM_THEMES.put(itemId, themeKey);
                            String itemBorder = parseBorderKey(obj);
                            if (itemBorder != null) {
                                ITEM_BORDERS.put(itemId, itemBorder);
                            }
                            List<String> badges = parseBadgeArray(obj);
                            if (badges != null) {
                                ITEM_BADGES.put(itemId, badges);
                            }
                            Boolean enabled = readBoolean(obj.get("enabled"));
                            if (enabled != null) ITEM_ENABLED.put(itemId, enabled);
                        }
                    }
                }

                // --- "namespaces" section ---
                // Each value is an object: { "enabled": false }.
                // Exact item eligibility overrides a namespace rule.
                if (json.has("namespaces") && json.get("namespaces").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> namespaceEntry
                            : json.getAsJsonObject("namespaces").entrySet()) {
                        String namespace = namespaceEntry.getKey();
                        JsonElement val = namespaceEntry.getValue();
                        if (!isValidNamespace(namespace) || !val.isJsonObject()) continue;

                        Boolean enabled = readBoolean(val.getAsJsonObject().get("enabled"));
                        if (enabled != null) NAMESPACE_ENABLED.put(namespace, enabled);
                    }
                }

                // --- "tooltip_components" section ---
                // Blacklist for native tooltip components other mods contribute, keyed by
                // implementation class name (exact, or a package prefix ending in '.' or '*'):
                //   { "com.example.tooltip.*": { "enabled": false } }
                // Distinct from the "components" array below, which matches item DataComponents.
                if (json.has("tooltip_components") && json.get("tooltip_components").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> componentEntry
                            : json.getAsJsonObject("tooltip_components").entrySet()) {
                        String pattern = componentEntry.getKey();
                        JsonElement val = componentEntry.getValue();
                        if (!isValidClassPattern(pattern) || !val.isJsonObject()) continue;

                        Boolean enabled = readBoolean(val.getAsJsonObject().get("enabled"));
                        if (enabled != null) TOOLTIP_COMPONENT_ENABLED.put(pattern, enabled);
                    }
                }

                // --- "components" section ---
                // Each entry:
                //   { "component": "ns:id", "value": "optional_value", "theme": "optional_key",
                //     "border": "optional_key", "badges": ["A"] }
                // Shorthand is also supported:
                //   { "component": "ns:id=optional_value", "theme": "optional_key" }
                if (json.has("components") && json.get("components").isJsonArray()) {
                    for (JsonElement el : json.getAsJsonArray("components")) {
                        if (!el.isJsonObject()) continue;
                        JsonObject componentObj = el.getAsJsonObject();
                        ComponentEntry componentEntry = parseComponentEntry(componentObj);
                        if (componentEntry != null) {
                            COMPONENT_ENTRIES.add(componentEntry);
                        }
                    }
                }

                // --- "tags" section ---
                // Each entry: { "tag": "ns:id", "theme": "optional_key", "border": "optional_key", "badges": ["A"] }
                if (json.has("tags") && json.get("tags").isJsonArray()) {
                    for (JsonElement el : json.getAsJsonArray("tags")) {
                        if (!el.isJsonObject()) continue;
                        JsonObject tagObj = el.getAsJsonObject();
                        String tagText = readString(tagObj.get("tag"));
                        if (tagText == null) continue;

                        Identifier tagId = Identifier.tryParse(tagText);
                        if (tagId == null) continue;

                        String themeKey = readString(tagObj.get("theme"));
                        String borderKey = parseBorderKey(tagObj);
                        List<String> badges = parseBadgeArray(tagObj);

                        if (themeKey != null || borderKey != null || badges != null) {
                            TAG_ENTRIES.add(new TagEntry(
                                    TagKey.of(RegistryKeys.ITEM, tagId), themeKey, borderKey, badges));
                        }
                    }
                }

            } catch (Exception e) {
                SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to load item_themes '{}': {}",
                        entry.getKey(), e.getMessage());
            }
        }

        NativeComponentFilter.setRules(TOOLTIP_COMPONENT_ENABLED);

        SimplyTooltips.LOGGER.info("[SimplyTooltips] Loaded {} item theme(s), {} item badge override(s), {} item border override(s), {} item eligibility rule(s), {} namespace eligibility rule(s), {} tooltip component rule(s), {} component entries, {} tag entries",
                ITEM_THEMES.size(), ITEM_BADGES.size(), ITEM_BORDERS.size(),
                ITEM_ENABLED.size(), NAMESPACE_ENABLED.size(), TOOLTIP_COMPONENT_ENABLED.size(),
                COMPONENT_ENTRIES.size(), TAG_ENTRIES.size());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Reads an entry's border key, accepting {@code "border"} or the {@code "borderStyle"} spelling
     * used by theme files (where {@code border} is already the frame colour). Returns {@code null}
     * when neither is present.
     */
    private static @Nullable String parseBorderKey(JsonObject obj) {
        String border = readString(obj.get("border"));
        return border != null ? border : readString(obj.get("borderStyle"));
    }

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

    private static @Nullable ComponentEntry parseComponentEntry(JsonObject obj) {
        String componentText = readString(obj.get("component"));
        if (componentText == null) return null;

        String valueText = readString(obj.get("value"));

        int shorthandSeparator = componentText.indexOf('=');
        if (shorthandSeparator >= 0) {
            if (valueText == null) valueText = componentText.substring(shorthandSeparator + 1);
            componentText = componentText.substring(0, shorthandSeparator);
        }

        Identifier componentId = Identifier.tryParse(componentText);
        if (componentId == null) return null;

        String themeKey = readString(obj.get("theme"));
        String borderKey = parseBorderKey(obj);
        List<String> badges = parseBadgeArray(obj);

        if (themeKey == null && borderKey == null && badges == null) return null;
        return new ComponentEntry(componentId, valueText, themeKey, borderKey, badges);
    }

    private static @Nullable String readString(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return null;
        return element.getAsJsonPrimitive().isString() ? element.getAsString() : null;
    }

    private static @Nullable Boolean readBoolean(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return null;
        return element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : null;
    }

    private static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) return false;
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            if ((c < 'a' || c > 'z') && (c < '0' || c > '9')
                    && c != '_' && c != '-' && c != '.') return false;
        }
        return true;
    }

    /**
     * Validates a {@code tooltip_components} key: a Java class name, optionally ending in
     * {@code *} to match a package prefix. Rejects anything that could never name a class so a
     * typo is dropped at load time rather than silently never matching.
     */
    private static boolean isValidClassPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return false;
        String body = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        if (body.isEmpty()) return false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '_' && c != '$') return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean componentEntryMatches(ItemStack stack, ComponentEntry entry) {
        Optional<ComponentType<?>> componentType = Registries.DATA_COMPONENT_TYPE.getOrEmpty(entry.componentId());
        if (componentType.isEmpty()) return false;

        Object value = stack.get((ComponentType) componentType.get());
        if (value == null) return false;
        if (entry.valueKey() == null) return true;

        return componentValueStrings(componentType.get(), value).contains(entry.valueKey());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Set<String> componentValueStrings(ComponentType<?> componentType, Object value) {
        Set<String> values = new HashSet<>();
        values.add(String.valueOf(value));

        addIdentifierLikeMethodValue(values, value, "id");
        addIdentifierLikeMethodValue(values, value, "name");
        addIdentifierLikeMethodValue(values, value, "getId");

        try {
            Codec codec = componentType.getCodec();
            if (codec != null) {
                codec.encodeStart(JsonOps.INSTANCE, value)
                        .result()
                        .ifPresent(json -> addPrimitiveJsonValue(values, (JsonElement) json));
            }
        } catch (Exception ignored) {
            // Some component values are intentionally not persistently serializable.
        }

        return values;
    }

    private static void addIdentifierLikeMethodValue(Set<String> values, Object value, String methodName) {
        try {
            Method method = value.getClass().getMethod(methodName);
            if (method.getParameterCount() != 0) return;
            Object methodValue = method.invoke(value);
            if (methodValue != null) values.add(String.valueOf(methodValue));
        } catch (ReflectiveOperationException | SecurityException ignored) {
        }
    }

    private static void addPrimitiveJsonValue(Set<String> values, JsonElement json) {
        if (!json.isJsonPrimitive()) return;
        values.add(json.getAsString());
        values.add(json.toString());
    }

    private record ComponentEntry(
            Identifier componentId,
            @Nullable String valueKey,
            @Nullable String themeKey,
            @Nullable String borderKey,
            @Nullable List<String> badges) {}

    private record TagEntry(TagKey<Item> tag,
                            @Nullable String themeKey,
                            @Nullable String borderKey,
                            @Nullable List<String> badges) {}

    private ItemThemeRegistry() {}
}
