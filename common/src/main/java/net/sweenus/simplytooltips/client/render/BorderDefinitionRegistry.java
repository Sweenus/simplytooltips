package net.sweenus.simplytooltips.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.api.BorderDefinition;
import net.sweenus.simplytooltips.api.BorderPalette;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches {@link BorderDefinition} instances from
 * {@code assets/simplytooltips/borders/<key>.json}.
 *
 * <p>Call {@link #loadAll(ResourceManager)} from platform-specific reload listeners.
 * Resource packs can override or add border definitions by placing files at the same path.
 */
public class BorderDefinitionRegistry {

    private static final Gson GSON = new Gson();
    private static final Map<String, BorderDefinition> DEFINITIONS = new HashMap<>();

    /**
     * Resolves a border key to a definition.
     *
     * <p>A loaded {@code borders/<key>.json} wins. Failing that, a key naming a registered pattern
     * (see {@link BorderRegistry}) resolves to that pattern with its own colours — which is why
     * {@code "border": "vine"} works without any border file existing. Anything else, including
     * {@code null} and {@code "none"}, yields {@link BorderDefinition#none()}: the plain themed
     * outline with no decoration.
     */
    public static BorderDefinition resolve(String key) {
        if (key == null || key.equals("none")) return BorderDefinition.none();

        BorderDefinition loaded = DEFINITIONS.get(key);
        if (loaded != null) return loaded;

        if (BorderRegistry.has(key)) return BorderDefinition.ofPattern(key);

        return BorderDefinition.none();
    }

    /**
     * Scans {@code assets/simplytooltips/borders/} for {@code *.json} files, parses each one,
     * and rebuilds the internal map. Safe to call repeatedly (clears the map first).
     *
     * <p>This method is synchronous and should be called from a background thread
     * (the "prepare" phase of the resource reload pipeline).
     */
    public static void loadAll(ResourceManager manager) {
        DEFINITIONS.clear();
        Map<Identifier, net.minecraft.resource.Resource> resources = manager.findResources(
                "borders",
                id -> id.getNamespace().equals(SimplyTooltips.MOD_ID) && id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : resources.entrySet()) {
            String path     = entry.getKey().getPath();                 // e.g. "borders/rarity_epic.json"
            String fileName = path.substring(path.lastIndexOf('/') + 1); // "rarity_epic.json"
            String key      = fileName.substring(0, fileName.length() - 5); // "rarity_epic"

            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) continue;

                String pattern = json.has("pattern") ? json.get("pattern").getAsString() : "none";
                DEFINITIONS.put(key, new BorderDefinition(pattern, BorderPalette.fromJson(json)));
            } catch (Exception e) {
                SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to load border '{}': {}", key, e.getMessage());
            }
        }

        SimplyTooltips.LOGGER.info("[SimplyTooltips] Loaded {} border definition(s)", DEFINITIONS.size());
    }

    private BorderDefinitionRegistry() {}
}
