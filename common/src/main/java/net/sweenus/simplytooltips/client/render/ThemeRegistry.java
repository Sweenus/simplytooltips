package net.sweenus.simplytooltips.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.api.ThemeDefinition;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and caches {@link ThemeDefinition} instances from
 * {@code assets/simplytooltips/themes/<key>.json}.
 *
 * <p>Call {@link #loadAll(ResourceManager)} from platform-specific reload listeners.
 * Resource packs can override or add themes by placing files at the same path.
 */
public class ThemeRegistry {

    private static final Gson GSON = new Gson();
    private static final Map<String, ThemeDefinition> THEMES = new HashMap<>();
    private static final ThemeDefinition DEFAULT = ThemeDefinition.defaultDefinition();

    /**
     * Returns the {@link ThemeDefinition} for the given key, or the default definition
     * (golden theme, no motif) if the key is not found.
     */
    public static ThemeDefinition get(String key) {
        if (key == null) return DEFAULT;
        return THEMES.getOrDefault(key, DEFAULT);
    }

    /** The default theme colours (used as fallback in {@link #get(String)}). */
    public static TooltipTheme defaultColors() {
        return DEFAULT.colors();
    }

    /**
     * Scans {@code assets/simplytooltips/themes/} for {@code *.json} files,
     * parses each one, and rebuilds the internal theme map.
     * Safe to call repeatedly (clears the map first).
     *
     * <p>This method is synchronous and should be called from a background thread
     * (the "prepare" phase of the resource reload pipeline).
     */
    public static void loadAll(ResourceManager manager) {
        THEMES.clear();
        Map<Identifier, net.minecraft.resource.Resource> resources = manager.findResources(
                "themes",
                id -> id.getNamespace().equals(SimplyTooltips.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : resources.entrySet()) {
            String path     = entry.getKey().getPath();          // e.g. "themes/vine.json"
            String fileName = path.substring(path.lastIndexOf('/') + 1); // "vine.json"
            String key      = fileName.substring(0, fileName.length() - 5); // "vine"

            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) continue;

                TooltipTheme colors = TooltipTheme.fromJson(json);
                String motif           = str(json, "motif",           "none");
                String itemAnimStyle   = str(json, "itemAnimStyle",   "breathe_spin_bob");
                String titleAnimStyle  = str(json, "titleAnimStyle",  "wave");
                String itemBorderShape = str(json, "itemBorderShape", "diamond");

                List<String> customTextKeys = new ArrayList<>();
                if (json.has("customTextKeys")) {
                    JsonArray arr = json.getAsJsonArray("customTextKeys");
                    for (JsonElement el : arr) customTextKeys.add(el.getAsString());
                }

                THEMES.put(key, new ThemeDefinition(colors, motif, itemAnimStyle,
                        titleAnimStyle, itemBorderShape, customTextKeys));
            } catch (Exception e) {
                SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to load theme '{}': {}", key, e.getMessage());
            }
        }

        SimplyTooltips.LOGGER.info("[SimplyTooltips] Loaded {} theme(s)", THEMES.size());
    }

    private static String str(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private ThemeRegistry() {}
}
