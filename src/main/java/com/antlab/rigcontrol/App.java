package com.antlab.rigcontrol;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.cell.TextFieldTableCell;

public class App extends Application {
    private DeviceManager deviceManager;
    private LogService logService;

    @Override
    public void start(Stage stage) {
        Settings settings = new Settings();
        logService = new LogService(Path.of(System.getProperty("user.home"), ".rigcontrol", "logs"));
        ADBService adbService = new ADBService(settings, logService.getLogger());
        deviceManager = new DeviceManager(settings, adbService, logService.getLogger());
        logService.getLogger().info("App starting");

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logService.getLogger().severe("Uncaught exception on " + thread.getName() + ": " + throwable));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.getStyleClass().add("root");

        HBox topBar = new HBox(10);
        topBar.getStyleClass().add("topbar");
        Button rescanButton = new Button("Rescan");
        Button pingAllButton = new Button("Ping all");
        Button demoButton = new Button("Demo Data");
        Button settingsButton = new Button("Settings");
        Button legendButton = new Button("Legend");
        Button helpButton = new Button("Help");
        Button importExpectedButton = new Button("Import Expected");
        Button exportSnapshotButton = new Button("Export Snapshot");
        Button restartAdbButton = new Button("Restart ADB");
        Button copyButton = new Button("Copy CSV");
        CheckBox hideDisconnected = new CheckBox("Hide disconnected");
        CheckBox pausePolling = new CheckBox("Pause");
        rescanButton.getStyleClass().add("secondary-button");
        pingAllButton.getStyleClass().add("primary-button");
        demoButton.getStyleClass().add("secondary-button");
        settingsButton.getStyleClass().add("secondary-button");
        legendButton.getStyleClass().add("secondary-button");
        helpButton.getStyleClass().add("secondary-button");
        importExpectedButton.getStyleClass().add("secondary-button");
        exportSnapshotButton.getStyleClass().add("secondary-button");
        restartAdbButton.getStyleClass().add("secondary-button");
        copyButton.getStyleClass().add("secondary-button");
        Label deviceCountLabel = new Label();
        deviceCountLabel.getStyleClass().add("chip-primary");
        Label adbHealthLabel = new Label();
        adbHealthLabel.getStyleClass().add("chip-ok");
        Label adbPathLabel = new Label();
        adbPathLabel.getStyleClass().add("chip-neutral");
        Label scanDurationLabel = new Label();
        scanDurationLabel.getStyleClass().add("chip-neutral");

        rescanButton.setOnAction(e -> deviceManager.rescanNow());
        pingAllButton.setOnAction(e -> deviceManager.pingAll());
        demoButton.setOnAction(e -> enableDemoData(settings));
        settingsButton.setOnAction(e -> {
            SettingsDialog.show(stage, settings, deviceManager);
            updateAdbPathLabel(adbPathLabel, settings);
        });
        legendButton.setOnAction(e -> showLegend(stage));
        helpButton.setOnAction(e -> showReconnectChecklist(stage));
        importExpectedButton.setOnAction(e -> importExpected(stage));
        exportSnapshotButton.setOnAction(e -> exportSnapshot(stage, filtered));
        restartAdbButton.setOnAction(e -> deviceManager.restartAdb());
        pausePolling.selectedProperty().addListener((obs, oldVal, newVal) -> deviceManager.setPaused(newVal));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(
                rescanButton, pingAllButton, demoButton, copyButton, settingsButton, legendButton, helpButton,
                importExpectedButton, exportSnapshotButton, restartAdbButton,
                topSpacer, deviceCountLabel, adbHealthLabel, adbPathLabel, scanDurationLabel, hideDisconnected, pausePolling
        );
        root.setTop(topBar);

