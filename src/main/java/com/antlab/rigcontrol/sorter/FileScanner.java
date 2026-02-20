package com.antlab.rigcontrol.sorter;

import com.antlab.rigcontrol.util.HashUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class FileScanner {
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "heic", "webp");
    private static final Set<String> DOC_EXT = Set.of("pdf");
    private static final Set<String> TEXT_EXT = Set.of("txt", "docx");

    public void scan(Path root, boolean strictHash, Consumer<FileRecord> consumer) throws IOException {
        if (root == null || !Files.exists(root)) {
            throw new IOException("Source root missing");
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }
                String ext = extension(file.getFileName().toString());
                if (!isSupported(ext)) {
                    return FileVisitResult.CONTINUE;
                }
                FileRecord record = new FileRecord();
                record.setSourcePath(file.toAbsolutePath().toString());
                record.setSizeBytes(attrs.size());
                record.setModifiedTime(attrs.lastModifiedTime().toMillis());
                record.setExtension(ext);
                record.setFileType(classify(ext));
                record.setStatus(FileStatus.NEW);
                if (record.getFileType() == FileType.IMAGE) {
                    populateExif(record, file);
                }
                try {
                    String id = strictHash ? HashUtil.sha256File(file) : HashUtil.sha256String(record.getSourcePath() + "|" + record.getSizeBytes() + "|" + record.getModifiedTime());
                    record.setFileId(id);
                    consumer.accept(record);
                } catch (IOException ex) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public boolean isSupported(String ext) {
        if (ext == null) {
            return false;
        }
        String e = ext.toLowerCase(Locale.ROOT);
        return IMAGE_EXT.contains(e) || DOC_EXT.contains(e) || TEXT_EXT.contains(e);
    }

    private FileType classify(String ext) {
        if (ext == null) {
            return FileType.UNKNOWN;
        }
        String e = ext.toLowerCase(Locale.ROOT);
        if (IMAGE_EXT.contains(e)) {
            return FileType.IMAGE;
        }
        if (DOC_EXT.contains(e)) {
            return FileType.DOCUMENT;
        }
        if (TEXT_EXT.contains(e)) {
            return FileType.TEXT;
        }
        return FileType.UNKNOWN;
    }

    private String extension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private void populateExif(FileRecord record, Path file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {
                String date = sub.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                record.setExifDateTime(date);
            }
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                String model = ifd0.getString(ExifIFD0Directory.TAG_MODEL);
                record.setExifModel(model);
            }
        } catch (Exception ignored) {
        }
    }
}
