package net.sweenus.simplytooltips.api;

import com.google.gson.JsonObject;

public record TooltipTheme(
        int border,
        int borderInner,
        int bgTop,
        int bgBottom,
        int name,
        int badgeBg,
        int badgeCutout,
        int sectionHeader,
        int body,
        int separator,
        int diamondFrame,
        int diamondFrameInner,
        int footerDot,
        int stringColor,
        int frameColor,
        int runeColor,
        int slotFilled,
        int slotEmpty,
        int hint
) {
    public static TooltipTheme defaultTheme() {
        return new TooltipTheme(
                0xFFE2A834, 0xFF8A6A1E, 0xF02E2210, 0xF0181208,
                0xFFFFF0CC, 0xFFEEEEEE, 0xFF141008, 0xFFFFD5A0,
                0xFFE6ECF5, 0xFF8A6A1E, 0xFFE2A834, 0xFF2A1E0A,
                0xFF8A6A1E, 0xFF9D62CA, 0xFF5E8ACF, 0xFFDB5E71,
                0xFFE2A834, 0xFF3D3020, 0xFFC7D2E2
        );
    }

    /**
     * Parses a {@link TooltipTheme} from a JSON object.
     * Each field is expected as a hex string (e.g. {@code "0xFFE2A834"}).
     * Missing fields fall back to the corresponding value from {@link #defaultTheme()}.
     */
    public static TooltipTheme fromJson(JsonObject json) {
        TooltipTheme d = defaultTheme();
        return new TooltipTheme(
                color(json, "border",            d.border()),
                color(json, "borderInner",       d.borderInner()),
                color(json, "bgTop",             d.bgTop()),
                color(json, "bgBottom",          d.bgBottom()),
                color(json, "name",              d.name()),
                color(json, "badgeBg",           d.badgeBg()),
                color(json, "badgeCutout",       d.badgeCutout()),
                color(json, "sectionHeader",     d.sectionHeader()),
                color(json, "body",              d.body()),
                color(json, "separator",         d.separator()),
                color(json, "diamondFrame",      d.diamondFrame()),
                color(json, "diamondFrameInner", d.diamondFrameInner()),
                color(json, "footerDot",         d.footerDot()),
                color(json, "stringColor",       d.stringColor()),
                color(json, "frameColor",        d.frameColor()),
                color(json, "runeColor",         d.runeColor()),
                color(json, "slotFilled",        d.slotFilled()),
                color(json, "slotEmpty",         d.slotEmpty()),
                color(json, "hint",              d.hint())
        );
    }

    private static int color(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        try {
            return (int) Long.parseLong(json.get(key).getAsString().replace("0x", ""), 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