        FilteredList<DeviceInfo> filtered = new FilteredList<>(deviceManager.getDevices(), d -> true);
        hideDisconnected.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(d -> !newVal || !"disconnected".equalsIgnoreCase(d.getAdbState()));
        });

        TableView<DeviceInfo> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setEditable(true);

        TableColumn<DeviceInfo, String> serialCol = new TableColumn<>("Serial");
        serialCol.setCellValueFactory(data -> data.getValue().serialProperty());

        TableColumn<DeviceInfo, String> stateCol = new TableColumn<>("ADB State");
        stateCol.setCellValueFactory(data -> data.getValue().adbStateProperty());

        TableColumn<DeviceInfo, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(data -> data.getValue().modelProperty());

        TableColumn<DeviceInfo, String> versionCol = new TableColumn<>("Android");
        versionCol.setCellValueFactory(data -> data.getValue().androidVersionProperty());

        TableColumn<DeviceInfo, String> lastSeenCol = new TableColumn<>("Last Seen");
        lastSeenCol.setCellValueFactory(data -> data.getValue().lastSeenProperty());

        TableColumn<DeviceInfo, String> pingCol = new TableColumn<>("Ping");
        pingCol.setCellValueFactory(data -> data.getValue().pingStatusProperty());

        TableColumn<DeviceInfo, String> lastPingCol = new TableColumn<>("Last Ping");
        lastPingCol.setCellValueFactory(data -> data.getValue().lastPingProperty());

        TableColumn<DeviceInfo, String> expectedCol = new TableColumn<>("Expected");
        expectedCol.setCellValueFactory(data -> data.getValue().expectedProperty());

        TableColumn<DeviceInfo, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(data -> data.getValue().tagProperty());
        tagCol.setCellFactory(TextFieldTableCell.forTableColumn());
        tagCol.setOnEditCommit(e -> e.getRowValue().setTag(e.getNewValue()));

        table.getColumns().addAll(serialCol, stateCol, modelCol, versionCol, lastSeenCol, pingCol, lastPingCol, expectedCol, tagCol);
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card");
        TabPane centerTabs = new TabPane();
        Tab monitorTab = new Tab("Monitor", tableCard);
        monitorTab.setClosable(false);
        Tab mcTab = new Tab("Monte Carlo", buildMonteCarloPanel());
        mcTab.setClosable(false);
        Tab aiTab = new Tab("Neural", buildNeuralPanel());
        aiTab.setClosable(false);
        centerTabs.getTabs().addAll(monitorTab, mcTab, aiTab);
        root.setCenter(centerTabs);

        ListView<String> logView = new ListView<>(logService.getLogLines());
        logView.setPrefHeight(160);
        logView.getStyleClass().add("log-list");

        Label statusLine = new Label();
        statusLine.getStyleClass().add("status-bar");

        Label lastErrorLabel = new Label("Last error: -");
        lastErrorLabel.getStyleClass().add("status-bar");

        Label telemetryLabel = new Label();
        telemetryLabel.getStyleClass().add("status-bar");
        Label expectedMissingLabel = new Label("Expected missing: 0");
        expectedMissingLabel.getStyleClass().add("status-bar");

        HBox telemetryRow = new HBox(12, telemetryLabel, expectedMissingLabel, lastErrorLabel);
        telemetryRow.setAlignment(Pos.CENTER_LEFT);

        VBox bottomCard = new VBox(10, new Label("Logs"), logView, telemetryRow, statusLine);
        bottomCard.getStyleClass().add("card");
        bottomCard.setPadding(new Insets(12));
        VBox bottom = new VBox(10, bottomCard);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottom);

        deviceManager.getDevices().addListener((ListChangeListener<DeviceInfo>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (DeviceInfo info : change.getAddedSubList()) {
                        info.adbStateProperty().addListener((obs, oldVal, newVal) -> updateStatus(deviceCountLabel, statusLine));
                        info.adbStateProperty().addListener((obs, oldVal, newVal) -> filtered.setPredicate(d -> !hideDisconnected.isSelected() || !"disconnected".equalsIgnoreCase(d.getAdbState())));
                    }
                }
            }
            updateStatus(deviceCountLabel, statusLine);
        });
        updateStatus(deviceCountLabel, statusLine);

        deviceManager.adbHealthProperty().addListener((obs, oldVal, newVal) -> updateHealthLabel(adbHealthLabel, newVal));
        deviceManager.lastScanDurationProperty().addListener((obs, oldVal, newVal) -> scanDurationLabel.setText("Scan: " + newVal));
        deviceManager.lastErrorProperty().addListener((obs, oldVal, newVal) -> lastErrorLabel.setText("Last error: " + (newVal == null || newVal.isBlank() ? "-" : newVal)));
        deviceManager.expectedMissingProperty().addListener((obs, oldVal, newVal) -> expectedMissingLabel.setText("Expected missing: " + newVal));
        updateHealthLabel(adbHealthLabel, deviceManager.adbHealthProperty().get());
        scanDurationLabel.setText("Scan: " + deviceManager.lastScanDurationProperty().get());
        updateAdbPathLabel(adbPathLabel, settings);

        copyButton.setOnAction(e -> copyCsv(filtered));

        Timeline telemetryTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            telemetryLabel.setText(buildTelemetryText(deviceManager));
        }));
        telemetryTimer.setCycleCount(Timeline.INDEFINITE);
        telemetryTimer.play();

        deviceManager.start();

        Scene scene = new Scene(root, settings.getWindowWidth(), settings.getWindowHeight());
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setTitle("RigControl v0.1");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> logService.getLogger().info("Stage close requested"));
        stage.setOnHidden(event -> logService.getLogger().info("Stage hidden"));
        stage.widthProperty().addListener((obs, oldVal, newVal) -> settings.setWindowWidth(newVal.doubleValue()));
        stage.heightProperty().addListener((obs, oldVal, newVal) -> settings.setWindowHeight(newVal.doubleValue()));
        stage.show();
    }

    private void updateStatus(Label deviceCountLabel, Label statusLine) {
        int total = deviceManager.getDevices().size();
        long online = deviceManager.getDevices().stream().filter(d -> "device".equalsIgnoreCase(d.getAdbState())).count();
        long offline = deviceManager.getDevices().stream().filter(d -> "offline".equalsIgnoreCase(d.getAdbState())).count();
        long unauth = deviceManager.getDevices().stream().filter(d -> "unauthorized".equalsIgnoreCase(d.getAdbState())).count();
        long disconnected = deviceManager.getDevices().stream().filter(d -> "disconnected".equalsIgnoreCase(d.getAdbState())).count();

        deviceCountLabel.setText("Devices: " + total + " (online " + online + ")");
        statusLine.setText("Online: " + online + " | Offline: " + offline + " | Unauthorized: " + unauth + " | Disconnected: " + disconnected);
    }

    private void updateHealthLabel(Label label, String health) {
        label.getStyleClass().removeAll("chip-ok", "chip-warn", "chip-neutral");
        if (health == null) {
            label.setText("ADB ?");
            label.getStyleClass().add("chip-neutral");
            return;
        }
        label.setText(health);
        if ("ADB OK".equalsIgnoreCase(health)) {
            label.getStyleClass().add("chip-ok");
        } else if ("SIMULATION".equalsIgnoreCase(health)) {
            label.getStyleClass().add("chip-neutral");
        } else {
            label.getStyleClass().add("chip-warn");
        }
    }

    private void updateAdbPathLabel(Label label, Settings settings) {
        String path = settings.getAdbPath();
        String text;
        String style;
        if (path == null || path.isBlank() || "adb".equalsIgnoreCase(path.trim())) {
            boolean adbOnPath = isAdbOnPath();
            text = adbOnPath ? "ADB: PATH OK" : "ADB: PATH MISSING";
            style = adbOnPath ? "chip-ok" : "chip-warn";
        } else if (Files.isExecutable(Path.of(path))) {
            text = "ADB: OK";
            style = "chip-ok";
        } else {
            text = "ADB: BAD PATH";
            style = "chip-warn";
        }
        label.setText(text);
        label.getStyleClass().removeAll("chip-ok", "chip-warn", "chip-neutral");
        label.getStyleClass().add(style);
    }

    private boolean isAdbOnPath() {
        try {
            Process process = new ProcessBuilder("adb", "version").start();
            boolean finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void enableDemoData(Settings settings) {
        try {
            Path demoPath = ensureDemoFile();
            settings.setSimulationEnabled(true);
            settings.setSimulationFile(demoPath.toString());
            deviceManager.applySettings();
            deviceManager.rescanNow();
        } catch (Exception e) {
            logService.getLogger().warning("Failed to enable demo data: " + e.getMessage());
        }
    }

    private Path ensureDemoFile() throws IOException {
        Path demoDir = Path.of(System.getProperty("user.home"), ".rigcontrol", "demo");
        Files.createDirectories(demoDir);
        Path demoFile = demoDir.resolve("adb_devices_sample.txt");
        try (InputStream in = getClass().getResourceAsStream("/samples/adb_devices_sample.txt")) {
            if (in == null) {
                throw new IOException("Missing demo resource");
            }
            Files.copy(in, demoFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return demoFile;
    }

    private void showLegend(Stage owner) {
        String text = String.join("\n",
                "device: ADB online and responsive",
                "offline: ADB sees the device but it is not responsive",
                "unauthorized: ADB key not approved on device",
                "disconnected: previously seen, not present in latest scan"
        );
        showInfoDialog(owner, "Device State Legend", text);
    }

    private void showReconnectChecklist(Stage owner) {
        String text = String.join("\n",
                "1. Verify `adb devices -l` shows expected devices.",
                "2. For unauthorized: approve USB debugging on the device.",
                "3. If ADB shows nothing: check ADB path in Settings.",
                "4. Restart ADB: `adb kill-server` then `adb start-server`.",
                "5. Reseat USB hub or power cycle the rig if needed."
        );
        showInfoDialog(owner, "Reconnect Checklist", text);
    }

    private void showInfoDialog(Stage owner, String title, String body) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(body);
        alert.showAndWait();
    }

    private String buildTelemetryText(DeviceManager manager) {
        long polls = manager.getTotalPolls();
        long ok = manager.getPingOk();
        long fail = manager.getPingFail();
        long total = ok + fail;
        String rate = total == 0 ? "-" : String.format(Locale.US, "%.1f%%", (ok * 100.0) / total);
        String batch = manager.pingBatchDurationProperty().get();
        String count = manager.pingBatchCountProperty().get();
        return "Polls: " + polls + " | Ping OK: " + rate + " | Ping batch: " + batch + " (" + count + " devices)";
    }

    private void copyCsv(Iterable<DeviceInfo> deviceInfos) {
        String header = "serial,adb_state,model,android,last_seen,ping_status,last_ping";
        String body = "";
        try {
            body = java.util.stream.StreamSupport.stream(deviceInfos.spliterator(), false)
                    .map(d -> String.join(",",
                            d.getSerial(),
                            d.getAdbState(),
                            safeCsv(d.modelProperty().get()),
                            safeCsv(d.androidVersionProperty().get()),
                            safeCsv(d.lastSeenProperty().get()),
                            safeCsv(d.pingStatusProperty().get()),
                            safeCsv(d.lastPingProperty().get())
                    ))
                    .collect(Collectors.joining("\n"));
        } catch (Exception ignored) {
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(header + "\n" + body);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private void importExpected(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Expected Serials (CSV or TXT)");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("CSV/TXT", "*.csv", "*.txt");
        chooser.getExtensionFilters().add(filter);
        java.io.File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Set<String> serials = Files.readAllLines(file.toPath()).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.toLowerCase(Locale.ROOT).startsWith("serial"))
                    .collect(Collectors.toSet());
            deviceManager.setExpectedSerials(serials);
        } catch (IOException e) {
            logService.getLogger().warning("Failed to import expected list: " + e.getMessage());
        }
    }

    private void exportSnapshot(Stage owner, Iterable<DeviceInfo> deviceInfos) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Snapshot CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName("rig_snapshot.csv");
        java.io.File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        String header = "serial,adb_state,model,android,last_seen,ping_status,last_ping,expected,tag";
        String body = java.util.stream.StreamSupport.stream(deviceInfos.spliterator(), false)
                .map(d -> String.join(",",
                        d.getSerial(),
                        d.getAdbState(),
                        safeCsv(d.modelProperty().get()),
                        safeCsv(d.androidVersionProperty().get()),
                        safeCsv(d.lastSeenProperty().get()),
                        safeCsv(d.pingStatusProperty().get()),
                        safeCsv(d.lastPingProperty().get()),
                        safeCsv(d.expectedProperty().get()),
                        safeCsv(d.tagProperty().get())
                ))
                .collect(Collectors.joining("\n"));
        try {
            Files.writeString(file.toPath(), header + "\n" + body);
        } catch (IOException e) {
            logService.getLogger().warning("Failed to export snapshot: " + e.getMessage());
        }
    }

    private VBox buildMonteCarloPanel() {
        VBox root = new VBox(12);
        root.getStyleClass().add("card");
        Label title = new Label("Monte Carlo Batch");
        title.getStyleClass().add("panel-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField distribution = new TextField("Normal");
        TextField params = new TextField("mu=0, sigma=1");
        TextField samples = new TextField("100000");
        TextField seed = new TextField("auto");
        TextField output = new TextField("/tmp/rig-mc-output.json");

        grid.addRow(0, new Label("Distribution"), distribution);
        grid.addRow(1, new Label("Parameters"), params);
        grid.addRow(2, new Label("Samples"), samples);
        grid.addRow(3, new Label("Seed Strategy"), seed);
        grid.addRow(4, new Label("Output Path"), output);

        HBox buttons = new HBox(10,
                new Button("Run Batch"),
                new Button("Pause"),
                new Button("Resume"),
                new Button("Stop")
        );
        buttons.getChildren().forEach(n -> n.getStyleClass().add("secondary-button"));
        buttons.getChildren().get(0).getStyleClass().add("primary-button");

        Label note = new Label("UI scaffold only (not wired to execution yet).");
        note.getStyleClass().add("status-bar");

        root.getChildren().addAll(title, grid, buttons, note);
        return root;
    }

    private VBox buildNeuralPanel() {
        VBox root = new VBox(12);
        root.getStyleClass().add("card");
        Label title = new Label("Neural Inference");
        title.getStyleClass().add("panel-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField modelPath = new TextField("/models/rig_model.tflite");
        TextField version = new TextField("v1.0.0");
        TextField hash = new TextField("sha256:...");
        TextField targets = new TextField("all");

        grid.addRow(0, new Label("Model Path"), modelPath);
        grid.addRow(1, new Label("Version"), version);
        grid.addRow(2, new Label("Hash"), hash);
        grid.addRow(3, new Label("Target Devices"), targets);

        HBox buttons = new HBox(10,
                new Button("Deploy Model"),
                new Button("Validate"),
                new Button("Run Inference"),
                new Button("Rollback")
        );
        buttons.getChildren().forEach(n -> n.getStyleClass().add("secondary-button"));
        buttons.getChildren().get(0).getStyleClass().add("primary-button");

        Label note = new Label("UI scaffold only (not wired to execution yet).");
        note.getStyleClass().add("status-bar");

        root.getChildren().addAll(title, grid, buttons, note);
        return root;
    }

    @Override
    public void stop() {
        if (logService != null) {
            logService.getLogger().info("App stopping");
        }
        if (deviceManager != null) {
            deviceManager.shutdown();
        }
        if (logService != null) {
            logService.shutdown();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
