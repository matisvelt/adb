package com.antlab.rigcontrol.sorter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public class FileMover {
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM").withZone(ZoneId.systemDefault());

    private final Logger logger;

    public FileMover(Logger logger) {
        this.logger = logger;
    }

    public MoveResult move(FileRecord record, ProjectConfig config, String destinationRelative) {
        if (record == null) {
            return MoveResult.error("Missing record");
        }
        if (config.getDestinationRoot() == null || config.getDestinationRoot().isBlank()) {
            return MoveResult.error("Destination root not set");
        }
        try {
            Path source = Path.of(record.getSourcePath());
            if (!Files.exists(source)) {
                return MoveResult.error("Source missing");
            }
            Path destRoot = Path.of(config.getDestinationRoot());
            String resolved = resolveTemplate(destinationRelative, record, config);
            Path destDir = destRoot.resolve(resolved);
            Files.createDirectories(destDir);
            Path dest = resolveCollision(destDir.resolve(source.getFileName()), record);
            try {
                Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                if (Files.size(source) == Files.size(dest)) {
                    Files.delete(source);
                } else {
                    return MoveResult.error("Copy verify failed");
                }
            }
            return MoveResult.success(dest.toString());
        } catch (Exception ex) {
            logger.warning("Move failed: " + ex.getMessage());
            return MoveResult.error(ex.getMessage());
        }
    }

    public void undo(List<AuditRecord> records) {
        if (records == null) {
            return;
        }
        for (AuditRecord record : records) {
            try {
                Path from = Path.of(record.getToPath());
                Path to = Path.of(record.getFromPath());
                if (!Files.exists(from)) {
                    continue;
                }
                Files.createDirectories(to.getParent());
                Path resolved = resolveCollision(to, null);
                Files.move(from, resolved, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                logger.warning("Undo failed: " + ex.getMessage());
            }
        }
    }

    private String resolveTemplate(String template, FileRecord record, ProjectConfig config) {
        String out = template == null ? "" : template;
        if (record != null) {
            String label = record.getLabel() == null ? "UNKNOWN" : record.getLabel();
            out = out.replace("{label}", label);
        }
        if (config.isAppendDateFolders()) {
            String year = YEAR_FMT.format(Instant.ofEpochMilli(record.getModifiedTime()));
            String month = MONTH_FMT.format(Instant.ofEpochMilli(record.getModifiedTime()));
            if (!out.contains("{yyyy}")) {
                out = out + "/" + year;
            }
            if (!out.contains("{mm}")) {
                out = out + "/" + month;
            }
        }
        out = out.replace("{yyyy}", YEAR_FMT.format(Instant.ofEpochMilli(record.getModifiedTime())));
        out = out.replace("{mm}", MONTH_FMT.format(Instant.ofEpochMilli(record.getModifiedTime())));
        return out;
    }

    private Path resolveCollision(Path target, FileRecord record) throws IOException {
        if (!Files.exists(target)) {
            return target;
        }
        String fileId = record == null ? "" : record.shortId();
        String name = target.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int counter = 1;
        Path candidate;
        do {
            String suffix = fileId.isBlank() ? ("_" + counter) : ("_" + fileId + "_" + counter);
            candidate = target.getParent().resolve(base + suffix + ext);
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }

    public static class MoveResult {
        private final boolean ok;
        private final String destination;
        private final String error;

        private MoveResult(boolean ok, String destination, String error) {
            this.ok = ok;
            this.destination = destination;
            this.error = error;
        }

        public static MoveResult success(String destination) {
            return new MoveResult(true, destination, null);
        }

        public static MoveResult error(String error) {
            return new MoveResult(false, null, error);
        }

        public boolean isOk() {
            return ok;
        }

        public String getDestination() {
            return destination;
        }

        public String getError() {
            return error;
        }
    }
}
