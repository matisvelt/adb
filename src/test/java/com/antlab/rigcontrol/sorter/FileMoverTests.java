package com.antlab.rigcontrol.sorter;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class FileMoverTests {
    @Test
    void handlesConflictsAndUndo() throws Exception {
        Path tempDir = Files.createTempDirectory("rigsort-move");
        Path source = tempDir.resolve("photo.jpg");
        Files.writeString(source, "data");

        Path destRoot = tempDir.resolve("dest");
        Files.createDirectories(destRoot.resolve("Photos"));
        Files.writeString(destRoot.resolve("Photos").resolve("photo.jpg"), "existing");

        FileRecord record = new FileRecord();
        record.setFileId("abc123");
        record.setSourcePath(source.toString());
        record.setModifiedTime(System.currentTimeMillis());

        ProjectConfig config = new ProjectConfig();
        config.setDestinationRoot(destRoot.toString());
        config.setAppendDateFolders(false);

        FileMover mover = new FileMover(Logger.getLogger("test"));
        FileMover.MoveResult result = mover.move(record, config, "Photos");

        assertTrue(result.isOk());
        assertTrue(Files.exists(Path.of(result.getDestination())));
        assertNotEquals(destRoot.resolve("Photos").resolve("photo.jpg").toString(), result.getDestination());

        AuditRecord audit = new AuditRecord();
        audit.setFromPath(source.toString());
        audit.setToPath(result.getDestination());
        mover.undo(List.of(audit));

        assertTrue(Files.exists(Path.of(source.toString())));
    }
}
