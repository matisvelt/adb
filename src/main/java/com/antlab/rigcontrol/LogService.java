package com.antlab.rigcontrol;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public class LogService {
    private static final int MAX_LINES = 200;

    private final ObservableList<String> logLines = FXCollections.observableArrayList();
    private final Logger logger;
    private final Handler listHandler;
    private final Handler fileHandler;

    public LogService(Path logDir) {
        logger = Logger.getLogger("RigControl");
        logger.setUseParentHandlers(false);

        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory", e);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path logFile = logDir.resolve("rigcontrol-" + timestamp + ".log");

        listHandler = new ListHandler();
        listHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(listHandler);

        try {
            fileHandler = new FileHandler(logFile.toString());
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log file", e);
        }

        logger.info("Logging started: " + logFile);
    }

    public Logger getLogger() {
        return logger;
    }

    public ObservableList<String> getLogLines() {
        return logLines;
    }

    public void shutdown() {
        listHandler.close();
        fileHandler.close();
    }

    private class ListHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            String msg = getFormatter() == null ? record.getMessage() : getFormatter().format(record);
            Platform.runLater(() -> {
                logLines.add(msg.trim());
                if (logLines.size() > MAX_LINES) {
                    logLines.remove(0, logLines.size() - MAX_LINES);
                }
            });
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
