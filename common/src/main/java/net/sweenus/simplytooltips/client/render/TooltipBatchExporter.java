package net.sweenus.simplytooltips.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.api.TooltipExportEntry;
import net.sweenus.simplytooltips.api.TooltipExportListener;
import net.sweenus.simplytooltips.api.TooltipExportOptions;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Generates deterministic, full-height tooltip GIFs without exposing the game framebuffer. */
public final class TooltipBatchExporter {
    private static final int FRAMES_PER_TICK = 3;
    private static final long ANIMATION_TIME_BASE_MS = 1_000_000L;
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "simplytooltips-batch-gif-writer");
        thread.setDaemon(true);
        return thread;
    });

    private static Batch active;

    public static boolean start(List<TooltipExportEntry> entries, Path outputDirectory,
                                TooltipExportOptions options, TooltipExportListener listener) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || entries == null || entries.isEmpty() || active != null) {
            return false;
        }
        active = new Batch(client, List.copyOf(entries), outputDirectory.toAbsolutePath().normalize(), options, listener);
        active.notifyStarted();
        return true;
    }

    public static boolean isRunning() {
        return active != null;
    }

    public static void cancel() {
        if (active != null) active.cancelRequested = true;
    }

    public static void tick() {
        if (active == null) return;
        try {
            active.tick();
        } catch (Throwable error) {
            active.failCurrent(error);
        }
    }

    private static final class Batch {
        private final MinecraftClient client;
        private final List<TooltipExportEntry> entries;
        private final Path outputDirectory;
        private final TooltipExportOptions options;
        private final TooltipExportListener listener;

        private int entryIndex;
        private int succeeded;
        private int failed;
        private boolean cancelRequested;

        private TooltipExportEntry entry;
        private ItemStack stack;
        private List<Text> rawLines;
        private TooltipProvider provider;
        private int canvasWidth;
        private int canvasHeight;
        private int frameIndex;
        private List<BufferedImage> frames;
        private SimpleFramebuffer framebuffer;
        private CompletableFuture<Throwable> pendingWrite;

        private Batch(MinecraftClient client, List<TooltipExportEntry> entries, Path outputDirectory,
                      TooltipExportOptions options, TooltipExportListener listener) {
            this.client = client;
            this.entries = entries;
            this.outputDirectory = outputDirectory;
            this.options = options;
            this.listener = listener;
        }

        private void notifyStarted() {
            safe(() -> listener.onStarted(entries.size(), outputDirectory));
        }

        private void tick() {
            if (pendingWrite != null) {
                if (!pendingWrite.isDone()) return;
                Throwable error = pendingWrite.join();
                pendingWrite = null;
                if (error == null) succeeded++;
                else {
                    failed++;
                    safe(() -> listener.onItemFailed(entry.outputName(), error));
                }
                entryIndex++;
                int completed = succeeded + failed;
                String name = entry.outputName();
                safe(() -> listener.onProgress(completed, entries.size(), name));
                resetEntry();
            }

            if (cancelRequested) {
                finishCancelled();
                return;
            }
            if (entryIndex >= entries.size()) {
                finishCompleted();
                return;
            }
            if (entry == null) prepareEntry();
            if (entry == null) return;

            int rendered = 0;
            while (rendered < FRAMES_PER_TICK && frameIndex < options.frameCount()) {
                long elapsedMs = Math.round(frameIndex * (1000.0 / options.framesPerSecond()));
                frames.add(renderFrame(elapsedMs));
                frameIndex++;
                rendered++;
            }

            if (frameIndex >= options.frameCount()) {
                disposeFramebuffer();
                List<BufferedImage> completedFrames = List.copyOf(frames);
                Path output = outputDirectory.resolve(entry.outputName() + ".gif");
                int delayCs = Math.max(1, Math.round(100.0F / options.framesPerSecond()));
                pendingWrite = CompletableFuture.supplyAsync(() -> {
                    try {
                        AnimatedGifWriter.writeAtomically(completedFrames, output, delayCs);
                        return null;
                    } catch (Throwable error) {
                        return error;
                    }
                }, WRITER);
            }
        }

        private void prepareEntry() {
            entry = entries.get(entryIndex);
            stack = entry.stack().copy();
            rawLines = Screen.getTooltipFromItem(client, stack);
            Optional<TooltipProvider> found = TooltipProviderRegistry.find(stack);
            if (rawLines == null || rawLines.isEmpty() || found.isEmpty()) {
                failCurrent(new IllegalStateException("No Simply Tooltips provider or tooltip text for " + entry.outputName()));
                return;
            }
            provider = found.get();

            VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
            DrawContext context = new DrawContext(client, consumers);
            TooltipExportRenderState.State measure = new TooltipExportRenderState.State(
                    true, 0L, ANIMATION_TIME_BASE_MS, options.margin());
            TooltipExportRenderState.run(measure, () -> TooltipRenderer.render(
                    context, client.textRenderer, stack, rawLines, provider,
                    0, 0, 32_768, 32_768));
            canvasWidth = measure.canvasWidth;
            canvasHeight = measure.canvasHeight;
            if (canvasWidth <= 0 || canvasHeight <= 0) {
                failCurrent(new IllegalStateException("Tooltip measurement returned an empty canvas"));
                return;
            }

            int physicalWidth = canvasWidth * options.outputScale();
            int physicalHeight = canvasHeight * options.outputScale();
            int maxTexture = RenderSystem.maxSupportedTextureSize();
            if (physicalWidth > maxTexture || physicalHeight > maxTexture) {
                failCurrent(new IllegalStateException("Tooltip exceeds GPU texture limit: "
                        + physicalWidth + "x" + physicalHeight + " > " + maxTexture));
                return;
            }
            framebuffer = new SimpleFramebuffer(physicalWidth, physicalHeight, true, MinecraftClient.IS_SYSTEM_MAC);
            frames = new ArrayList<>(options.frameCount());
            frameIndex = 0;
        }

        private BufferedImage renderFrame(long elapsedMs) {
            RenderSystem.assertOnRenderThread();
            int matte = options.matteArgb();
            framebuffer.setClearColor(
                    ((matte >> 16) & 0xFF) / 255.0F,
                    ((matte >> 8) & 0xFF) / 255.0F,
                    (matte & 0xFF) / 255.0F,
                    1.0F);
            framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            framebuffer.beginWrite(true);

            RenderSystem.backupProjectionMatrix();
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            try {
                Matrix4f projection = new Matrix4f().setOrtho(
                        0.0F, canvasWidth, canvasHeight, 0.0F, 1000.0F, 21000.0F);
                RenderSystem.setProjectionMatrix(projection, VertexSorter.BY_Z);
                modelView.translation(0.0F, 0.0F, -11000.0F);
                RenderSystem.applyModelViewMatrix();
                DiffuseLighting.enableGuiDepthLighting();

                VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
                DrawContext context = new DrawContext(client, consumers);
                TooltipExportRenderState.State render = new TooltipExportRenderState.State(
                        false, elapsedMs, ANIMATION_TIME_BASE_MS + elapsedMs, options.margin());
                TooltipExportRenderState.run(render, () -> TooltipRenderer.render(
                        context, client.textRenderer, stack, rawLines, provider,
                        0, 0, canvasWidth, canvasHeight));
                context.draw();

                NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(framebuffer);
                try {
                    return toBufferedImage(nativeImage);
                } finally {
                    nativeImage.close();
                }
            } finally {
                modelView.popMatrix();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.restoreProjectionMatrix();
                client.getFramebuffer().beginWrite(true);
            }
        }

        private BufferedImage toBufferedImage(NativeImage image) {
            BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getColor(x, y);
                    int a = (abgr >>> 24) & 0xFF;
                    int b = (abgr >>> 16) & 0xFF;
                    int g = (abgr >>> 8) & 0xFF;
                    int r = abgr & 0xFF;
                    result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            return result;
        }

        private void failCurrent(Throwable error) {
            String name = entry != null ? entry.outputName() : "unknown";
            failed++;
            entryIndex++;
            safe(() -> listener.onItemFailed(name, error));
            int completed = succeeded + failed;
            safe(() -> listener.onProgress(completed, entries.size(), name));
            resetEntry();
            if (entryIndex >= entries.size()) finishCompleted();
        }

        private void resetEntry() {
            disposeFramebuffer();
            entry = null;
            stack = null;
            rawLines = null;
            provider = null;
            frames = null;
            frameIndex = 0;
            canvasWidth = 0;
            canvasHeight = 0;
        }

        private void disposeFramebuffer() {
            if (framebuffer != null) {
                framebuffer.delete();
                framebuffer = null;
            }
        }

        private void finishCompleted() {
            disposeFramebuffer();
            int successCount = succeeded;
            int failureCount = failed;
            safe(() -> listener.onCompleted(successCount, failureCount, outputDirectory));
            active = null;
        }

        private void finishCancelled() {
            disposeFramebuffer();
            int completed = succeeded + failed;
            safe(() -> listener.onCancelled(completed, entries.size(), outputDirectory));
            active = null;
        }

        private void safe(Runnable callback) {
            try {
                callback.run();
            } catch (Throwable ignored) {
            }
        }
    }

    private TooltipBatchExporter() {}
}
