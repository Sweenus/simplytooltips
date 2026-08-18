package net.sweenus.simplytooltips.client.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.sweenus.simplytooltips.api.ThemeDefinition;
import net.sweenus.simplytooltips.api.TooltipTheme;

/**
 * Writes a {@link ThemeDefinition} back out as theme JSON.
 *
 * <p>The inverse of {@code TooltipTheme.fromJson} plus {@code ThemeRegistry.parse}. Key order and
 * the {@code 0xAARRGGBB} colour spelling match the shipped {@code themes/*.json} files so a theme
 * saved from the Studio is indistinguishable from a hand-written one.
 */
public final class ThemeJson {

    /** Every key a theme file may contain, in the order they are written. */
    public static final String[] COLOR_KEYS = {
            "border", "borderInner", "bgTop", "bgBottom", "name", "badgeBg", "badgeCutout",
            "sectionHeader", "body", "separator", "diamondFrame", "diamondFrameInner", "footerDot",
            "stringColor", "frameColor", "runeColor", "slotFilled", "slotEmpty", "hint"
    };

    public static JsonObject toJson(ThemeDefinition def) {
        TooltipTheme c = def.colors();
        JsonObject json = new JsonObject();

        json.addProperty("border",            hex(c.border()));
        json.addProperty("borderInner",       hex(c.borderInner()));
        json.addProperty("bgTop",             hex(c.bgTop()));
        json.addProperty("bgBottom",          hex(c.bgBottom()));
        json.addProperty("name",              hex(c.name()));
        json.addProperty("badgeBg",           hex(c.badgeBg()));
        json.addProperty("badgeCutout",       hex(c.badgeCutout()));
        json.addProperty("sectionHeader",     hex(c.sectionHeader()));
        json.addProperty("body",              hex(c.body()));
        json.addProperty("separator",         hex(c.separator()));
        json.addProperty("diamondFrame",      hex(c.diamondFrame()));
        json.addProperty("diamondFrameInner", hex(c.diamondFrameInner()));
        json.addProperty("footerDot",         hex(c.footerDot()));
        json.addProperty("stringColor",       hex(c.stringColor()));
        json.addProperty("frameColor",        hex(c.frameColor()));
        json.addProperty("runeColor",         hex(c.runeColor()));
        json.addProperty("slotFilled",        hex(c.slotFilled()));
        json.addProperty("slotEmpty",         hex(c.slotEmpty()));
        json.addProperty("hint",              hex(c.hint()));

        json.addProperty("motif",           def.motif());
        json.addProperty("borderStyle",     def.border());
        json.addProperty("itemAnimStyle",   def.itemAnimStyle());
        json.addProperty("titleAnimStyle",  def.titleAnimStyle());
        json.addProperty("textShadow",      c.textShadow());
        json.addProperty("itemBorderShape", def.itemBorderShape());

        JsonArray custom = new JsonArray();
        for (String key : def.customTextKeys()) custom.add(key);
        json.add("customTextKeys", custom);

        return json;
    }

    /** Reads the colour component named by {@link #COLOR_KEYS}, so the editor can loop over them. */
    public static int colorOf(TooltipTheme t, String key) {
        return switch (key) {
            case "border"            -> t.border();
            case "borderInner"       -> t.borderInner();
            case "bgTop"             -> t.bgTop();
            case "bgBottom"          -> t.bgBottom();
            case "name"              -> t.name();
            case "badgeBg"           -> t.badgeBg();
            case "badgeCutout"       -> t.badgeCutout();
            case "sectionHeader"     -> t.sectionHeader();
            case "body"              -> t.body();
            case "separator"         -> t.separator();
            case "diamondFrame"      -> t.diamondFrame();
            case "diamondFrameInner" -> t.diamondFrameInner();
            case "footerDot"         -> t.footerDot();
            case "stringColor"       -> t.stringColor();
            case "frameColor"        -> t.frameColor();
            case "runeColor"         -> t.runeColor();
            case "slotFilled"        -> t.slotFilled();
            case "slotEmpty"         -> t.slotEmpty();
            case "hint"              -> t.hint();
            default -> throw new IllegalArgumentException("Unknown colour key: " + key);
        };
    }

    /** Returns a copy of {@code t} with the single colour named by {@code key} replaced. */
    public static TooltipTheme withColor(TooltipTheme t, String key, int value) {
        int[] v = new int[COLOR_KEYS.length];
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            v[i] = COLOR_KEYS[i].equals(key) ? value : colorOf(t, COLOR_KEYS[i]);
        }
        return new TooltipTheme(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9],
                v[10], v[11], v[12], v[13], v[14], v[15], v[16], v[17], v[18], t.textShadow());
    }

    /** Returns a copy of {@code t} with only {@code textShadow} replaced. */
    public static TooltipTheme withTextShadow(TooltipTheme t, boolean shadow) {
        return new TooltipTheme(t.border(), t.borderInner(), t.bgTop(), t.bgBottom(), t.name(),
                t.badgeBg(), t.badgeCutout(), t.sectionHeader(), t.body(), t.separator(),
                t.diamondFrame(), t.diamondFrameInner(), t.footerDot(), t.stringColor(),
                t.frameColor(), t.runeColor(), t.slotFilled(), t.slotEmpty(), t.hint(), shadow);
    }

    /** Formats an ARGB int the way theme files spell colours. */
    public static String hex(int argb) {
        return String.format("0x%08X", argb);
    }

    private ThemeJson() {}
}
