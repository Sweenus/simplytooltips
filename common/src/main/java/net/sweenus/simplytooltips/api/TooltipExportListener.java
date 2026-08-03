package net.sweenus.simplytooltips.api;

import java.nio.file.Path;

/** Receives batch export progress on the Minecraft client thread. */
public interface TooltipExportListener {
    TooltipExportListener NONE = new TooltipExportListener() {};

    default void onStarted(int total, Path outputDirectory) {}
    default void onProgress(int completed, int total, String outputName) {}
    default void onItemFailed(String outputName, Throwable error) {}
    default void onCompleted(int succeeded, int failed, Path outputDirectory) {}
    default void onCancelled(int completed, int total, Path outputDirectory) {}
}
