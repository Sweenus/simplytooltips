package net.sweenus.simplytooltips.api;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * The colours a border pattern draws with.
 *
 * <p>Every component is nullable, where {@code null} means "not specified". A pattern's
 * {@link net.sweenus.simplytooltips.client.render.border.BorderPattern#defaultPalette} supplies a fully
 * populated palette; a {@code borders/<key>.json} file supplies a sparse one whose set components are
 * layered on top via {@link #over(BorderPalette)}. That layering is what keeps existing visuals byte
 * identical: a border definition that names only a pattern inherits every colour from that pattern.
 *
 * <p>{@code frame} and {@code frameInner} restyle the panel outline itself (the lines and corner
 * diamonds drawn by {@link net.sweenus.simplytooltips.client.render.BorderRenderer}); when unset the
 * theme's {@code border} / {@code borderInner} colours are used. The five accents are pattern-specific
 * decoration colours — see each pattern class for what it maps them to.
 */
public record BorderPalette(
        @Nullable Integer frame,
        @Nullable Integer frameInner,
        @Nullable Integer accentA,
        @Nullable Integer accentB,
        @Nullable Integer accentC,
        @Nullable Integer accentD,
        @Nullable Integer accentE
) {
    /** A palette that specifies nothing; layering it over another is a no-op. */
    public static final BorderPalette EMPTY =
            new BorderPalette(null, null, null, null, null, null, null);

    public static BorderPalette accents(int a) {
        return new BorderPalette(null, null, a, null, null, null, null);
    }

    public static BorderPalette accents(int a, int b) {
        return new BorderPalette(null, null, a, b, null, null, null);
    }

    public static BorderPalette accents(int a, int b, int c) {
        return new BorderPalette(null, null, a, b, c, null, null);
    }

    public static BorderPalette accents(int a, int b, int c, int d) {
        return new BorderPalette(null, null, a, b, c, d, null);
    }

    public static BorderPalette accents(int a, int b, int c, int d, int e) {
        return new BorderPalette(null, null, a, b, c, d, e);
    }

    /**
     * Returns this palette with every unset component filled in from {@code base}.
     * Components set on this palette always win.
     */
    public BorderPalette over(BorderPalette base) {
        if (base == null) return this;
        return new BorderPalette(
                frame       != null ? frame       : base.frame(),
                frameInner  != null ? frameInner  : base.frameInner(),
                accentA     != null ? accentA     : base.accentA(),
                accentB     != null ? accentB     : base.accentB(),
                accentC     != null ? accentC     : base.accentC(),
                accentD     != null ? accentD     : base.accentD(),
                accentE     != null ? accentE     : base.accentE()
        );
    }

    /** Outline colour, or {@code fallback} (normally the theme's {@code border}) when unset. */
    public int frameOr(int fallback) {
        return frame != null ? frame : fallback;
    }

    /** Inner highlight colour, or {@code fallback} (normally the theme's {@code borderInner}) when unset. */
    public int frameInnerOr(int fallback) {
        return frameInner != null ? frameInner : fallback;
    }

    /** Accent A, or fully transparent when unset. */
    public int a() { return accentA != null ? accentA : 0; }

    /** Accent B, or fully transparent when unset. */
    public int b() { return accentB != null ? accentB : 0; }

    /** Accent C, or fully transparent when unset. */
    public int c() { return accentC != null ? accentC : 0; }

    /** Accent D, or fully transparent when unset. */
    public int d() { return accentD != null ? accentD : 0; }

    /** Accent E, or fully transparent when unset. */
    public int e() { return accentE != null ? accentE : 0; }

    /**
     * Reads a sparse palette from a border definition JSON object. Colours use the same
     * {@code "0xAARRGGBB"} hex-string form as theme files; absent or unparsable keys stay unset.
     */
    public static BorderPalette fromJson(JsonObject json) {
        return new BorderPalette(
                TooltipTheme.colorOrNull(json, "frame"),
                TooltipTheme.colorOrNull(json, "frameInner"),
                TooltipTheme.colorOrNull(json, "accentA"),
                TooltipTheme.colorOrNull(json, "accentB"),
                TooltipTheme.colorOrNull(json, "accentC"),
                TooltipTheme.colorOrNull(json, "accentD"),
                TooltipTheme.colorOrNull(json, "accentE")
        );
    }
}
