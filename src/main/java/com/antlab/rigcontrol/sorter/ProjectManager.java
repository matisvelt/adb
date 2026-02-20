package com.antlab.rigcontrol.sorter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectManager {
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private Path projectDir;
    private Path configPath;
    private Path manifestPath;
    private Path auditPath;
    private Path previewDir;

    private final Map<String, FileRecord> records = new HashMap<>();
    private final List<AuditRecord> auditRecords = new ArrayList<>();
    private ProjectConfig config;

    public void createProject(Path dir, ProjectConfig config) throws IOException {
        this.projectDir = dir;
        this.config = config;
        initPaths();
        Files.createDirectories(projectDir);
        Files.createDirectories(previewDir);
        saveConfig();
        if (!Files.exists(manifestPath)) {
            Files.createFile(manifestPath);
        }
        if (!Files.exists(auditPath)) {
            Files.createFile(auditPath);
        }
    }

    public void loadProject(Path dir) throws IOException {
        this.projectDir = dir;
        initPaths();
        if (!Files.exists(configPath)) {
            throw new IOException("Missing project.json in " + dir);
        }
        this.config = mapper.readValue(configPath.toFile(), ProjectConfig.class);
        if (this.config.getRules() == null || this.config.getRules().isEmpty()) {
            this.config.setRules(defaultRules());
        }
        Files.createDirectories(previewDir);
        records.clear();
        auditRecords.clear();
        loadManifest();
        loadAudit();
    }

    private void initPaths() {
        configPath = projectDir.resolve("project.json");
        manifestPath = projectDir.resolve("manifest.jsonl");
        auditPath = projectDir.resolve("audit.jsonl");
        previewDir = projectDir.resolve(".rigsort").resolve("cache").resolve("previews");
    }

    public Path getProjectDir() {
        return projectDir;
    }

    public ProjectConfig getConfig() {
        return config;
    }

    public Path getPreviewDir() {
        return previewDir;
    }

    public synchronized void saveConfig() throws IOException {
        if (config == null || configPath == null) {
            return;
        }
        mapper.writeValue(configPath.toFile(), config);
    }

    public synchronized void upsertRecord(FileRecord record) throws IOException {
        record.setLastUpdated(System.currentTimeMillis());
        records.put(record.getFileId(), record);
        appendJsonLine(manifestPath, record);
    }

    public synchronized void appendAudit(AuditRecord record) throws IOException {
        auditRecords.add(record);
        appendJsonLine(auditPath, record);
    }

    public synchronized List<FileRecord> getRecords() {
        return new ArrayList<>(records.values());
    }

    public synchronized FileRecord getRecord(String fileId) {
        return records.get(fileId);
    }

    public synchronized List<AuditRecord> getAuditRecords() {
        return new ArrayList<>(auditRecords);
    }

    private void loadManifest() throws IOException {
        if (!Files.exists(manifestPath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(manifestPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                FileRecord record = mapper.readValue(line, FileRecord.class);
                records.put(record.getFileId(), record);
            }
        }
    }

    private void loadAudit() throws IOException {
        if (!Files.exists(auditPath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(auditPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                AuditRecord record = mapper.readValue(line, AuditRecord.class);
                auditRecords.add(record);
            }
        }
    }

    private void appendJsonLine(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(mapper.writeValueAsString(value));
            writer.newLine();
        }
    }

    public static List<Rule> defaultRules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("rule-1", true, "label == DOCUMENT_INVOICE", "Work/Invoices", "Invoices"));
        rules.add(new Rule("rule-2", true, "label == DOCUMENT_OTHER || hasTextLikelihood > 0.7", "Work/Documents", "Documents"));
        rules.add(new Rule("rule-3", true, "label == SCREENSHOT", "Work/Screenshots", "Screenshots"));
        rules.add(new Rule("rule-4", true, "label startsWith PHOTO && facesCount >= 1", "Private/Family/People", "People photos"));
        rules.add(new Rule("rule-5", true, "label startsWith PHOTO && facesCount == 0", "Private/Photos/Other", "Other photos"));
        return rules;
    }
}
