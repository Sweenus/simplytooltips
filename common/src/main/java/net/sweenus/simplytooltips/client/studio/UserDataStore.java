package net.sweenus.simplytooltips.client.studio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.api.ThemeDefinition;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads and writes the player's own themes and item mappings under
 * {@code <gamedir>/config/simplytooltips/}.
 *
 * <p>These files use exactly the schema of the resource-pack ones - {@code themes/<key>.json} and
 * {@code item_themes/<name>.json} - but live outside any pack, so the Theme Studio can save without
 * asking the player to enable a resource pack, and the result survives a resource reload.
 */
public final class UserDataStore {

    /** Theme and mapping file names are used as registry keys, so keep them to the safe set. */
    private static final Pattern SAFE_KEY = Pattern.compile("[a-z0-9_][a-z0-9_.-]*");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Gson PARSER = new Gson();

    /** File the Studio writes item assignments into. */
    public static final String STUDIO_MAPPING_FILE = "studio";

    public static Path root() {
        MinecraftClient client = MinecraftClient.getInstance();
        Path gameDir = client != null && client.runDirectory != null
                ? client.runDirectory.toPath()
                : Path.of(".");
        return gameDir.resolve("config").resolve(SimplyTooltips.MOD_ID);
    }

    public static Path themesDir() {
        return root().resolve("themes");
    }

    public static Path itemThemesDir() {
        return root().resolve("item_themes");
    }

    /** True if {@code key} is usable as both a file name and a theme key. */
    public static boolean isValidKey(String key) {
        return key != null && !key.isBlank() && SAFE_KEY.matcher(key).matches();
    }

    /** Normalises user input towards a valid key without silently accepting garbage. */
    public static String sanitizeKey(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    // ---- themes -------------------------------------------------------------------------------

    /** Parses every {@code config/simplytooltips/themes/*.json}. Never throws; bad files are logged. */
    public static Map<String, ThemeDefinition> loadThemes() {
        Map<String, ThemeDefinition> loaded = new HashMap<>();
        Path dir = themesDir();
        if (!Files.isDirectory(dir)) return loaded;

        try (Stream<Path> files = Files.list(dir)) {
            List<Path> jsonFiles = files
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            for (Path file : jsonFiles) {
                String fileName = file.getFileName().toString();
                String key = fileName.substring(0, fileName.length() - 5);
                if (!isValidKey(key)) {
                    SimplyTooltips.LOGGER.warn("[SimplyTooltips] Skipping user theme with unusable name '{}'", fileName);
                    continue;
                }
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject json = PARSER.fromJson(reader, JsonObject.class);
                    if (json == null) continue;
                    loaded.put(key, ThemeRegistry.parse(json));
                } catch (Exception e) {
                    SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to load user theme '{}': {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to scan user themes: {}", e.getMessage());
        }
        return loaded;
    }

    /** Writes {@code themes/<key>.json} and refreshes the registry overlay. */
    public static void saveTheme(String key, ThemeDefinition def) throws Exception {
        if (!isValidKey(key)) throw new IllegalArgumentException("Unusable theme key: " + key);
        Path file = themesDir().resolve(key + ".json");
        writeJson(file, ThemeJson.toJson(def));
        ThemeRegistry.reloadUser();
    }

    /** Removes {@code themes/<key>.json} and refreshes the registry overlay. */
    public static void deleteTheme(String key) throws Exception {
        if (!isValidKey(key)) return;
        Files.deleteIfExists(themesDir().resolve(key + ".json"));
        ThemeRegistry.reloadUser();
    }

    // ---- item mappings ------------------------------------------------------------------------

    /**
     * Parses every {@code config/simplytooltips/item_themes/*.json} in file-name order, for
     * {@code ItemThemeRegistry} to ingest after the resource-pack files.
     */
    public static List<JsonObject> loadItemThemeFiles() {
        List<JsonObject> loaded = new ArrayList<>();
        Path dir = itemThemesDir();
        if (!Files.isDirectory(dir)) return loaded;

        try (Stream<Path> files = Files.list(dir)) {
            List<Path> jsonFiles = files
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            for (Path file : jsonFiles) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject json = PARSER.fromJson(reader, JsonObject.class);
                    if (json != null) loaded.add(json);
                } catch (Exception e) {
                    SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to load user item mapping '{}': {}",
                            file.getFileName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to scan user item mappings: {}", e.getMessage());
        }
        return loaded;
    }

    /**
     * Adds or replaces one entry in {@code item_themes/studio.json}, preserving everything already
     * mapped there, then reloads the item-mapping overlay.
     *
     * @param badges replaces the provider's badges when non-empty; an empty list writes no badge
     *               override, which leaves the item's existing badges alone
     */
    public static void saveItemMapping(String itemId, String themeKey, List<String> badges) throws Exception {
        Path file = itemThemesDir().resolve(STUDIO_MAPPING_FILE + ".json");

        JsonObject root = readJson(file);
        if (root == null) root = new JsonObject();

        JsonObject items = root.has("items") && root.get("items").isJsonObject()
                ? root.getAsJsonObject("items")
                : new JsonObject();

        JsonObject entry = new JsonObject();
        entry.addProperty("theme", themeKey);
        if (badges != null && !badges.isEmpty()) {
            JsonArray array = new JsonArray();
            for (String badge : badges) array.add(badge);
            entry.add("badges", array);
        }
        items.add(itemId, entry);
        root.add("items", items);

        writeJson(file, root);
        net.sweenus.simplytooltips.client.render.ItemThemeRegistry.reloadUser();
    }

    /** Removes one entry from {@code item_themes/studio.json}, if present. */
    public static void removeItemMapping(String itemId) throws Exception {
        Path file = itemThemesDir().resolve(STUDIO_MAPPING_FILE + ".json");
        JsonObject root = readJson(file);
        if (root == null || !root.has("items") || !root.get("items").isJsonObject()) return;

        JsonObject items = root.getAsJsonObject("items");
        if (items.remove(itemId) == null) return;

        writeJson(file, root);
        net.sweenus.simplytooltips.client.render.ItemThemeRegistry.reloadUser();
    }

    /** The theme this item is assigned in {@code studio.json}, or {@code null}. */
    public static String studioMappingFor(String itemId) {
        JsonObject root = readJson(itemThemesDir().resolve(STUDIO_MAPPING_FILE + ".json"));
        if (root == null || !root.has("items") || !root.get("items").isJsonObject()) return null;
        JsonObject items = root.getAsJsonObject("items");
        if (!items.has(itemId) || !items.get(itemId).isJsonObject()) return null;
        JsonObject entry = items.getAsJsonObject(itemId);
        return entry.has("theme") ? entry.get("theme").getAsString() : null;
    }

    // ---- io -----------------------------------------------------------------------------------

    private static JsonObject readJson(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return PARSER.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            SimplyTooltips.LOGGER.error("[SimplyTooltips] Failed to read '{}': {}", file, e.getMessage());
            return null;
        }
    }

    /** Writes through a sibling temp file so an interrupted save cannot truncate a good theme. */
    private static void writeJson(Path file, JsonObject json) throws Exception {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(json));
            writer.write('\n');
        }

        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private UserDataStore() {}
}
