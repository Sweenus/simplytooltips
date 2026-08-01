package net.sweenus.simplytooltips.api;

/**
 * A data-driven border: which pattern to draw, plus optional colour overrides for it.
 * Instances are loaded from {@code assets/simplytooltips/borders/<key>.json}.
 *
 * <p>{@code pattern} is a key into
 * {@link net.sweenus.simplytooltips.client.render.BorderRegistry} — either one of the built-ins
 * (which share their keys with the motifs: {@code vine}, {@code ember}, {@code jade}, …) or one a mod
 * registered itself. {@code "none"} or an unknown key draws the plain outline with no decoration.
 *
 * <p>{@code palette} is sparse: only the colours the JSON actually set. It is layered over the
 * pattern's own default palette at draw time, so a definition that names only a pattern renders
 * exactly like that built-in pattern always has.
 */
public record BorderDefinition(String pattern, BorderPalette palette) {

    /** No decoration, no colour overrides — the plain themed outline. */
    public static BorderDefinition none() {
        return new BorderDefinition("none", BorderPalette.EMPTY);
    }

    /** A built-in pattern used with its own colours. */
    public static BorderDefinition ofPattern(String pattern) {
        return new BorderDefinition(pattern, BorderPalette.EMPTY);
    }
}
