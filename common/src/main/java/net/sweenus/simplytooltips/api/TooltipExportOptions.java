package net.sweenus.simplytooltips.api;

/** Rendering and animation settings for a tooltip batch export. */
public record TooltipExportOptions(
        int durationMs,
        int framesPerSecond,
        int outputScale,
        int matteArgb,
        int margin
) {
    public static TooltipExportOptions documentationDefaults() {
        return new TooltipExportOptions(4_000, 15, 2, 0xFF0C0E14, 6);
    }

    public TooltipExportOptions {
        if (durationMs < 100 || durationMs > 30_000) {
            throw new IllegalArgumentException("durationMs must be between 100 and 30000");
        }
        if (framesPerSecond < 1 || framesPerSecond > 60) {
            throw new IllegalArgumentException("framesPerSecond must be between 1 and 60");
        }
        if (outputScale < 1 || outputScale > 4) {
            throw new IllegalArgumentException("outputScale must be between 1 and 4");
        }
        if (margin < 0 || margin > 32) {
            throw new IllegalArgumentException("margin must be between 0 and 32");
        }
        matteArgb |= 0xFF000000;
    }

    public int frameCount() {
        return Math.max(1, (int) Math.ceil(durationMs * framesPerSecond / 1000.0));
    }
}
