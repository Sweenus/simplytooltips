package net.sweenus.simplytooltips.api;

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
}
