package net.sweenus.simplytooltips.api;

import net.sweenus.simplytooltips.client.render.TooltipBatchExporter;

import java.nio.file.Path;
import java.util.List;

/** Client-only entry point for deterministic, full-height tooltip GIF exports. */
public final class TooltipExportApi {
    public static boolean startBatch(List<TooltipExportEntry> entries, Path outputDirectory,
                                     TooltipExportOptions options, TooltipExportListener listener) {
        return TooltipBatchExporter.start(entries, outputDirectory, options,
                listener == null ? TooltipExportListener.NONE : listener);
    }

    public static boolean isRunning() {
        return TooltipBatchExporter.isRunning();
    }

    public static void cancel() {
        TooltipBatchExporter.cancel();
    }

    private TooltipExportApi() {}
}
