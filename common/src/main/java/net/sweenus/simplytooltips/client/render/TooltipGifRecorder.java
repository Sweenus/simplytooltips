package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Captures a cropped, looping GIF of the on-screen tooltip as rendered in-game.
 */
public final class TooltipGifRecorder {

    private static final int CAPTURE_DURATION_MS = 4000;
    private static final int TARGET_FPS = 20;
    private static final int FRAME_DELAY_CS = 100 / TARGET_FPS;
    private static final int FRAME_INTERVAL_MS = 1000 / TARGET_FPS;
    private static final int TARGET_FRAME_COUNT = CAPTURE_DURATION_MS / FRAME_INTERVAL_MS;
    private static final long TOOLTIP_ACTIVE_TTL_MS = 250L;

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "simplytooltips-gif-writer");
        t.setDaemon(true);
        return t;
    });

    private static boolean recording = false;
    private static boolean writing = false;

    private static long lastTooltipRenderMs = 0L;
    private static int tooltipX = 0;
    private static int tooltipY = 0;
    private static int tooltipW = 0;
    private static int tooltipH = 0;

    private static long recordingStartMs = 0L;
    private static long nextCaptureMs = 0L;
    private static final List<BufferedImage> frames = new ArrayList<>(TARGET_FRAME_COUNT);

    public static void markTooltipRendered(int x, int y, int w, int h) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        long now = System.currentTimeMillis();
        synchronized (TooltipGifRecorder.class) {
            lastTooltipRenderMs = now;
            tooltipX = x;
            tooltipY = y;
            tooltipW = w;
            tooltipH = h;

            if (recording) {
                captureDueFrames(client, now);
                if (shouldFinish(now)) {
                    finishRecording(client);
                }
            }
        }
    }

    public static void requestCapture() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        synchronized (TooltipGifRecorder.class) {
            long now = System.currentTimeMillis();
            if (writing || recording) {
                chat(client, Text.literal("[Simply Tooltips] GIF capture is already in progress."));
                return;
            }

            if (!isTooltipActive(now)) {
                chat(client, Text.literal("[Simply Tooltips] No tooltip is currently visible to capture."));
                return;
            }

            recording = true;
            recordingStartMs = now;
            nextCaptureMs = now;
            frames.clear();

            chat(client, Text.literal("[Simply Tooltips] Recording tooltip GIF (6.0s @ 20 FPS)..."));
            captureDueFrames(client, now);
            if (shouldFinish(now)) {
                finishRecording(client);
            }
        }
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        synchronized (TooltipGifRecorder.class) {
            if (!recording) return;
            long now = System.currentTimeMillis();

            if (!isTooltipActive(now)) {
                recording = false;
                frames.clear();
                chat(client, Text.literal("[Simply Tooltips] Capture cancelled: tooltip is no longer visible."));
                return;
            }

            if (shouldFinish(now)) {
                finishRecording(client);
            }
        }
    }

    private static boolean isTooltipActive(long now) {
        return (now - lastTooltipRenderMs) <= TOOLTIP_ACTIVE_TTL_MS && tooltipW > 0 && tooltipH > 0;
    }

    private static boolean shouldFinish(long now) {
        return (now - recordingStartMs) >= CAPTURE_DURATION_MS || frames.size() >= TARGET_FRAME_COUNT;
    }

    private static void captureDueFrames(MinecraftClient client, long now) {
        if (tooltipW <= 0 || tooltipH <= 0) return;

        while (now >= nextCaptureMs && frames.size() < TARGET_FRAME_COUNT) {
            BufferedImage frame = grabTooltipFrame(client, tooltipX, tooltipY, tooltipW, tooltipH);
            if (frame == null) {
                recording = false;
                frames.clear();
                chat(client, Text.literal("[Simply Tooltips] Failed to capture tooltip frame."));
                return;
            }
            frames.add(frame);
            nextCaptureMs += FRAME_INTERVAL_MS;
        }
    }

    private static BufferedImage grabTooltipFrame(MinecraftClient client, int x, int y, int w, int h) {
        NativeImage full = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
        if (full == null) return null;

        try {
            int maxW = full.getWidth();
            int maxH = full.getHeight();

            double scaleX = maxW / (double) client.getWindow().getScaledWidth();
            double scaleY = maxH / (double) client.getWindow().getScaledHeight();

            int startX = Math.max(0, (int) Math.floor(x * scaleX));
            int startY = Math.max(0, (int) Math.floor(y * scaleY));
            int endX = Math.min(maxW, (int) Math.ceil((x + w) * scaleX));
            int endY = Math.min(maxH, (int) Math.ceil((y + h) * scaleY));
            int outW = endX - startX;
            int outH = endY - startY;
            if (outW <= 0 || outH <= 0) return null;

            BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < outH; py++) {
                for (int px = 0; px < outW; px++) {
                    int abgr = full.getColor(startX + px, startY + py);
                    int argb = abgrToArgb(abgr);
                    out.setRGB(px, py, argb);
                }
            }
            return out;
        } finally {
            full.close();
        }
    }

    private static int abgrToArgb(int abgr) {
        int a = (abgr >>> 24) & 0xFF;
        int b = (abgr >>> 16) & 0xFF;
        int g = (abgr >>> 8) & 0xFF;
        int r = abgr & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void finishRecording(MinecraftClient client) {
        recording = false;
        if (frames.isEmpty()) {
            chat(client, Text.literal("[Simply Tooltips] GIF capture finished, but no frames were recorded."));
            return;
        }

        List<BufferedImage> frameCopy = new ArrayList<>(frames);
        frames.clear();
        writing = true;

        Path outputDir = client.runDirectory.toPath().resolve("screenshots").resolve("simplytooltips");
        Path outputFile = outputDir.resolve("tooltip-" + LocalDateTime.now().format(FILE_TS) + ".gif");

        IO_EXECUTOR.execute(() -> {
            Throwable error = null;
            try {
                Files.createDirectories(outputDir);
                writeGif(frameCopy, outputFile);
            } catch (Throwable t) {
                error = t;
            }

            Throwable finalError = error;
            client.execute(() -> {
                synchronized (TooltipGifRecorder.class) {
                    writing = false;
                }
                if (client.player == null) return;

                if (finalError == null) {
                    Text dirLink = Text.literal(outputDir.toString()).setStyle(
                            Style.EMPTY.withColor(Formatting.AQUA)
                                    .withUnderline(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputDir.toString()))
                    );
                    chat(client, Text.literal("[Simply Tooltips] Saved looping tooltip GIF. Folder: ").append(dirLink));
                } else {
                    chat(client, Text.literal("[Simply Tooltips] Failed to save tooltip GIF: " + finalError.getMessage()));
                }
            });
        });
    }

    private static void writeGif(List<BufferedImage> images, Path output) throws IOException {
        if (images.isEmpty()) {
            throw new IOException("No frames available for GIF output");
        }

        ImageWriter gifWriter = ImageIO.getImageWritersBySuffix("gif").next();
        ImageWriteParam param = gifWriter.getDefaultWriteParam();
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        IIOMetadata metadata = gifWriter.getDefaultImageMetadata(type, param);
        configureGifMetadata(metadata, FRAME_DELAY_CS);

        try (ImageOutputStream out = ImageIO.createImageOutputStream(Files.newOutputStream(output))) {
            gifWriter.setOutput(out);
            gifWriter.prepareWriteSequence(null);
            for (BufferedImage image : images) {
                gifWriter.writeToSequence(new IIOImage(image, null, metadata), param);
            }
            gifWriter.endWriteSequence();
        } finally {
            gifWriter.dispose();
        }
    }

    private static void configureGifMetadata(IIOMetadata metadata, int delayCs) throws IOException {
        String nativeFormat = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);

        IIOMetadataNode graphicControl = getOrCreateNode(root, "GraphicControlExtension");
        graphicControl.setAttribute("disposalMethod", "none");
        graphicControl.setAttribute("userInputFlag", "FALSE");
        graphicControl.setAttribute("transparentColorFlag", "FALSE");
        graphicControl.setAttribute("delayTime", Integer.toString(delayCs));
        graphicControl.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensions = getOrCreateNode(root, "ApplicationExtensions");
        IIOMetadataNode appNode = new IIOMetadataNode("ApplicationExtension");
        appNode.setAttribute("applicationID", "NETSCAPE");
        appNode.setAttribute("authenticationCode", "2.0");
        appNode.setUserObject(new byte[]{0x1, 0x0, 0x0});
        appExtensions.appendChild(appNode);

        metadata.setFromTree(nativeFormat, root);
    }

    private static IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (name.equals(root.item(i).getNodeName())) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    private static void chat(MinecraftClient client, Text message) {
        if (client.player != null) {
            client.player.sendMessage(message, false);
        }
    }

    private TooltipGifRecorder() {}
}
