package com.antlab.rigcontrol;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.scene.control.cell.TextFieldTableCell;

import com.antlab.systemthinker.intel.sim.AnalystPolicy;
import com.antlab.systemthinker.intel.sim.CostWeights;
import com.antlab.systemthinker.intel.sim.ExperimentWriter;
import com.antlab.systemthinker.intel.sim.IntelBatchSummary;
import com.antlab.systemthinker.intel.sim.IntelDatasetConfig;
import com.antlab.systemthinker.intel.sim.IntelDatasetGenerator;
import com.antlab.systemthinker.intel.sim.IntelScenarioConfig;
import com.antlab.systemthinker.intel.sim.IntelSimulator;
import com.antlab.systemthinker.intel.sim.IntelSurrogateModel;
import com.antlab.systemthinker.intel.sim.IntelSurrogateTrainer;
import com.antlab.systemthinker.intel.sim.IntelSweepRunner;
import com.antlab.systemthinker.intel.sim.MonteCarloRunnerIntel;
import com.antlab.systemthinker.intel.sim.SourceConfig;
import com.antlab.systemthinker.intel.sim.SweepPoint;
import com.antlab.systemthinker.intel.sim.SweepSeries;

import com.antlab.systemthinker.sim.*;

public class App extends Application {
    private DeviceManager deviceManager;
    private LogService logService;
    private ExecutorService simExecutor;
    private MonteCarloRunner monteCarloRunner;
    private MonteCarloRunnerIntel intelRunner;
    private IntelSweepRunner intelSweepRunner;
    private ExperimentWriter experimentWriter;
    private IntelDatasetGenerator intelDatasetGenerator;
    private IntelSurrogateModel intelSurrogateModel;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        primaryStage = stage;
        Settings settings = new Settings();
        logService = new LogService(Path.of(System.getProperty("user.home"), ".rigcontrol", "logs"));
        ADBService adbService = new ADBService(settings, logService.getLogger());
        deviceManager = new DeviceManager(settings, adbService, logService.getLogger());
        simExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "sim-worker");
            t.setDaemon(true);
            return t;
        });
        monteCarloRunner = new MonteCarloRunner(new AntWarSimulator(), logService.getLogger(),
                MonteCarloRunner.DEFAULT_EPSILON,
                MonteCarloRunner.DEFAULT_CHECK_INTERVAL,
                MonteCarloRunner.DEFAULT_STABLE_CHECKS);
        intelRunner = new MonteCarloRunnerIntel(new IntelSimulator(),
                MonteCarloRunnerIntel.DEFAULT_EPSILON,
                MonteCarloRunnerIntel.DEFAULT_CHECK_INTERVAL,
                MonteCarloRunnerIntel.DEFAULT_STABLE_CHECKS);
        intelSweepRunner = new IntelSweepRunner(intelRunner);
        experimentWriter = new ExperimentWriter();
        intelDatasetGenerator = new IntelDatasetGenerator(intelRunner, experimentWriter);
        logService.getLogger().info("App starting");

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logService.getLogger().severe("Uncaught exception on " + thread.getName() + ": " + throwable));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.getStyleClass().add("root");

        VBox topBar = new VBox(6);
        topBar.getStyleClass().add("topbar");
        Button rescanButton = new Button("Rescan");
        Button rescanInfo = infoButton("Force an immediate device discovery scan.");
        Button pingAllButton = new Button("Ping all");
        Button pingAllInfo = infoButton("Send a ping to all online devices.");
        Button demoButton = new Button("Demo Data");
        Button demoInfo = infoButton("Load a simulated adb devices list for testing without hardware.");
        Button settingsButton = new Button("Settings");
        Button settingsInfo = infoButton("Configure ADB path, polling intervals, and limits.");
        Button legendButton = new Button("Legend");
        Button legendInfo = infoButton("Explain device states (device/offline/unauthorized/disconnected).");
        Button helpButton = new Button("Help");
        Button helpInfo = infoButton("Show reconnect checklist and troubleshooting steps.");
        Button importExpectedButton = new Button("Import Expected");
        Button importExpectedInfo = infoButton("Import expected serials from CSV/TXT.");
        Button exportSnapshotButton = new Button("Export Snapshot");
        Button exportSnapshotInfo = infoButton("Export the current table as CSV.");
        Button restartAdbButton = new Button("Restart ADB");
        Button restartAdbInfo = infoButton("Restart the local ADB server.");
        Button copyButton = new Button("Copy CSV");
        Button copyInfo = infoButton("Copy current table to clipboard as CSV.");
        Button quitButton = new Button("Quit");
        Button quitInfo = infoButton("Exit System Thinker.");
        CheckBox hideDisconnected = new CheckBox("Hide disconnected");
        Button hideDisconnectedInfo = infoButton("Hide devices that are no longer present.");
        CheckBox hideEmulators = new CheckBox("Hide emulators");
        Button hideEmulatorsInfo = infoButton("Hide Android emulator devices.");
        CheckBox pausePolling = new CheckBox("Pause");
        Button pausePollingInfo = infoButton("Pause polling and pinging without closing the app.");
        rescanButton.getStyleClass().add("secondary-button");
        pingAllButton.getStyleClass().add("primary-button");
        demoButton.getStyleClass().add("secondary-button");
        settingsButton.getStyleClass().add("secondary-button");
        legendButton.getStyleClass().add("secondary-button");
        helpButton.getStyleClass().add("secondary-button");
        importExpectedButton.getStyleClass().add("secondary-button");
        exportSnapshotButton.getStyleClass().add("secondary-button");
        restartAdbButton.getStyleClass().add("secondary-button");
        quitButton.getStyleClass().add("secondary-button");
        copyButton.getStyleClass().add("secondary-button");
        Label deviceCountLabel = new Label();
        deviceCountLabel.getStyleClass().add("chip-primary");
        Label adbHealthLabel = new Label();
        adbHealthLabel.getStyleClass().add("chip-ok");
        Label adbPathLabel = new Label();
        adbPathLabel.getStyleClass().add("chip-neutral");
        Label scanDurationLabel = new Label();
        scanDurationLabel.getStyleClass().add("chip-neutral");

        final boolean[] allowExit = {false};

        FilteredList<DeviceInfo> filtered = new FilteredList<>(deviceManager.getDevices(), d -> true);

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exportItem = new MenuItem("Export Snapshot...");
        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(e -> requestExit(stage, allowExit));
        fileMenu.getItems().addAll(exportItem, new SeparatorMenuItem(), quitItem);

        Menu viewMenu = new Menu("View");
        CheckMenuItem hideDisconnectedItem = new CheckMenuItem("Hide disconnected");
        hideDisconnectedItem.selectedProperty().bindBidirectional(hideDisconnected.selectedProperty());
        CheckMenuItem hideEmulatorsItem = new CheckMenuItem("Hide emulators");
        hideEmulatorsItem.selectedProperty().bindBidirectional(hideEmulators.selectedProperty());
        CheckMenuItem pausePollingItem = new CheckMenuItem("Pause polling");
        pausePollingItem.selectedProperty().bindBidirectional(pausePolling.selectedProperty());
        viewMenu.getItems().addAll(hideDisconnectedItem, hideEmulatorsItem, pausePollingItem);

        Menu toolsMenu = new Menu("Tools");
        MenuItem rescanItem = new MenuItem("Rescan");
        rescanItem.setOnAction(e -> deviceManager.rescanNow());
        MenuItem pingAllItem = new MenuItem("Ping all");
        pingAllItem.setOnAction(e -> deviceManager.pingAll());
        MenuItem restartAdbItem = new MenuItem("Restart ADB");
        restartAdbItem.setOnAction(e -> deviceManager.restartAdb());
        MenuItem demoItem = new MenuItem("Demo data");
        demoItem.setOnAction(e -> enableDemoData(settings));
        MenuItem importExpectedItem = new MenuItem("Import expected list...");
        importExpectedItem.setOnAction(e -> importExpected(stage));
        toolsMenu.getItems().addAll(rescanItem, pingAllItem, restartAdbItem, new SeparatorMenuItem(), demoItem, importExpectedItem);

        Menu helpMenu = new Menu("Help");
        MenuItem legendItem = new MenuItem("Device legend");
        legendItem.setOnAction(e -> showLegend(stage));
        MenuItem checklistItem = new MenuItem("Reconnect checklist");
        checklistItem.setOnAction(e -> showReconnectChecklist(stage));
        helpMenu.getItems().addAll(legendItem, checklistItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, toolsMenu, helpMenu);

        VBox scanGroup = buildRibbonGroup("Scan",
                new HBox(6, rescanButton, rescanInfo),
                new HBox(6, pausePolling, pausePollingInfo),
                new HBox(6, restartAdbButton, restartAdbInfo)
        );

        VBox pingGroup = buildRibbonGroup("Ping",
                new HBox(6, pingAllButton, pingAllInfo)
        );

        VBox dataGroup = buildRibbonGroup("Data",
                new HBox(6, demoButton, demoInfo),
                new HBox(6, importExpectedButton, importExpectedInfo),
                new HBox(6, exportSnapshotButton, exportSnapshotInfo),
                new HBox(6, copyButton, copyInfo)
        );

        VBox filterGroup = buildRibbonGroup("Filters",
                new HBox(6, hideDisconnected, hideDisconnectedInfo),
                new HBox(6, hideEmulators, hideEmulatorsInfo)
        );

        VBox helpGroup = buildRibbonGroup("Help",
                new HBox(6, legendButton, legendInfo),
                new HBox(6, helpButton, helpInfo)
        );

        VBox settingsGroup = buildRibbonGroup("System",
                new HBox(6, settingsButton, settingsInfo),
                new HBox(6, quitButton, quitInfo)
        );

        HBox leftRibbon = new HBox(12,
                scanGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                pingGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                dataGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                filterGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                helpGroup,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                settingsGroup
        );
        leftRibbon.getStyleClass().add("ribbon");

        HBox statusRight = new HBox(8, deviceCountLabel, adbHealthLabel, adbPathLabel, scanDurationLabel);
        statusRight.getStyleClass().add("toolbar-group");
        statusRight.setAlignment(Pos.CENTER_RIGHT);

        BorderPane ribbonRow = new BorderPane();
        ribbonRow.setLeft(leftRibbon);
        ribbonRow.setRight(statusRight);
        ribbonRow.setPadding(new Insets(6, 0, 0, 0));

        topBar.getChildren().addAll(menuBar, ribbonRow);
        root.setTop(topBar);

        exportItem.setOnAction(e -> exportSnapshot(stage, filtered));
        hideDisconnected.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(d -> filterDevice(d, hideDisconnected.isSelected(), hideEmulators.isSelected()));
        });
        hideEmulators.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(d -> filterDevice(d, hideDisconnected.isSelected(), hideEmulators.isSelected()));
        });

        TableView<DeviceInfo> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setEditable(true);

        TableColumn<DeviceInfo, String> serialCol = new TableColumn<>("Serial");
        serialCol.setCellValueFactory(data -> data.getValue().serialProperty());

        TableColumn<DeviceInfo, String> indexCol = new TableColumn<>("#");
        indexCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(table.getItems().indexOf(cell.getValue()) + 1)
        ));
        indexCol.setPrefWidth(50);

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

        table.getColumns().addAll(indexCol, serialCol, stateCol, modelCol, versionCol, lastSeenCol, pingCol, lastPingCol, expectedCol, tagCol);
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card");
        TabPane centerTabs = new TabPane();
        Tab monitorTab = new Tab("Monitor", tableCard);
        monitorTab.setClosable(false);
        Tab intelTab = new Tab("Intel Analytics", buildIntelAnalyticsPanel());
        intelTab.setClosable(false);
        Tab mcTab = new Tab("Monte Carlo", buildMonteCarloPanel());
        mcTab.setClosable(false);
        Tab aiTab = new Tab("Neural", buildNeuralPanel());
        aiTab.setClosable(false);
        centerTabs.getTabs().addAll(monitorTab, intelTab, mcTab, aiTab);
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

        Label actionLabel = new Label("Action: -");
        actionLabel.getStyleClass().add("status-bar");
        HBox telemetryRow = new HBox(12, telemetryLabel, expectedMissingLabel, lastErrorLabel, actionLabel);
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

        copyButton.setOnAction(e -> {
            copyCsv(filtered);
            flashAction(actionLabel, "Copied CSV");
        });
        exportSnapshotButton.setOnAction(e -> {
            exportSnapshot(stage, filtered);
            flashAction(actionLabel, "Export snapshot");
        });

        rescanButton.setOnAction(e -> {
            deviceManager.rescanNow();
            flashAction(actionLabel, "Rescan requested");
        });
        pingAllButton.setOnAction(e -> {
            deviceManager.pingAll();
            flashAction(actionLabel, "Ping all queued");
        });
        demoButton.setOnAction(e -> {
            enableDemoData(settings);
            flashAction(actionLabel, "Demo data loaded");
        });
        settingsButton.setOnAction(e -> {
            SettingsDialog.show(stage, settings, deviceManager);
            updateAdbPathLabel(adbPathLabel, settings);
            flashAction(actionLabel, "Opened settings");
        });
        legendButton.setOnAction(e -> {
            showLegend(stage);
            flashAction(actionLabel, "Opened legend");
        });
        helpButton.setOnAction(e -> {
            showReconnectChecklist(stage);
            flashAction(actionLabel, "Opened help");
        });
        importExpectedButton.setOnAction(e -> {
            importExpected(stage);
            flashAction(actionLabel, "Import expected list");
        });
        restartAdbButton.setOnAction(e -> {
            deviceManager.restartAdb();
            flashAction(actionLabel, "Restarting ADB");
        });
        pausePolling.selectedProperty().addListener((obs, oldVal, newVal) -> {
            deviceManager.setPaused(newVal);
            flashAction(actionLabel, newVal ? "Polling paused" : "Polling resumed");
        });
        quitButton.setOnAction(e -> {
            flashAction(actionLabel, "Quit requested");
            requestExit(stage, allowExit);
        });

        Timeline telemetryTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            telemetryLabel.setText(buildTelemetryText(deviceManager));
        }));
        telemetryTimer.setCycleCount(Timeline.INDEFINITE);
        telemetryTimer.play();

        deviceManager.start();

        Scene scene = new Scene(root, settings.getWindowWidth(), settings.getWindowHeight());
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setTitle("System Thinker v0.1");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!allowExit[0]) {
                logService.getLogger().info("Close requested (blocked). Use Quit button.");
                event.consume();
                return;
            }
            logService.getLogger().info("Stage close requested");
        });
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

    private void requestExit(Stage stage, boolean[] allowExit) {
        allowExit[0] = true;
        stage.close();
        Platform.exit();
    }

    private void updateAdbPathLabel(Label label, Settings settings) {
        String path = settings.getResolvedAdbPath();
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

    private void flashAction(Label label, String message) {
        if (label == null) {
            return;
        }
        label.setText("Action: " + message);
        Object existing = label.getProperties().get("actionTimer");
        if (existing instanceof PauseTransition) {
            ((PauseTransition) existing).stop();
        }
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> label.setText("Action: -"));
        label.getProperties().put("actionTimer", pause);
        pause.play();
    }

    private VBox buildRibbonGroup(String title, Node... nodes) {
        VBox group = new VBox(6);
        group.getStyleClass().add("ribbon-group");
        Label label = new Label(title);
        label.getStyleClass().add("ribbon-title");
        VBox items = new VBox(6);
        for (Node node : nodes) {
            items.getChildren().add(node);
        }
        group.getChildren().addAll(items, label);
        return group;
    }

    private Button infoButton(String message) {
        Button b = new Button("i");
        b.getStyleClass().add("info-button");
        b.setFocusTraversable(false);
        Tooltip tooltip = new Tooltip(message);
        tooltip.setShowDelay(Duration.millis(250));
        tooltip.setHideDelay(Duration.millis(0));
        tooltip.setShowDuration(Duration.seconds(8));
        Tooltip.install(b, tooltip);
        return b;
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

    private boolean filterDevice(DeviceInfo device, boolean hideDisconnected, boolean hideEmulators) {
        if (hideDisconnected && "disconnected".equalsIgnoreCase(device.getAdbState())) {
            return false;
        }
        if (hideEmulators && device.getSerial() != null && device.getSerial().startsWith("emulator-")) {
            return false;
        }
        return true;
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
        Label title = new Label("Monte Carlo Engine");
        title.getStyleClass().add("panel-title");

        GridPane scenarioGrid = new GridPane();
        scenarioGrid.setHgap(10);
        scenarioGrid.setVgap(10);

        TextField gridWField = new TextField("40");
        TextField gridHField = new TextField("30");
        TextField colonyAField = new TextField("80");
        TextField colonyBField = new TextField("80");
        TextField spawnAField = new TextField("0.2");
        TextField spawnBField = new TextField("0.2");
        TextField resourceField = new TextField("0.35");
        TextField biasField = new TextField("0.6");
        TextField detectionField = new TextField("3");
        TextField lethalityField = new TextField("0.35");
        TextField maxStepsField = new TextField("250");
        TextField thresholdField = new TextField("0.7");
        TextField streakField = new TextField("10");
        TextField trialsField = new TextField("1000");
        TextField seedField = new TextField("12345");

        scenarioGrid.addRow(0, new Label("Grid Width"), gridWField);
        scenarioGrid.addRow(1, new Label("Grid Height"), gridHField);
        scenarioGrid.addRow(2, new Label("Colony A Size"), colonyAField);
        scenarioGrid.addRow(3, new Label("Colony B Size"), colonyBField);
        scenarioGrid.addRow(4, new Label("Spawn Rate A"), spawnAField);
        scenarioGrid.addRow(5, new Label("Spawn Rate B"), spawnBField);
        scenarioGrid.addRow(6, new Label("Resource Density"), resourceField);
        scenarioGrid.addRow(7, new Label("Movement Bias"), biasField);
        scenarioGrid.addRow(8, new Label("Detection Radius"), detectionField);
        scenarioGrid.addRow(9, new Label("Lethality Coef"), lethalityField);
        scenarioGrid.addRow(10, new Label("Max Steps"), maxStepsField);
        scenarioGrid.addRow(11, new Label("Territory Threshold"), thresholdField);
        scenarioGrid.addRow(12, new Label("Decision Streak"), streakField);
        scenarioGrid.addRow(13, new Label("Trials"), trialsField);
        scenarioGrid.addRow(14, new Label("Seed"), seedField);

        Button runButton = new Button("Run Locally");
        Button runInfo = infoButton("Run Monte Carlo trials locally using the parameters above.");
        runButton.getStyleClass().add("primary-button");
        ProgressIndicator runProgress = new ProgressIndicator();
        runProgress.setPrefSize(18, 18);
        runProgress.setVisible(false);
        Label runStatus = new Label();
        runStatus.getStyleClass().add("status-bar");

        HBox runRow = new HBox(10, runButton, runInfo, runProgress, runStatus);

        TableView<ResultRow> resultTable = new TableView<>();
        resultTable.setPrefHeight(200);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<ResultRow, String> metricCol = new TableColumn<>("Metric");
        metricCol.setCellValueFactory(data -> data.getValue().metric);
        TableColumn<ResultRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> data.getValue().value);
        resultTable.getColumns().addAll(metricCol, valueCol);

        ResultRow winA = new ResultRow("Win Probability A", "-");
        ResultRow winB = new ResultRow("Win Probability B", "-");
        ResultRow winD = new ResultRow("Win Probability Draw", "-");
        ResultRow ciA = new ResultRow("95% CI (A)", "-");
        ResultRow meanTime = new ResultRow("Mean Time to Decision", "-");
        ResultRow stdTime = new ResultRow("Std Dev Time", "-");
        ResultRow meanCasualty = new ResultRow("Mean Casualty Ratio", "-");
        ResultRow trialsRun = new ResultRow("Trials Run", "-");
        ResultRow converged = new ResultRow("Converged", "-");
        ResultRow stabilized = new ResultRow("Stabilized At Trial", "-");

        resultTable.setItems(FXCollections.observableArrayList(
                winA, winB, winD, ciA, meanTime, stdTime, meanCasualty, trialsRun, converged, stabilized
        ));

        runButton.setOnAction(e -> {
            runButton.setDisable(true);
            runProgress.setVisible(true);
            runStatus.setText("Running...");

            ScenarioParameters params;
            int trials;
            try {
                params = new ScenarioParameters(
                        parseIntField(gridWField, 40),
                        parseIntField(gridHField, 30),
                        parseIntField(colonyAField, 80),
                        parseIntField(colonyBField, 80),
                        parseDoubleField(spawnAField, 0.2),
                        parseDoubleField(spawnBField, 0.2),
                        parseDoubleField(resourceField, 0.35),
                        parseDoubleField(biasField, 0.6),
                        parseIntField(detectionField, 3),
                        parseDoubleField(lethalityField, 0.35),
                        parseIntField(maxStepsField, 250),
                        parseLongField(seedField, 12345L),
                        parseDoubleField(thresholdField, 0.7),
                        parseIntField(streakField, 10)
                );
                trials = parseIntField(trialsField, 1000);
            } catch (Exception ex) {
                runStatus.setText("Invalid input: " + ex.getMessage());
                runProgress.setVisible(false);
                runButton.setDisable(false);
                return;
            }

            CompletableFuture.supplyAsync(() -> monteCarloRunner.runBatch(params, trials), simExecutor)
                    .whenComplete((summary, err) -> Platform.runLater(() -> {
                        if (err != null) {
                            runStatus.setText("Error: " + err.getMessage());
                        } else {
                            winA.value.set(format(summary.winProbabilityA));
                            winB.value.set(format(summary.winProbabilityB));
                            winD.value.set(format(summary.winProbabilityDraw));
                            ciA.value.set("[" + format(summary.confidenceLow) + ", " + format(summary.confidenceHigh) + "]");
                            meanTime.value.set(format(summary.meanTimeToDecision));
                            stdTime.value.set(format(summary.standardDeviationTime));
                            meanCasualty.value.set(format(summary.meanCasualtyRatio));
                            trialsRun.value.set(String.valueOf(summary.trialsRun));
                            converged.value.set(summary.convergenceMetrics.converged ? "yes" : "no");
                            stabilized.value.set(String.valueOf(summary.convergenceMetrics.stabilizedAtTrial));
                            runStatus.setText("Done");
                        }
                        runProgress.setVisible(false);
                        runButton.setDisable(false);
                    }));
        });

        TitledPane scenarioPane = new TitledPane("Scenario Parameters", scenarioGrid);
        scenarioPane.setExpanded(true);

        TitledPane resultsPane = new TitledPane("Results", resultTable);
        resultsPane.setExpanded(true);

        GridPane datasetGrid = new GridPane();
        datasetGrid.setHgap(10);
        datasetGrid.setVgap(10);

        TextField dsColonyAMin = new TextField("60");
        TextField dsColonyAMax = new TextField("120");
        TextField dsColonyBMin = new TextField("60");
        TextField dsColonyBMax = new TextField("120");
        TextField dsGridWMin = new TextField("30");
        TextField dsGridWMax = new TextField("60");
        TextField dsGridHMin = new TextField("20");
        TextField dsGridHMax = new TextField("40");
        TextField dsSpawnAMin = new TextField("0.1");
        TextField dsSpawnAMax = new TextField("0.4");
        TextField dsSpawnBMin = new TextField("0.1");
        TextField dsSpawnBMax = new TextField("0.4");
        TextField dsResourceMin = new TextField("0.2");
        TextField dsResourceMax = new TextField("0.6");
        TextField dsBiasMin = new TextField("0.2");
        TextField dsBiasMax = new TextField("0.8");
        TextField dsDetectMin = new TextField("1");
        TextField dsDetectMax = new TextField("5");
        TextField dsLethMin = new TextField("0.2");
        TextField dsLethMax = new TextField("0.6");
        TextField dsStepsMin = new TextField("150");
        TextField dsStepsMax = new TextField("300");
        TextField dsThresholdMin = new TextField("0.65");
        TextField dsThresholdMax = new TextField("0.8");
        TextField dsStreakMin = new TextField("8");
        TextField dsStreakMax = new TextField("15");
        TextField dsScenarios = new TextField("30");
        TextField dsTrials = new TextField("400");
        TextField dsSeed = new TextField("9001");

        datasetGrid.addRow(0, new Label("Colony A Min"), dsColonyAMin, new Label("Max"), dsColonyAMax);
        datasetGrid.addRow(1, new Label("Colony B Min"), dsColonyBMin, new Label("Max"), dsColonyBMax);
        datasetGrid.addRow(2, new Label("Grid W Min"), dsGridWMin, new Label("Max"), dsGridWMax);
        datasetGrid.addRow(3, new Label("Grid H Min"), dsGridHMin, new Label("Max"), dsGridHMax);
        datasetGrid.addRow(4, new Label("Spawn A Min"), dsSpawnAMin, new Label("Max"), dsSpawnAMax);
        datasetGrid.addRow(5, new Label("Spawn B Min"), dsSpawnBMin, new Label("Max"), dsSpawnBMax);
        datasetGrid.addRow(6, new Label("Resource Min"), dsResourceMin, new Label("Max"), dsResourceMax);
        datasetGrid.addRow(7, new Label("Bias Min"), dsBiasMin, new Label("Max"), dsBiasMax);
        datasetGrid.addRow(8, new Label("Detect Min"), dsDetectMin, new Label("Max"), dsDetectMax);
        datasetGrid.addRow(9, new Label("Lethality Min"), dsLethMin, new Label("Max"), dsLethMax);
        datasetGrid.addRow(10, new Label("Steps Min"), dsStepsMin, new Label("Max"), dsStepsMax);
        datasetGrid.addRow(11, new Label("Threshold Min"), dsThresholdMin, new Label("Max"), dsThresholdMax);
        datasetGrid.addRow(12, new Label("Streak Min"), dsStreakMin, new Label("Max"), dsStreakMax);
        datasetGrid.addRow(13, new Label("Scenarios"), dsScenarios);
        datasetGrid.addRow(14, new Label("Trials/Scenario"), dsTrials);
        datasetGrid.addRow(15, new Label("Base Seed"), dsSeed);

        Button datasetButton = new Button("Generate Dataset");
        Button datasetInfo = infoButton("Generate CSV datasets for ML training from random parameter ranges.");
        datasetButton.getStyleClass().add("primary-button");
        ProgressIndicator dsProgress = new ProgressIndicator();
        dsProgress.setPrefSize(18, 18);
        dsProgress.setVisible(false);
        Label dsStatus = new Label();
        dsStatus.getStyleClass().add("status-bar");

        HBox dsRow = new HBox(10, datasetButton, datasetInfo, dsProgress, dsStatus);

        datasetButton.setOnAction(e -> {
            datasetButton.setDisable(true);
            dsProgress.setVisible(true);
            dsStatus.setText("Generating...");

            DatasetConfig config;
            try {
                config = new DatasetConfig(
                        new IntRange(parseIntField(dsColonyAMin, 60), parseIntField(dsColonyAMax, 120)),
                        new IntRange(parseIntField(dsColonyBMin, 60), parseIntField(dsColonyBMax, 120)),
                        new IntRange(parseIntField(dsGridWMin, 30), parseIntField(dsGridWMax, 60)),
                        new IntRange(parseIntField(dsGridHMin, 20), parseIntField(dsGridHMax, 40)),
                        new DoubleRange(parseDoubleField(dsSpawnAMin, 0.1), parseDoubleField(dsSpawnAMax, 0.4)),
                        new DoubleRange(parseDoubleField(dsSpawnBMin, 0.1), parseDoubleField(dsSpawnBMax, 0.4)),
                        new DoubleRange(parseDoubleField(dsResourceMin, 0.2), parseDoubleField(dsResourceMax, 0.6)),
                        new DoubleRange(parseDoubleField(dsBiasMin, 0.2), parseDoubleField(dsBiasMax, 0.8)),
                        new IntRange(parseIntField(dsDetectMin, 1), parseIntField(dsDetectMax, 5)),
                        new DoubleRange(parseDoubleField(dsLethMin, 0.2), parseDoubleField(dsLethMax, 0.6)),
                        new IntRange(parseIntField(dsStepsMin, 150), parseIntField(dsStepsMax, 300)),
                        new DoubleRange(parseDoubleField(dsThresholdMin, 0.65), parseDoubleField(dsThresholdMax, 0.8)),
                        new IntRange(parseIntField(dsStreakMin, 8), parseIntField(dsStreakMax, 15)),
                        parseIntField(dsScenarios, 30),
                        parseIntField(dsTrials, 400),
                        parseLongField(dsSeed, 9001L)
                );
            } catch (Exception ex) {
                dsStatus.setText("Invalid input: " + ex.getMessage());
                dsProgress.setVisible(false);
                datasetButton.setDisable(false);
                return;
            }

            CompletableFuture.supplyAsync(() -> {
                try {
                    DatasetGenerator generator = new DatasetGenerator(monteCarloRunner, logService.getLogger());
                    return generator.generate(config);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, simExecutor).whenComplete((path, err) -> Platform.runLater(() -> {
                if (err != null) {
                    dsStatus.setText("Error: " + err.getMessage());
                } else {
                    dsStatus.setText("Saved: " + path);
                }
                dsProgress.setVisible(false);
                datasetButton.setDisable(false);
            }));
        });

        TitledPane datasetPane = new TitledPane("Dataset Generation", new VBox(10, datasetGrid, dsRow));
        datasetPane.setExpanded(false);

        root.getChildren().addAll(title, scenarioPane, runRow, resultsPane, datasetPane);
        return root;
    }

    private Node buildIntelAnalyticsPanel() {
        VBox root = new VBox(12);
        root.getStyleClass().add("card");

        Label title = new Label("Intel Analytics (Noise + Urgency)");
        title.getStyleClass().add("panel-title");

        TextField trialsField = new TextField("10000");
        TextField seedField = new TextField("4242");
        TextField horizonField = new TextField("2000");

        Slider urgencySlider = new Slider(0, 1, 0.4);
        urgencySlider.setShowTickLabels(true);
        urgencySlider.setShowTickMarks(true);
        urgencySlider.setMajorTickUnit(0.25);
        TextField urgencyField = new TextField("0.40");
        urgencyField.setPrefWidth(70);
        urgencySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                urgencyField.setText(String.format(Locale.US, "%.2f", newVal.doubleValue())));
        urgencyField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double value = Double.parseDouble(newVal);
                urgencySlider.setValue(Math.max(0.0, Math.min(1.0, value)));
            } catch (Exception ignored) {
            }
        });

        TextField baselineBeliefField = new TextField("0.25");
        TextField decayField = new TextField("0.92");
        TextField actThresholdField = new TextField("0.75");
        TextField investigateLowField = new TextField("0.45");
        TextField investigateHighField = new TextField("0.65");
        TextField urgencyActShiftField = new TextField("0.12");
        TextField urgencyInvestigateShiftField = new TextField("0.08");
        TextField invStepsField = new TextField("4");
        TextField invSensBoostField = new TextField("0.08");
        TextField invSpecBoostField = new TextField("0.05");
        TextField invDropoutField = new TextField("0.6");
        TextField invDelayField = new TextField("0.7");

        TextField fpCostField = new TextField("6.0");
        TextField fnCostField = new TextField("10.0");
        TextField invCostField = new TextField("0.8");
        TextField actCostField = new TextField("0.2");
        TextField urgencyPenaltyField = new TextField("1.2");

        TextField noToEmergingField = new TextField("0.02");
        TextField noToActiveField = new TextField("0.003");
        TextField emergingToNoField = new TextField("0.12");
        TextField emergingToActiveField = new TextField("0.18");
        TextField activeToNoField = new TextField("0.05");
        TextField activeToEmergingField = new TextField("0.10");

        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(10);
        generalGrid.addRow(0, new Label("Trials"), trialsField, new Label("Base Seed"), seedField);
        generalGrid.addRow(1, new Label("Time Horizon"), horizonField, new Label("Urgency (U)"),
                new HBox(6, urgencySlider, urgencyField));

        GridPane policyGrid = new GridPane();
        policyGrid.setHgap(10);
        policyGrid.setVgap(10);
        policyGrid.addRow(0, new Label("Baseline Belief"), baselineBeliefField, new Label("Decay Factor"), decayField);
        policyGrid.addRow(1, new Label("Act Threshold"), actThresholdField, new Label("Investigate Low"), investigateLowField);
        policyGrid.addRow(2, new Label("Investigate High"), investigateHighField, new Label("Urgency Act Shift"), urgencyActShiftField);
        policyGrid.addRow(3, new Label("Urgency Investigate Shift"), urgencyInvestigateShiftField, new Label("Investigation Steps"), invStepsField);
        policyGrid.addRow(4, new Label("Inv Sens Boost"), invSensBoostField, new Label("Inv Spec Boost"), invSpecBoostField);
        policyGrid.addRow(5, new Label("Inv Dropout Mult"), invDropoutField, new Label("Inv Delay Mult"), invDelayField);

        GridPane costGrid = new GridPane();
        costGrid.setHgap(10);
        costGrid.setVgap(10);
        costGrid.addRow(0, new Label("False Positive Cost"), fpCostField, new Label("False Negative Cost"), fnCostField);
        costGrid.addRow(1, new Label("Investigation Cost"), invCostField, new Label("Act Cost"), actCostField);
        costGrid.addRow(2, new Label("Urgency Penalty"), urgencyPenaltyField);

        GridPane transitionGrid = new GridPane();
        transitionGrid.setHgap(10);
        transitionGrid.setVgap(10);
        transitionGrid.addRow(0, new Label("NO->EMERGING"), noToEmergingField, new Label("NO->ACTIVE"), noToActiveField);
        transitionGrid.addRow(1, new Label("EMERGING->NO"), emergingToNoField, new Label("EMERGING->ACTIVE"), emergingToActiveField);
        transitionGrid.addRow(2, new Label("ACTIVE->NO"), activeToNoField, new Label("ACTIVE->EMERGING"), activeToEmergingField);

        TitledPane generalPane = new TitledPane("Batch Inputs", generalGrid);
        generalPane.setExpanded(true);
        TitledPane policyPane = new TitledPane("Policy & Investigation", policyGrid);
        policyPane.setExpanded(false);
        TitledPane costPane = new TitledPane("Costs", costGrid);
        costPane.setExpanded(false);
        TitledPane transitionPane = new TitledPane("Truth Transitions", transitionGrid);
        transitionPane.setExpanded(false);

        TableView<SourceRow> sourceTable = new TableView<>();
        sourceTable.setEditable(true);
        sourceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        var sourceRows = FXCollections.observableArrayList(
                new SourceRow("Source A", "0.78", "0.92", "0.00", "0.05", "0.70", "1.5", "4"),
                new SourceRow("Source B", "0.72", "0.88", "0.00", "0.08", "0.60", "2.0", "5"),
                new SourceRow("Source C", "0.70", "0.90", "0.02", "0.06", "0.55", "1.0", "3")
        );
        sourceTable.setItems(sourceRows);

        TableColumn<SourceRow, String> sourceNameCol = new TableColumn<>("Source");
        sourceNameCol.setCellValueFactory(data -> data.getValue().name);
        sourceNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> sensCol = new TableColumn<>("Sensitivity");
        sensCol.setCellValueFactory(data -> data.getValue().sensitivity);
        sensCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> specCol = new TableColumn<>("Specificity");
        specCol.setCellValueFactory(data -> data.getValue().specificity);
        specCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> biasCol = new TableColumn<>("Bias");
        biasCol.setCellValueFactory(data -> data.getValue().bias);
        biasCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> dropoutCol = new TableColumn<>("Dropout");
        dropoutCol.setCellValueFactory(data -> data.getValue().dropout);
        dropoutCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(data -> data.getValue().weight);
        weightCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> delayCol = new TableColumn<>("Delay Mean");
        delayCol.setCellValueFactory(data -> data.getValue().delayMean);
        delayCol.setCellFactory(TextFieldTableCell.forTableColumn());
        TableColumn<SourceRow, String> maxDelayCol = new TableColumn<>("Max Delay");
        maxDelayCol.setCellValueFactory(data -> data.getValue().maxDelay);
        maxDelayCol.setCellFactory(TextFieldTableCell.forTableColumn());
        sourceTable.getColumns().addAll(sourceNameCol, sensCol, specCol, biasCol, dropoutCol, weightCol, delayCol, maxDelayCol);

        Button addSourceButton = new Button("Add Source");
        Button addSourceInfo = infoButton("Append a new report source with default parameters.");
        Button removeSourceButton = new Button("Remove Selected");
        Button removeSourceInfo = infoButton("Remove the selected report source.");
        addSourceButton.getStyleClass().add("secondary-button");
        removeSourceButton.getStyleClass().add("secondary-button");
        addSourceButton.setOnAction(e -> sourceRows.add(new SourceRow("Source " + (sourceRows.size() + 1), "0.70", "0.90", "0.00", "0.05", "0.60", "1.5", "4")));
        removeSourceButton.setOnAction(e -> {
            SourceRow selected = sourceTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                sourceRows.remove(selected);
            }
        });

        HBox sourceButtons = new HBox(8, addSourceButton, addSourceInfo, removeSourceButton, removeSourceInfo);
        VBox sourcePaneContent = new VBox(8, sourceTable, sourceButtons);
        TitledPane sourcesPane = new TitledPane("Report Sources", sourcePaneContent);
        sourcesPane.setExpanded(true);

        Button runBatchButton = new Button("Run Simulation Batch");
        Button runBatchInfo = infoButton("Run deterministic Monte Carlo batch for the current config.");
        Button cancelBatchButton = new Button("Cancel");
        Button cancelBatchInfo = infoButton("Cancel the current batch run.");
        runBatchButton.getStyleClass().add("primary-button");
        cancelBatchButton.getStyleClass().add("secondary-button");
        cancelBatchButton.setDisable(true);

        ProgressBar batchProgress = new ProgressBar(0);
        batchProgress.setPrefWidth(200);
        Label batchStatus = new Label("Idle");
        batchStatus.getStyleClass().add("status-bar");
        Label batchThroughput = new Label("");
        batchThroughput.getStyleClass().add("status-bar");

        HBox batchButtons = new HBox(8, runBatchButton, runBatchInfo, cancelBatchButton, cancelBatchInfo);
        HBox batchStatusRow = new HBox(8, batchProgress, batchStatus, batchThroughput);
        VBox batchBox = new VBox(6, batchButtons, batchStatusRow);

        TableView<MetricRow> metricTable = new TableView<>();
        metricTable.setPrefHeight(220);
        metricTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<MetricRow, String> metricCol = new TableColumn<>("Metric");
        metricCol.setCellValueFactory(data -> data.getValue().metric);
        TableColumn<MetricRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> data.getValue().value);
        metricTable.getColumns().addAll(metricCol, valueCol);

        MetricRow totalCostMean = new MetricRow("Total Cost (mean)", "-");
        MetricRow totalCostP = new MetricRow("Total Cost (p5/p50/p95)", "-");
        MetricRow overMean = new MetricRow("Overreaction Rate (mean)", "-");
        MetricRow overCI = new MetricRow("Overreaction 95% CI", "-");
        MetricRow missMean = new MetricRow("Missed Threat Rate (mean)", "-");
        MetricRow missCI = new MetricRow("Missed Threat 95% CI", "-");
        MetricRow delayMean = new MetricRow("Decision Delay (mean)", "-");
        MetricRow delayP = new MetricRow("Decision Delay (p5/p50/p95)", "-");
        MetricRow accuracyMean = new MetricRow("Decision Accuracy (mean)", "-");
        MetricRow trialsRun = new MetricRow("Trials Run", "-");
        MetricRow converged = new MetricRow("Converged", "-");
        MetricRow convergedAt = new MetricRow("Converged At Trial", "-");
        metricTable.setItems(FXCollections.observableArrayList(totalCostMean, totalCostP, overMean, overCI, missMean, missCI,
                delayMean, delayP, accuracyMean, trialsRun, converged, convergedAt));

        Button exportSummaryButton = new Button("Export Summary CSV");
        Button exportSummaryInfo = infoButton("Export the current batch summary metrics as CSV.");
        exportSummaryButton.getStyleClass().add("secondary-button");
        HBox exportRow = new HBox(8, exportSummaryButton, exportSummaryInfo);

        TitledPane outputsPane = new TitledPane("Batch Outputs", new VBox(8, metricTable, exportRow));
        outputsPane.setExpanded(true);

        GridPane sweepGrid = new GridPane();
        sweepGrid.setHgap(10);
        sweepGrid.setVgap(10);
        TextField sweepPointsField = new TextField("8");
        TextField sweepTrialsField = new TextField("1200");
        TextField sweepSeedField = new TextField("8100");
        TextField specStartField = new TextField("0.95");
        TextField specEndField = new TextField("0.50");
        TextField sensStartField = new TextField("0.95");
        TextField sensEndField = new TextField("0.50");
        sweepGrid.addRow(0, new Label("Points"), sweepPointsField, new Label("Trials/Point"), sweepTrialsField);
        sweepGrid.addRow(1, new Label("Base Seed"), sweepSeedField, new Label("Spec Start"), specStartField);
        sweepGrid.addRow(2, new Label("Spec End"), specEndField, new Label("Sens Start"), sensStartField);
        sweepGrid.addRow(3, new Label("Sens End"), sensEndField);

        Button runSweepButton = new Button("Run Sweep Experiments");
        Button runSweepInfo = infoButton("Run specificity and sensitivity sweeps for baseline vs urgency.");
        Button cancelSweepButton = new Button("Cancel");
        Button cancelSweepInfo = infoButton("Cancel the sweep run.");
        runSweepButton.getStyleClass().add("primary-button");
        cancelSweepButton.getStyleClass().add("secondary-button");
        cancelSweepButton.setDisable(true);
        ProgressBar sweepProgress = new ProgressBar(0);
        sweepProgress.setPrefWidth(200);
        Label sweepStatus = new Label("Idle");
        sweepStatus.getStyleClass().add("status-bar");

        HBox sweepButtons = new HBox(8, runSweepButton, runSweepInfo, cancelSweepButton, cancelSweepInfo);
        HBox sweepStatusRow = new HBox(8, sweepProgress, sweepStatus);

        NumberAxis specXAxis = new NumberAxis();
        specXAxis.setLabel("False Positive Rate (1 - specificity)");
        NumberAxis specYAxis = new NumberAxis();
        specYAxis.setLabel("Total Cost");
        LineChart<Number, Number> specChart = new LineChart<>(specXAxis, specYAxis);
        specChart.setCreateSymbols(false);
        specChart.setPrefHeight(240);
        specChart.setTitle("Cost vs False Positive Rate");

        NumberAxis sensXAxis = new NumberAxis();
        sensXAxis.setLabel("Missed Detection Rate (1 - sensitivity)");
        NumberAxis sensYAxis = new NumberAxis();
        sensYAxis.setLabel("Total Cost");
        LineChart<Number, Number> sensChart = new LineChart<>(sensXAxis, sensYAxis);
        sensChart.setCreateSymbols(false);
        sensChart.setPrefHeight(240);
        sensChart.setTitle("Cost vs Missed Detection Rate");

        TextArea conclusionArea = new TextArea("No sweep results yet.");
        conclusionArea.setEditable(false);
        conclusionArea.setWrapText(true);
        conclusionArea.setPrefRowCount(4);

        VBox sweepCharts = new VBox(10, specChart, sensChart);
        TitledPane sweepPane = new TitledPane("Sweep Experiments",
                new VBox(10, sweepGrid, sweepButtons, sweepStatusRow, sweepCharts, conclusionArea));
        sweepPane.setExpanded(false);

        GridPane datasetGrid = new GridPane();
        datasetGrid.setHgap(10);
        datasetGrid.setVgap(10);
        TextField dsSensMin = new TextField("0.60");
        TextField dsSensMax = new TextField("0.95");
        TextField dsSpecMin = new TextField("0.60");
        TextField dsSpecMax = new TextField("0.98");
        TextField dsDropMin = new TextField("0.00");
        TextField dsDropMax = new TextField("0.20");
        TextField dsUrgMin = new TextField("0.00");
        TextField dsUrgMax = new TextField("0.90");
        TextField dsFpMin = new TextField("2.0");
        TextField dsFpMax = new TextField("9.0");
        TextField dsFnMin = new TextField("5.0");
        TextField dsFnMax = new TextField("14.0");
        TextField dsActMin = new TextField("0.55");
        TextField dsActMax = new TextField("0.85");
        TextField dsScenarios = new TextField("40");
        TextField dsTrials = new TextField("500");
        TextField dsSeed = new TextField("6060");
        datasetGrid.addRow(0, new Label("Sensitivity Min"), dsSensMin, new Label("Max"), dsSensMax);
        datasetGrid.addRow(1, new Label("Specificity Min"), dsSpecMin, new Label("Max"), dsSpecMax);
        datasetGrid.addRow(2, new Label("Dropout Min"), dsDropMin, new Label("Max"), dsDropMax);
        datasetGrid.addRow(3, new Label("Urgency Min"), dsUrgMin, new Label("Max"), dsUrgMax);
        datasetGrid.addRow(4, new Label("FP Cost Min"), dsFpMin, new Label("Max"), dsFpMax);
        datasetGrid.addRow(5, new Label("FN Cost Min"), dsFnMin, new Label("Max"), dsFnMax);
        datasetGrid.addRow(6, new Label("Act Threshold Min"), dsActMin, new Label("Max"), dsActMax);
        datasetGrid.addRow(7, new Label("Scenarios"), dsScenarios, new Label("Trials/Scenario"), dsTrials);
        datasetGrid.addRow(8, new Label("Base Seed"), dsSeed);

        Button datasetButton = new Button("Generate Dataset CSV");
        Button datasetInfo = infoButton("Generate ML-ready CSV datasets for intel simulations.");
        datasetButton.getStyleClass().add("primary-button");
        ProgressBar datasetProgress = new ProgressBar(0);
        datasetProgress.setPrefWidth(180);
        Label datasetStatus = new Label("Idle");
        datasetStatus.getStyleClass().add("status-bar");

        HBox datasetRow = new HBox(8, datasetButton, datasetInfo, datasetProgress, datasetStatus);
        TitledPane datasetPane = new TitledPane("Dataset Generation", new VBox(10, datasetGrid, datasetRow));
        datasetPane.setExpanded(false);

        GridPane aiGrid = new GridPane();
        aiGrid.setHgap(10);
        aiGrid.setVgap(10);
        TextField aiScenariosField = new TextField("30");
        TextField aiTrialsField = new TextField("300");
        TextField aiLambdaField = new TextField("0.5");
        aiGrid.addRow(0, new Label("Training Scenarios"), aiScenariosField, new Label("Trials/Scenario"), aiTrialsField);
        aiGrid.addRow(1, new Label("Ridge Lambda"), aiLambdaField);

        Button trainAiButton = new Button("Train Surrogate");
        Button trainAiInfo = infoButton("Train a lightweight surrogate model on generated Monte Carlo runs.");
        Button aiEstimateButton = new Button("AI Estimate");
        Button aiEstimateInfo = infoButton("Run the surrogate model to estimate metrics instantly.");
        trainAiButton.getStyleClass().add("primary-button");
        aiEstimateButton.getStyleClass().add("secondary-button");

        ProgressBar aiProgress = new ProgressBar(0);
        aiProgress.setPrefWidth(180);
        Label aiStatus = new Label("Idle");
        aiStatus.getStyleClass().add("status-bar");
        Label aiMetrics = new Label("No surrogate trained.");
        aiMetrics.getStyleClass().add("status-bar");

        HBox aiRow = new HBox(8, trainAiButton, trainAiInfo, aiEstimateButton, aiEstimateInfo);
        VBox aiBox = new VBox(8, aiGrid, aiRow, aiProgress, aiStatus, aiMetrics);
        TitledPane aiPane = new TitledPane("AI Surrogate (Optional)", aiBox);
        aiPane.setExpanded(false);

        VBox inputsBox = new VBox(10, generalPane, policyPane, costPane, transitionPane, sourcesPane);
        root.getChildren().addAll(title, inputsBox, batchBox, outputsPane, sweepPane, datasetPane, aiPane);

        AtomicBoolean batchCancel = new AtomicBoolean(false);
        AtomicBoolean sweepCancel = new AtomicBoolean(false);
        AtomicReference<IntelBatchSummary> lastSummaryRef = new AtomicReference<>();

        runBatchButton.setOnAction(e -> {
            IntelScenarioConfig config;
            int trials;
            long baseSeed;
            try {
                config = buildIntelConfig(trialsField, seedField, horizonField, urgencyField, baselineBeliefField, decayField,
                        actThresholdField, investigateLowField, investigateHighField, urgencyActShiftField, urgencyInvestigateShiftField,
                        invStepsField, invSensBoostField, invSpecBoostField, invDropoutField, invDelayField,
                        fpCostField, fnCostField, invCostField, actCostField, urgencyPenaltyField,
                        noToEmergingField, noToActiveField, emergingToNoField, emergingToActiveField, activeToNoField, activeToEmergingField,
                        sourceRows);
                trials = requireInt(trialsField, "Trials");
                baseSeed = requireLong(seedField, "Base Seed");
            } catch (Exception ex) {
                batchStatus.setText("Invalid input: " + ex.getMessage());
                return;
            }

            batchCancel.set(false);
            runBatchButton.setDisable(true);
            cancelBatchButton.setDisable(false);
            batchProgress.setProgress(0);
            batchStatus.setText("Running...");
            batchThroughput.setText("");
            logService.getLogger().info("Intel batch starting (" + trials + " trials).");

            CompletableFuture.supplyAsync(() -> {
                IntelBatchSummary summary = intelRunner.runBatch(config, trials, baseSeed,
                        MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL,
                        (completed, total, rate) -> Platform.runLater(() -> {
                            batchProgress.setProgress(total == 0 ? 0 : (double) completed / total);
                            batchStatus.setText("Running: " + completed + "/" + total);
                            batchThroughput.setText(String.format(Locale.US, "%.1f trials/s", rate));
                        }),
                        batchCancel::get);
                try {
                    experimentWriter.writeBatchArtifacts(config, summary, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL.name());
                } catch (Exception ex) {
                    logService.getLogger().warning("Intel batch artifacts failed: " + ex.getMessage());
                }
                return summary;
            }, simExecutor).whenComplete((summary, err) -> Platform.runLater(() -> {
                runBatchButton.setDisable(false);
                cancelBatchButton.setDisable(true);
                if (err != null) {
                    batchStatus.setText("Error: " + err.getMessage());
                } else {
                    lastSummaryRef.set(summary);
                    totalCostMean.value.set(format(summary.getTotalCostStats().mean()));
                    totalCostP.value.set(format(summary.getTotalCostStats().p5()) + "/" + format(summary.getTotalCostStats().p50()) + "/" + format(summary.getTotalCostStats().p95()));
                    overMean.value.set(format(summary.getOverreactionStats().mean()));
                    overCI.value.set("[" + format(summary.getOverreactionCI().lower()) + ", " + format(summary.getOverreactionCI().upper()) + "]");
                    missMean.value.set(format(summary.getMissedStats().mean()));
                    missCI.value.set("[" + format(summary.getMissedCI().lower()) + ", " + format(summary.getMissedCI().upper()) + "]");
                    delayMean.value.set(format(summary.getDecisionDelayStats().mean()));
                    delayP.value.set(format(summary.getDecisionDelayStats().p5()) + "/" + format(summary.getDecisionDelayStats().p50()) + "/" + format(summary.getDecisionDelayStats().p95()));
                    accuracyMean.value.set(format(summary.getDecisionAccuracyStats().mean()));
                    trialsRun.value.set(String.valueOf(summary.getTrials()));
                    converged.value.set(summary.getConvergenceMetrics().isConverged() ? "yes" : "no");
                    convergedAt.value.set(String.valueOf(summary.getConvergenceMetrics().getTrialsAtConvergence()));
                    batchStatus.setText(batchCancel.get() ? "Canceled" : "Done");
                    logService.getLogger().info("Intel batch finished (trials " + summary.getTrials() + ").");
                }
            }));
        });

        cancelBatchButton.setOnAction(e -> {
            batchCancel.set(true);
            batchStatus.setText("Canceling...");
        });

        exportSummaryButton.setOnAction(e -> {
            IntelBatchSummary summary = lastSummaryRef.get();
            if (summary == null) {
                batchStatus.setText("No batch summary to export.");
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Batch Summary CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            chooser.setInitialFileName("intel_batch_summary.csv");
            java.io.File file = chooser.showSaveDialog(primaryStage);
            if (file == null) {
                return;
            }
            String csv = "metric,value\\n" +
                    "total_cost_mean," + summary.getTotalCostStats().mean() + "\\n" +
                    "total_cost_p5," + summary.getTotalCostStats().p5() + "\\n" +
                    "total_cost_p50," + summary.getTotalCostStats().p50() + "\\n" +
                    "total_cost_p95," + summary.getTotalCostStats().p95() + "\\n" +
                    "overreaction_mean," + summary.getOverreactionStats().mean() + "\\n" +
                    "overreaction_ci_low," + summary.getOverreactionCI().lower() + "\\n" +
                    "overreaction_ci_high," + summary.getOverreactionCI().upper() + "\\n" +
                    "missed_mean," + summary.getMissedStats().mean() + "\\n" +
                    "missed_ci_low," + summary.getMissedCI().lower() + "\\n" +
                    "missed_ci_high," + summary.getMissedCI().upper() + "\\n" +
                    "decision_delay_mean," + summary.getDecisionDelayStats().mean() + "\\n" +
                    "decision_delay_p50," + summary.getDecisionDelayStats().p50() + "\\n" +
                    "decision_accuracy_mean," + summary.getDecisionAccuracyStats().mean() + "\\n" +
                    "trials," + summary.getTrials() + "\\n";
            try {
                Files.writeString(file.toPath(), csv);
                batchStatus.setText("Exported: " + file.getName());
            } catch (Exception ex) {
                batchStatus.setText("Export failed: " + ex.getMessage());
            }
        });

        runSweepButton.setOnAction(e -> {
            IntelScenarioConfig config;
            int points;
            int trialsPerPoint;
            long baseSeed;
            double specStart;
            double specEnd;
            double sensStart;
            double sensEnd;
            try {
                config = buildIntelConfig(trialsField, seedField, horizonField, urgencyField, baselineBeliefField, decayField,
                        actThresholdField, investigateLowField, investigateHighField, urgencyActShiftField, urgencyInvestigateShiftField,
                        invStepsField, invSensBoostField, invSpecBoostField, invDropoutField, invDelayField,
                        fpCostField, fnCostField, invCostField, actCostField, urgencyPenaltyField,
                        noToEmergingField, noToActiveField, emergingToNoField, emergingToActiveField, activeToNoField, activeToEmergingField,
                        sourceRows);
                points = requireInt(sweepPointsField, "Sweep Points");
                trialsPerPoint = requireInt(sweepTrialsField, "Trials per point");
                baseSeed = requireLong(sweepSeedField, "Sweep Seed");
                specStart = requireProbability(specStartField, "Spec Start");
                specEnd = requireProbability(specEndField, "Spec End");
                sensStart = requireProbability(sensStartField, "Sens Start");
                sensEnd = requireProbability(sensEndField, "Sens End");
            } catch (Exception ex) {
                sweepStatus.setText("Invalid input: " + ex.getMessage());
                return;
            }

            sweepCancel.set(false);
            runSweepButton.setDisable(true);
            cancelSweepButton.setDisable(false);
            sweepProgress.setProgress(0);
            sweepStatus.setText("Running sweeps...");
            conclusionArea.setText("Running sweeps...");
            specChart.getData().clear();
            sensChart.getData().clear();
            logService.getLogger().info("Intel sweep starting (" + points + " points).");

            CompletableFuture.supplyAsync(() -> {
                AtomicInteger done = new AtomicInteger(0);
                int totalPoints = points * 4;
                IntelSweepRunner.SweepProgressListener progressListener = (completed, total) -> Platform.runLater(() -> {
                    int overall = done.incrementAndGet();
                    sweepProgress.setProgress((double) overall / totalPoints);
                    sweepStatus.setText("Sweep progress: " + overall + "/" + totalPoints);
                });

                IntelScenarioConfig baseline = config.withUrgency(0.0);
                SweepSeries specBaseline = intelSweepRunner.runSpecificitySweep(baseline, specStart, specEnd, points,
                        trialsPerPoint, baseSeed, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, sweepCancel::get, progressListener);
                SweepSeries specUrgency = intelSweepRunner.runSpecificitySweep(config, specStart, specEnd, points,
                        trialsPerPoint, baseSeed + 9999L, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, sweepCancel::get, progressListener);
                SweepSeries sensBaseline = intelSweepRunner.runSensitivitySweep(baseline, sensStart, sensEnd, points,
                        trialsPerPoint, baseSeed + 19999L, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, sweepCancel::get, progressListener);
                SweepSeries sensUrgency = intelSweepRunner.runSensitivitySweep(config, sensStart, sensEnd, points,
                        trialsPerPoint, baseSeed + 29999L, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, sweepCancel::get, progressListener);
                try {
                    experimentWriter.writeSweepArtifacts(config, specBaseline, specUrgency, "specificity");
                    experimentWriter.writeSweepArtifacts(config, sensBaseline, sensUrgency, "sensitivity");
                } catch (Exception ex) {
                    logService.getLogger().warning("Intel sweep artifacts failed: " + ex.getMessage());
                }
                return new SweepPayload(specBaseline, specUrgency, sensBaseline, sensUrgency);
            }, simExecutor).whenComplete((payload, err) -> Platform.runLater(() -> {
                runSweepButton.setDisable(false);
                cancelSweepButton.setDisable(true);
                if (err != null) {
                    sweepStatus.setText("Error: " + err.getMessage());
                    conclusionArea.setText("Sweep failed: " + err.getMessage());
                    return;
                }
                updateSweepChart(specChart, payload.specBaseline, payload.specUrgency,
                        "Baseline (U=0)", "Urgency (U=" + format(requireDouble(urgencyField, "Urgency")) + ")");
                updateSweepChart(sensChart, payload.sensBaseline, payload.sensUrgency,
                        "Baseline (U=0)", "Urgency (U=" + format(requireDouble(urgencyField, "Urgency")) + ")");
                conclusionArea.setText(buildIntelConclusion(payload.specBaseline, payload.specUrgency,
                        payload.sensBaseline, payload.sensUrgency, requireDouble(urgencyField, "Urgency")));
                sweepStatus.setText(sweepCancel.get() ? "Canceled" : "Done");
                logService.getLogger().info("Intel sweep finished.");
            }));
        });

        cancelSweepButton.setOnAction(e -> {
            sweepCancel.set(true);
            sweepStatus.setText("Canceling...");
        });

        datasetButton.setOnAction(e -> {
            IntelScenarioConfig baseConfig;
            IntelDatasetConfig datasetConfig;
            try {
                baseConfig = buildIntelConfig(trialsField, seedField, horizonField, urgencyField, baselineBeliefField, decayField,
                        actThresholdField, investigateLowField, investigateHighField, urgencyActShiftField, urgencyInvestigateShiftField,
                        invStepsField, invSensBoostField, invSpecBoostField, invDropoutField, invDelayField,
                        fpCostField, fnCostField, invCostField, actCostField, urgencyPenaltyField,
                        noToEmergingField, noToActiveField, emergingToNoField, emergingToActiveField, activeToNoField, activeToEmergingField,
                        sourceRows);
                datasetConfig = new IntelDatasetConfig(
                        requireInt(dsScenarios, "Scenarios"),
                        requireInt(dsTrials, "Trials/Scenario"),
                        requireLong(dsSeed, "Dataset Seed"),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsSensMin, "Sens Min"), requireProbability(dsSensMax, "Sens Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsSpecMin, "Spec Min"), requireProbability(dsSpecMax, "Spec Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsDropMin, "Drop Min"), requireProbability(dsDropMax, "Drop Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsUrgMin, "Urgency Min"), requireProbability(dsUrgMax, "Urgency Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireDouble(dsFpMin, "FP Cost Min"), requireDouble(dsFpMax, "FP Cost Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireDouble(dsFnMin, "FN Cost Min"), requireDouble(dsFnMax, "FN Cost Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsActMin, "Act Th Min"), requireProbability(dsActMax, "Act Th Max"))
                );
            } catch (Exception ex) {
                datasetStatus.setText("Invalid input: " + ex.getMessage());
                return;
            }

            datasetButton.setDisable(true);
            datasetProgress.setProgress(-1);
            datasetStatus.setText("Generating...");
            logService.getLogger().info("Intel dataset generation starting.");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return intelDatasetGenerator.generateDataset(baseConfig, datasetConfig);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, simExecutor).whenComplete((path, err) -> Platform.runLater(() -> {
                datasetButton.setDisable(false);
                datasetProgress.setProgress(0);
                if (err != null) {
                    datasetStatus.setText("Error: " + err.getMessage());
                } else {
                    datasetStatus.setText("Saved: " + path);
                    logService.getLogger().info("Intel dataset saved: " + path);
                }
            }));
        });

        trainAiButton.setOnAction(e -> {
            IntelScenarioConfig baseConfig;
            IntelDatasetConfig datasetConfig;
            int scenarios;
            int trialsPerScenario;
            double lambda;
            try {
                baseConfig = buildIntelConfig(trialsField, seedField, horizonField, urgencyField, baselineBeliefField, decayField,
                        actThresholdField, investigateLowField, investigateHighField, urgencyActShiftField, urgencyInvestigateShiftField,
                        invStepsField, invSensBoostField, invSpecBoostField, invDropoutField, invDelayField,
                        fpCostField, fnCostField, invCostField, actCostField, urgencyPenaltyField,
                        noToEmergingField, noToActiveField, emergingToNoField, emergingToActiveField, activeToNoField, activeToEmergingField,
                        sourceRows);
                datasetConfig = new IntelDatasetConfig(
                        requireInt(dsScenarios, "Scenarios"),
                        requireInt(dsTrials, "Trials/Scenario"),
                        requireLong(dsSeed, "Dataset Seed"),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsSensMin, "Sens Min"), requireProbability(dsSensMax, "Sens Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsSpecMin, "Spec Min"), requireProbability(dsSpecMax, "Spec Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsDropMin, "Drop Min"), requireProbability(dsDropMax, "Drop Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsUrgMin, "Urgency Min"), requireProbability(dsUrgMax, "Urgency Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireDouble(dsFpMin, "FP Cost Min"), requireDouble(dsFpMax, "FP Cost Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireDouble(dsFnMin, "FN Cost Min"), requireDouble(dsFnMax, "FN Cost Max")),
                        new com.antlab.systemthinker.intel.sim.DoubleRange(requireProbability(dsActMin, "Act Th Min"), requireProbability(dsActMax, "Act Th Max"))
                );
                scenarios = requireInt(aiScenariosField, "Training Scenarios");
                trialsPerScenario = requireInt(aiTrialsField, "Trials/Scenario");
                lambda = requireDouble(aiLambdaField, "Lambda");
            } catch (Exception ex) {
                aiStatus.setText("Invalid input: " + ex.getMessage());
                return;
            }

            trainAiButton.setDisable(true);
            aiProgress.setProgress(-1);
            aiStatus.setText("Training...");
            aiMetrics.setText("Training surrogate...");

            CompletableFuture.supplyAsync(() -> IntelSurrogateTrainer.train(baseConfig, datasetConfig,
                    scenarios, trialsPerScenario, lambda), simExecutor)
                    .whenComplete((model, err) -> Platform.runLater(() -> {
                        trainAiButton.setDisable(false);
                        aiProgress.setProgress(0);
                        if (err != null) {
                            aiStatus.setText("Training error: " + err.getMessage());
                        } else {
                            intelSurrogateModel = model;
                            aiStatus.setText("Surrogate trained");
                            aiMetrics.setText(String.format(Locale.US, "MAE cost %.3f | over %.3f | missed %.3f | delay %.3f",
                                    model.getMaeCost(), model.getMaeOverreaction(), model.getMaeMissed(), model.getMaeDelay()));
                        }
                    }));
        });

        aiEstimateButton.setOnAction(e -> {
            if (intelSurrogateModel == null) {
                aiStatus.setText("Train surrogate first.");
                return;
            }
            IntelScenarioConfig config;
            try {
                config = buildIntelConfig(trialsField, seedField, horizonField, urgencyField, baselineBeliefField, decayField,
                        actThresholdField, investigateLowField, investigateHighField, urgencyActShiftField, urgencyInvestigateShiftField,
                        invStepsField, invSensBoostField, invSpecBoostField, invDropoutField, invDelayField,
                        fpCostField, fnCostField, invCostField, actCostField, urgencyPenaltyField,
                        noToEmergingField, noToActiveField, emergingToNoField, emergingToActiveField, activeToNoField, activeToEmergingField,
                        sourceRows);
            } catch (Exception ex) {
                aiStatus.setText("Invalid input: " + ex.getMessage());
                return;
            }
            long start = System.nanoTime();
            double[] features = IntelSurrogateTrainer.extractFeatures(config);
            double cost = intelSurrogateModel.getCostModel().predict(features);
            double over = intelSurrogateModel.getOverreactionModel().predict(features);
            double missed = intelSurrogateModel.getMissedModel().predict(features);
            double delay = intelSurrogateModel.getDelayModel().predict(features);
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            aiStatus.setText("AI estimate in " + durationMs + " ms");
            aiMetrics.setText(String.format(Locale.US, "Predicted cost %.3f | over %.3f | missed %.3f | delay %.3f",
                    cost, over, missed, delay));
        });

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
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

    private int parseIntField(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLongField(TextField field, long fallback) {
        try {
            return Long.parseLong(field.getText().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDoubleField(TextField field, double fallback) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private int requireInt(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(name + " is invalid");
        }
    }

    private long requireLong(TextField field, String name) {
        try {
            return Long.parseLong(field.getText().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(name + " is invalid");
        }
    }

    private double requireDouble(TextField field, String name) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(name + " is invalid");
        }
    }

    private double requireProbability(TextField field, String name) {
        double value = requireDouble(field, name);
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
        return value;
    }

    private IntelScenarioConfig buildIntelConfig(TextField trialsField,
                                                 TextField seedField,
                                                 TextField horizonField,
                                                 TextField urgencyField,
                                                 TextField baselineBeliefField,
                                                 TextField decayField,
                                                 TextField actThresholdField,
                                                 TextField investigateLowField,
                                                 TextField investigateHighField,
                                                 TextField urgencyActShiftField,
                                                 TextField urgencyInvestigateShiftField,
                                                 TextField invStepsField,
                                                 TextField invSensBoostField,
                                                 TextField invSpecBoostField,
                                                 TextField invDropoutField,
                                                 TextField invDelayField,
                                                 TextField fpCostField,
                                                 TextField fnCostField,
                                                 TextField invCostField,
                                                 TextField actCostField,
                                                 TextField urgencyPenaltyField,
                                                 TextField noToEmergingField,
                                                 TextField noToActiveField,
                                                 TextField emergingToNoField,
                                                 TextField emergingToActiveField,
                                                 TextField activeToNoField,
                                                 TextField activeToEmergingField,
                                                 List<SourceRow> sourceRows) {
        int horizon = requireInt(horizonField, "Time Horizon");
        double urgency = requireProbability(urgencyField, "Urgency");

        double baselineBelief = requireProbability(baselineBeliefField, "Baseline Belief");
        double decay = requireProbability(decayField, "Decay Factor");
        double actThreshold = requireProbability(actThresholdField, "Act Threshold");
        double investigateLow = requireProbability(investigateLowField, "Investigate Low");
        double investigateHigh = requireProbability(investigateHighField, "Investigate High");
        if (investigateLow > investigateHigh) {
            throw new IllegalArgumentException("Investigate Low must be <= Investigate High");
        }

        double urgencyActShift = requireDouble(urgencyActShiftField, "Urgency Act Shift");
        double urgencyInvestigateShift = requireDouble(urgencyInvestigateShiftField, "Urgency Investigate Shift");
        int invSteps = requireInt(invStepsField, "Investigation Steps");
        double invSensBoost = requireDouble(invSensBoostField, "Inv Sens Boost");
        double invSpecBoost = requireDouble(invSpecBoostField, "Inv Spec Boost");
        double invDropout = requireDouble(invDropoutField, "Inv Dropout Mult");
        double invDelay = requireDouble(invDelayField, "Inv Delay Mult");

        AnalystPolicy policy = new AnalystPolicy(
                baselineBelief,
                decay,
                actThreshold,
                investigateLow,
                investigateHigh,
                urgencyActShift,
                urgencyInvestigateShift,
                invSteps,
                invSensBoost,
                invSpecBoost,
                invDropout,
                invDelay
        );

        CostWeights costs = new CostWeights(
                requireDouble(fpCostField, "False Positive Cost"),
                requireDouble(fnCostField, "False Negative Cost"),
                requireDouble(invCostField, "Investigation Cost"),
                requireDouble(actCostField, "Act Cost"),
                requireDouble(urgencyPenaltyField, "Urgency Penalty")
        );

        double[][] transitions = buildTransitionMatrix(
                requireProbability(noToEmergingField, "NO->EMERGING"),
                requireProbability(noToActiveField, "NO->ACTIVE"),
                requireProbability(emergingToNoField, "EMERGING->NO"),
                requireProbability(emergingToActiveField, "EMERGING->ACTIVE"),
                requireProbability(activeToNoField, "ACTIVE->NO"),
                requireProbability(activeToEmergingField, "ACTIVE->EMERGING")
        );

        if (sourceRows.isEmpty()) {
            throw new IllegalArgumentException("At least one source is required");
        }
        List<SourceConfig> sources = new ArrayList<>();
        for (int i = 0; i < sourceRows.size(); i++) {
            sources.add(sourceRows.get(i).toConfig(i));
        }

        return new IntelScenarioConfig(horizon, transitions, sources, policy, costs, urgency);
    }

    private double[][] buildTransitionMatrix(double noToEmerging,
                                             double noToActive,
                                             double emergingToNo,
                                             double emergingToActive,
                                             double activeToNo,
                                             double activeToEmerging) {
        double[] row0 = normalizeRow(new double[] {1.0 - noToEmerging - noToActive, noToEmerging, noToActive});
        double[] row1 = normalizeRow(new double[] {emergingToNo, 1.0 - emergingToNo - emergingToActive, emergingToActive});
        double[] row2 = normalizeRow(new double[] {activeToNo, activeToEmerging, 1.0 - activeToNo - activeToEmerging});
        return new double[][] {row0, row1, row2};
    }

    private double[] normalizeRow(double[] row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] < 0.0) {
                row[i] = 0.0;
            }
        }
        double sum = 0.0;
        for (double value : row) {
            sum += value;
        }
        if (sum <= 0.0) {
            return new double[] {1.0, 0.0, 0.0};
        }
        for (int i = 0; i < row.length; i++) {
            row[i] /= sum;
        }
        return row;
    }

    private void updateSweepChart(LineChart<Number, Number> chart,
                                  SweepSeries baseline,
                                  SweepSeries urgency,
                                  String baselineLabel,
                                  String urgencyLabel) {
        chart.getData().clear();
        if (baseline == null || urgency == null) {
            return;
        }
        XYChart.Series<Number, Number> baselineSeries = new XYChart.Series<>();
        baselineSeries.setName(baselineLabel);
        for (SweepPoint point : baseline.getPoints()) {
            baselineSeries.getData().add(new XYChart.Data<>(point.xValue(), point.meanTotalCost()));
        }
        XYChart.Series<Number, Number> urgencySeries = new XYChart.Series<>();
        urgencySeries.setName(urgencyLabel);
        for (SweepPoint point : urgency.getPoints()) {
            urgencySeries.getData().add(new XYChart.Data<>(point.xValue(), point.meanTotalCost()));
        }
        chart.getData().addAll(baselineSeries, urgencySeries);
    }

    private String buildIntelConclusion(SweepSeries specBaseline,
                                        SweepSeries specUrgency,
                                        SweepSeries sensBaseline,
                                        SweepSeries sensUrgency,
                                        double urgency) {
        if (specBaseline == null || sensBaseline == null || specBaseline.getPoints().isEmpty() || sensBaseline.getPoints().isEmpty()) {
            return "No sweep data to conclude.";
        }
        double specDelta = specBaseline.getPoints().get(specBaseline.getPoints().size() - 1).meanTotalCost()
                - specBaseline.getPoints().get(0).meanTotalCost();
        double sensDelta = sensBaseline.getPoints().get(sensBaseline.getPoints().size() - 1).meanTotalCost()
                - sensBaseline.getPoints().get(0).meanTotalCost();
        String h1 = specDelta > sensDelta ? "supports" : "does not clearly support";

        double specUrgencyImpact = specUrgency.getPoints().get(specUrgency.getPoints().size() - 1).meanTotalCost()
                - specBaseline.getPoints().get(specBaseline.getPoints().size() - 1).meanTotalCost();
        double sensUrgencyImpact = sensUrgency.getPoints().get(sensUrgency.getPoints().size() - 1).meanTotalCost()
                - sensBaseline.getPoints().get(sensBaseline.getPoints().size() - 1).meanTotalCost();
        String h2 = specUrgencyImpact > sensUrgencyImpact ? "supports" : "does not clearly support";

        return String.format(Locale.US,
                "H1: Degrading specificity increased cost by %.3f vs sensitivity by %.3f; this %s H1. " +
                        "H2: Urgency U=%.2f increased low-specificity cost by %.3f vs low-sensitivity by %.3f; this %s H2.",
                specDelta, sensDelta, h1, urgency, specUrgencyImpact, sensUrgencyImpact, h2);
    }

    private static class MetricRow {
        final SimpleStringProperty metric;
        final SimpleStringProperty value;

        MetricRow(String metric, String value) {
            this.metric = new SimpleStringProperty(metric);
            this.value = new SimpleStringProperty(value);
        }
    }

    private static class SourceRow {
        final SimpleStringProperty name;
        final SimpleStringProperty sensitivity;
        final SimpleStringProperty specificity;
        final SimpleStringProperty bias;
        final SimpleStringProperty dropout;
        final SimpleStringProperty weight;
        final SimpleStringProperty delayMean;
        final SimpleStringProperty maxDelay;

        SourceRow(String name,
                  String sensitivity,
                  String specificity,
                  String bias,
                  String dropout,
                  String weight,
                  String delayMean,
                  String maxDelay) {
            this.name = new SimpleStringProperty(name);
            this.sensitivity = new SimpleStringProperty(sensitivity);
            this.specificity = new SimpleStringProperty(specificity);
            this.bias = new SimpleStringProperty(bias);
            this.dropout = new SimpleStringProperty(dropout);
            this.weight = new SimpleStringProperty(weight);
            this.delayMean = new SimpleStringProperty(delayMean);
            this.maxDelay = new SimpleStringProperty(maxDelay);
        }

        SourceConfig toConfig(int id) {
            double sens = parseDouble(sensitivity.get(), "Sensitivity");
            double spec = parseDouble(specificity.get(), "Specificity");
            double biasVal = parseDouble(bias.get(), "Bias");
            double drop = parseDouble(dropout.get(), "Dropout");
            double weightVal = parseDouble(weight.get(), "Weight");
            double delay = parseDouble(delayMean.get(), "Delay Mean");
            int maxDelayVal = parseInt(maxDelay.get(), "Max Delay");
            if (sens < 0.0 || sens > 1.0 || spec < 0.0 || spec > 1.0 || drop < 0.0 || drop > 1.0) {
                throw new IllegalArgumentException("Source values must be within 0..1 where applicable");
            }
            return new SourceConfig(id, name.get(), sens, spec, biasVal, drop, weightVal, delay, Math.max(0, maxDelayVal));
        }

        private double parseDouble(String value, String label) {
            try {
                return Double.parseDouble(value.trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException(label + " is invalid");
            }
        }

        private int parseInt(String value, String label) {
            try {
                return Integer.parseInt(value.trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException(label + " is invalid");
            }
        }
    }

    private static class SweepPayload {
        final SweepSeries specBaseline;
        final SweepSeries specUrgency;
        final SweepSeries sensBaseline;
        final SweepSeries sensUrgency;

        SweepPayload(SweepSeries specBaseline, SweepSeries specUrgency, SweepSeries sensBaseline, SweepSeries sensUrgency) {
            this.specBaseline = specBaseline;
            this.specUrgency = specUrgency;
            this.sensBaseline = sensBaseline;
            this.sensUrgency = sensUrgency;
        }
    }

    private static class ResultRow {
        final SimpleStringProperty metric;
        final SimpleStringProperty value;

        ResultRow(String metric, String value) {
            this.metric = new SimpleStringProperty(metric);
            this.value = new SimpleStringProperty(value);
        }
    }

    @Override
    public void stop() {
        if (logService != null) {
            logService.getLogger().info("App stopping");
        }
        if (deviceManager != null) {
            deviceManager.shutdown();
        }
        if (simExecutor != null) {
            simExecutor.shutdownNow();
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
