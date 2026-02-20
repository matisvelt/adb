package com.antlab.rigcontrol.sorter;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class PreviewGeneratorTests {
    @Test
    void scalesToMaxEdge() throws Exception {
        Path tempDir = Files.createTempDirectory("rigsort-preview");
        Path source = tempDir.resolve("source.png");

        BufferedImage img = new BufferedImage(4000, 2000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 4000, 2000);
        g.dispose();
        ImageIO.write(img, "png", source.toFile());

        FileRecord record = new FileRecord();
        record.setFileId("test");
        record.setSourcePath(source.toString());
        record.setExtension("png");
        record.setFileType(FileType.IMAGE);

        PreviewPolicy policy = new PreviewPolicy(1000, "JPEG", 85, false);
        PreviewGenerator generator = new PreviewGenerator(Logger.getLogger("test"));
        PreviewGenerator.PreviewResult result = generator.generate(record, tempDir, policy);

        assertTrue(result.isOk());
        BufferedImage out = ImageIO.read(Path.of(result.getPreviewPath()).toFile());
        assertNotNull(out);
        assertTrue(Math.max(out.getWidth(), out.getHeight()) <= 1000);
    }
}
