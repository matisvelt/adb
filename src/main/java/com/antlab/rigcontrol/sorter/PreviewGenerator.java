package com.antlab.rigcontrol.sorter;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

public class PreviewGenerator {
    private final Logger logger;

    public PreviewGenerator(Logger logger) {
        this.logger = logger;
    }

    public PreviewResult generate(FileRecord record, Path previewDir, PreviewPolicy policy) {
        try {
            Files.createDirectories(previewDir);
            String format = chooseFormat(policy.getFormat());
            String ext = format.equals("WEBP") ? "webp" : "jpg";
            Path output = previewDir.resolve(record.getFileId() + "." + ext);

            BufferedImage source = loadSource(record);
            if (source == null) {
                return PreviewResult.error("Unsupported source");
            }
            BufferedImage scaled = scaleToMax(source, policy.getMaxLongEdgePx());
            if (!writeImage(scaled, output, format, policy.getQuality())) {
                return PreviewResult.error("Failed to write preview");
            }
            return PreviewResult.success(output.toString());
        } catch (Exception e) {
            logger.warning("Preview generation failed: " + e.getMessage());
            return PreviewResult.error(e.getMessage());
        }
    }

    private BufferedImage loadSource(FileRecord record) throws IOException {
        Path source = Path.of(record.getSourcePath());
        if (record.getFileType() == FileType.DOCUMENT && "pdf".equalsIgnoreCase(record.getExtension())) {
            return renderPdf(source);
        }
        if (record.getFileType() == FileType.TEXT) {
            return renderText(source);
        }
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            return null;
        }
        int orientation = readOrientation(source);
        return applyOrientation(image, orientation);
    }

    private BufferedImage renderPdf(Path pdfPath) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            return renderer.renderImageWithDPI(0, 150);
        }
    }

    private BufferedImage renderText(Path path) throws IOException {
        java.util.List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (Exception ex) {
            lines = java.util.List.of("Preview unavailable");
        }
        int width = 1200;
        int height = Math.max(400, Math.min(2000, lines.size() * 22 + 80));
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        int y = 40;
        for (String line : lines) {
            g.drawString(truncate(line, 120), 40, y);
            y += 22;
            if (y > height - 40) {
                break;
            }
        }
        g.dispose();
        return img;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    private int readOrientation(Path path) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private BufferedImage applyOrientation(BufferedImage image, int orientation) {
        if (orientation == 1) {
            return image;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage rotated;
        Graphics2D g;
        int type = image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType();
        switch (orientation) {
            case 6: // 90 CW
                rotated = new BufferedImage(height, width, type);
                g = rotated.createGraphics();
                g.translate(height, 0);
                g.rotate(Math.toRadians(90));
                g.drawImage(image, 0, 0, null);
                g.dispose();
                return rotated;
            case 3: // 180
                rotated = new BufferedImage(width, height, type);
                g = rotated.createGraphics();
                g.translate(width, height);
                g.rotate(Math.toRadians(180));
                g.drawImage(image, 0, 0, null);
                g.dispose();
                return rotated;
            case 8: // 270 CW
                rotated = new BufferedImage(height, width, type);
                g = rotated.createGraphics();
                g.translate(0, width);
                g.rotate(Math.toRadians(270));
                g.drawImage(image, 0, 0, null);
                g.dispose();
                return rotated;
            default:
                return image;
        }
    }

    private BufferedImage scaleToMax(BufferedImage src, int maxLongEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= maxLongEdge) {
            return src;
        }
        double scale = (double) maxLongEdge / longEdge;
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return resized;
    }

    private boolean writeImage(BufferedImage image, Path output, String format, int quality) throws IOException {
        String targetFormat = format.equals("WEBP") ? "webp" : "jpg";
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(targetFormat);
        if (!writers.hasNext()) {
            if (!format.equals("JPEG")) {
                return writeImage(image, output, "JPEG", quality);
            }
            return false;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.min(1f, Math.max(0.5f, quality / 100f)));
        }
        try (ImageOutputStream out = ImageIO.createImageOutputStream(output.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return true;
    }

    private String chooseFormat(String preferred) {
        if (preferred == null) {
            return "JPEG";
        }
        String f = preferred.toUpperCase(Locale.ROOT);
        if (f.equals("WEBP")) {
            return "WEBP";
        }
        return "JPEG";
    }

    public static class PreviewResult {
        private final boolean ok;
        private final String previewPath;
        private final String error;

        private PreviewResult(boolean ok, String previewPath, String error) {
            this.ok = ok;
            this.previewPath = previewPath;
            this.error = error;
        }

        public static PreviewResult success(String path) {
            return new PreviewResult(true, path, null);
        }

        public static PreviewResult error(String error) {
            return new PreviewResult(false, null, error);
        }

        public boolean isOk() {
            return ok;
        }

        public String getPreviewPath() {
            return previewPath;
        }

        public String getError() {
            return error;
        }
    }
}
