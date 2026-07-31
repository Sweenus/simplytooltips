package net.sweenus.simplytooltips.client.render;

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
import java.nio.file.StandardCopyOption;
import java.util.List;

final class AnimatedGifWriter {
    static void writeAtomically(List<BufferedImage> images, Path output, int delayCentiseconds) throws IOException {
        if (images.isEmpty()) throw new IOException("No frames available for GIF output");
        Files.createDirectories(output.getParent());
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
        try {
            write(images, temporary, delayCentiseconds);
            try {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void write(List<BufferedImage> images, Path output, int delayCentiseconds) throws IOException {
        if (images.isEmpty()) throw new IOException("No frames available for GIF output");
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        IIOMetadata metadata = writer.getDefaultImageMetadata(type, param);
        configureMetadata(metadata, Math.max(1, delayCentiseconds));

        try (ImageOutputStream out = ImageIO.createImageOutputStream(Files.newOutputStream(output))) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (BufferedImage image : images) {
                writer.writeToSequence(new IIOImage(image, null, metadata), param);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static void configureMetadata(IIOMetadata metadata, int delayCs) throws IOException {
        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        IIOMetadataNode control = getOrCreate(root, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        control.setAttribute("delayTime", Integer.toString(delayCs));
        control.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode extensions = getOrCreate(root, "ApplicationExtensions");
        IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
        loop.setAttribute("applicationID", "NETSCAPE");
        loop.setAttribute("authenticationCode", "2.0");
        loop.setUserObject(new byte[]{0x1, 0x0, 0x0});
        extensions.appendChild(loop);
        metadata.setFromTree(format, root);
    }

    private static IIOMetadataNode getOrCreate(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (name.equals(root.item(i).getNodeName())) return (IIOMetadataNode) root.item(i);
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    private AnimatedGifWriter() {}
}
