package com.antlab.rigcontrol;

import com.antlab.rigcontrol.sorter.*;
import com.antlab.rigcontrol.worker.WorkerClient;
import com.antlab.rigcontrol.worker.WorkerMonitor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.util.logging.Logger;

public class App extends Application {
    private final ObservableList<FileRecord> fileRecords = FXCollections.observableArrayList();
    private final ObservableList<Rule> ruleRecords = FXCollections.observableArrayList();
    private final ObservableList<AuditRecord> auditRecords = FXCollections.observableArrayList();

    private Settings settings;
    private LogService logService;
    private ADBService adbService;
    private DeviceManager deviceManager;
    private WorkerClient workerClient;
    private WorkerMonitor workerMonitor;
    private ProjectManager projectManager;
    private Dispatcher dispatcher;
    private PreviewGenerator previewGenerator;
    private RulesEngine rulesEngine;
    private FileMover fileMover;

    private ExecutorService ioExecutor;

    private TableView<FileRecord> sorterTable;
    private TableView<FileRecord> reviewTable;
    private TableView<AuditRecord> auditTable;
    private FilteredList<FileRecord> sorterFiltered;
    private FilteredList<FileRecord> reviewFiltered;
    private FilteredList<AuditRecord> auditFiltered;
    private TextField auditSearchField;
    private ListView<String> logView;
    private Label statusCounts;
    private Label queueDepthLabel;
    private Label deviceCountLabel;

    private ImageView inspectorImage;
    private Label inspectorPath;
    private Label inspectorMeta;
    private Label inspectorLabel;
    private Label inspectorDestination;
    private Label inspectorRule;

    private TextField sourceField;
    private TextField destField;
    private ComboBox<String> filterBox;
    private TextField batchField;
    private TextField inFlightField;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        primaryStage = stage;
        settings = new Settings();
        logService = new LogService(Path.of(System.getProperty("user.home"), ".rigsort", "logs"));
        Logger logger = logService.getLogger();

        ADBService adbService = new ADBService(settings, logger);
        this.adbService = adbService;
        deviceManager = new DeviceManager(settings, adbService, logger);
        workerClient = new WorkerClient(adbService, logger);
        workerMonitor = new WorkerMonitor(deviceManager, workerClient);
        previewGenerator = new PreviewGenerator(logger);
        rulesEngine = new RulesEngine();
        fileMover = new FileMover(logger);
        projectManager = new ProjectManager();

        ioExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "rigsort-io");
            t.setDaemon(true);
            return t;
        });

        dispatcher = new Dispatcher(deviceManager, projectManager, previewGenerator, rulesEngine, fileMover, workerClient, logger);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));
        root.getStyleClass().add("root");

        VBox ribbon = buildRibbon(stage);
        root.setTop(ribbon);

        HBox main = new HBox(10);
        Node nav = buildNav();
        StackPane center = new StackPane();
        Node monitorPage = buildMonitorPage();
        Node sorterPage = buildSorterPage();
        Node rulesPage = buildRulesPage();
        Node reviewPage = buildReviewPage();
        Node auditPage = buildAuditPage();

        center.getChildren().addAll(monitorPage, sorterPage, rulesPage, reviewPage, auditPage);
        monitorPage.setVisible(true);
        sorterPage.setVisible(false);
        rulesPage.setVisible(false);
        reviewPage.setVisible(false);
        auditPage.setVisible(false);

        VBox inspector = buildInspector();
        main.getChildren().addAll(nav, center, inspector);
        HBox.setHgrow(center, Priority.ALWAYS);
        root.setCenter(main);

        VBox bottom = buildBottom();
        root.setBottom(bottom);

        registerNavHandlers(nav, monitorPage, sorterPage, rulesPage, reviewPage, auditPage);

        Scene scene = new Scene(root, settings.getWindowWidth(), settings.getWindowHeight());
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setTitle("RigSort v0.2");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            event.consume();
            shutdown();
        });
        stage.widthProperty().addListener((obs, oldVal, newVal) -> settings.setWindowWidth(newVal.doubleValue()));
        stage.heightProperty().addListener((obs, oldVal, newVal) -> settings.setWindowHeight(newVal.doubleValue()));

        deviceManager.start();
        workerMonitor.start();

        Timeline refresh = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshUI()));
        refresh.setCycleCount(Timeline.INDEFINITE);
        refresh.play();

        loadLastProject();

        stage.show();
    }

    private VBox buildRibbon(Stage stage) {
        VBox ribbon = new VBox(6);
        ribbon.getStyleClass().add("ribbon");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox projectGroup = ribbonGroup("Project",
                actionButton("New", e -> newProject(stage)),
                actionButton("Open", e -> openProject(stage)),
                actionButton("Save", e -> saveProject())
        );
        VBox scanGroup = ribbonGroup("Scan",
                actionButton("Scan", e -> scanSource()),
                actionButton("Start", e -> startProcessing()),
                actionButton("Stop", e -> stopProcessing())
        );
        VBox rulesGroup = ribbonGroup("Rules",
                actionButton("Validate", e -> validateRules()),
                actionButton("Refresh", e -> reloadRules())
        );
        VBox moveGroup = ribbonGroup("Move",
                actionButton("Undo", e -> undoMoves())
        );
        VBox settingsGroup = ribbonGroup("Settings",
                actionButton("ADB Settings", e -> SettingsDialog.show(stage, settings, deviceManager))
        );

        row.getChildren().addAll(projectGroup, scanGroup, rulesGroup, moveGroup, settingsGroup);
        ribbon.getChildren().add(row);
        return ribbon;
    }

    private Node buildNav() {
        VBox nav = new VBox(6);
        nav.getStyleClass().add("nav");
        ToggleGroup group = new ToggleGroup();
        ToggleButton monitor = navButton("Monitor", group);
        ToggleButton sorter = navButton("Sorter", group);
        ToggleButton rules = navButton("Rules", group);
        ToggleButton review = navButton("Review", group);
        ToggleButton audit = navButton("Audit", group);
        monitor.setSelected(true);
        nav.getChildren().addAll(monitor, sorter, rules, review, audit);
        nav.setUserData(group);
        return nav;
    }

    private Node buildMonitorPage() {
        TableView<DeviceInfo> table = new TableView<>(deviceManager.getDevices());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<DeviceInfo, String> serialCol = new TableColumn<>("Serial");
        serialCol.setCellValueFactory(data -> data.getValue().serialProperty());
        TableColumn<DeviceInfo, String> stateCol = new TableColumn<>("ADB State");
        stateCol.setCellValueFactory(data -> data.getValue().adbStateProperty());
        TableColumn<DeviceInfo, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(data -> data.getValue().modelProperty());
        TableColumn<DeviceInfo, String> androidCol = new TableColumn<>("Android");
        androidCol.setCellValueFactory(data -> data.getValue().androidVersionProperty());
        TableColumn<DeviceInfo, String> pingCol = new TableColumn<>("Ping");
        pingCol.setCellValueFactory(data -> data.getValue().pingStatusProperty());
        TableColumn<DeviceInfo, String> lastSeenCol = new TableColumn<>("Last Seen");
        lastSeenCol.setCellValueFactory(data -> data.getValue().lastSeenProperty());
        TableColumn<DeviceInfo, String> workerCol = new TableColumn<>("Worker");
        workerCol.setCellValueFactory(data -> data.getValue().workerStatusProperty());
        TableColumn<DeviceInfo, String> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(data -> data.getValue().forwardedPortProperty().asString());
        TableColumn<DeviceInfo, String> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(data -> data.getValue().workerVersionProperty());
        TableColumn<DeviceInfo, String> queueCol = new TableColumn<>("Queue");
        queueCol.setCellValueFactory(data -> data.getValue().workerQueueDepthProperty().asString());
        TableColumn<DeviceInfo, String> uptimeCol = new TableColumn<>("Uptime");
        uptimeCol.setCellValueFactory(data -> data.getValue().workerUptimeProperty());

        table.getColumns().addAll(serialCol, stateCol, modelCol, androidCol, pingCol, lastSeenCol, workerCol, portCol, versionCol, queueCol, uptimeCol);

        HBox controls = new HBox(8,
                actionButton("Ping All", e -> deviceManager.pingAll()),
                actionButton("Restart ADB", e -> deviceManager.restartAdb()),
                actionButton("Install Worker", e -> installWorkerApk()),
                actionButton("Start Worker", e -> startWorkers()),
                actionButton("Stop Worker", e -> stopWorkers()),
                actionButton("Check Workers", e -> workerMonitor.refreshAll()),
                actionButton("Forward Ports", e -> forwardPorts())
        );
        VBox box = new VBox(10, controls, table);
        box.getStyleClass().add("page");
        return box;
    }

    private Node buildSorterPage() {
        sorterTable = new TableView<>();
        sorterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        sorterTable.getColumns().addAll(
                textColumn("Status", r -> r.getStatus().name()),
                textColumn("FileId", FileRecord::shortId),
                textColumn("Path", FileRecord::getSourcePath),
                textColumn("Type", r -> r.getFileType().name()),
                textColumn("Size", r -> formatSize(r.getSizeBytes())),
                textColumn("Modified", r -> formatTime(r.getModifiedTime())),
                textColumn("Preview", r -> r.getPreviewStatus().name()),
                textColumn("Label", FileRecord::getLabel),
                textColumn("Confidence", r -> formatDouble(r.getConfidence())),
                textColumn("Faces", r -> String.valueOf(r.getFacesCount())),
                textColumn("HasText", r -> formatDouble(r.getHasTextLikelihood())),
                textColumn("Rule", FileRecord::getRuleName),
                textColumn("Destination", FileRecord::getDestinationPath),
                textColumn("Move", FileRecord::getMoveStatus)
        );

        sorterFiltered = new FilteredList<>(fileRecords, r -> true);
        sorterTable.setItems(sorterFiltered);

        filterBox = new ComboBox<>();
        filterBox.getItems().addAll("ALL", "NEW", "QUEUED", "INFERRED", "REVIEW", "MOVED", "ERROR");
        filterBox.getSelectionModel().select("ALL");
        filterBox.valueProperty().addListener((obs, oldVal, newVal) -> sorterFiltered.setPredicate(r -> filterRecord(r, newVal)));

        sourceField = new TextField();
        sourceField.setPromptText("Source Root");
        Button sourceBrowse = actionButton("Browse", e -> chooseSource());
        destField = new TextField();
        destField.setPromptText("Destination Root");
        Button destBrowse = actionButton("Browse", e -> chooseDestination());
        sourceField.setOnAction(e -> updateSourceFromField());
        destField.setOnAction(e -> updateDestinationFromField());

        HBox pathRow = new HBox(8, new Label("Source"), sourceField, sourceBrowse,
                new Label("Destination"), destField, destBrowse);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        batchField = new TextField();
        batchField.setPrefWidth(60);
        inFlightField = new TextField();
        inFlightField.setPrefWidth(80);
        Button applyLimits = actionButton("Apply", e -> applyLimits());

        HBox controls = new HBox(8,
                new Label("Filter"), filterBox,
                new Label("Batch"), batchField,
                new Label("Max In-Flight"), inFlightField,
                applyLimits,
                actionButton("Scan", e -> scanSource()),
                actionButton("Start", e -> startProcessing()),
                actionButton("Stop", e -> stopProcessing()),
                actionButton("Preview Settings", e -> showPreviewSettings())
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, pathRow, controls, sorterTable);
        box.getStyleClass().add("page");

        sorterTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateInspector(newVal));

        return box;
    }

    private Node buildRulesPage() {
        TableView<Rule> table = new TableView<>(ruleRecords);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Rule, Boolean> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(data -> {
            javafx.beans.property.SimpleBooleanProperty prop = new javafx.beans.property.SimpleBooleanProperty(data.getValue().isEnabled());
            prop.addListener((obs, oldVal, newVal) -> {
                data.getValue().setEnabled(newVal);
                persistRules();
            });
            return prop;
        });
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
        enabledCol.setEditable(true);
        enabledCol.setPrefWidth(80);

        TableColumn<Rule, String> condCol = new TableColumn<>("Condition");
        condCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCondition()));
        condCol.setCellFactory(TextFieldTableCell.forTableColumn());
        condCol.setOnEditCommit(e -> {
            String value = e.getNewValue();
            String err = rulesEngine.validateCondition(value);
            if (err != null) {
                showAlert("Rule error", err);
                return;
            }
            e.getRowValue().setCondition(value);
            persistRules();
        });

        TableColumn<Rule, String> destCol = new TableColumn<>("Destination");
        destCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDestination()));
        destCol.setCellFactory(TextFieldTableCell.forTableColumn());
        destCol.setOnEditCommit(e -> {
            e.getRowValue().setDestination(e.getNewValue());
            persistRules();
        });

        TableColumn<Rule, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getNotes()));
        notesCol.setCellFactory(TextFieldTableCell.forTableColumn());
        notesCol.setOnEditCommit(e -> {
            e.getRowValue().setNotes(e.getNewValue());
            persistRules();
        });

        table.getColumns().addAll(enabledCol, condCol, destCol, notesCol);

        Button add = actionButton("Add Rule", e -> {
            Rule rule = new Rule("rule-" + (ruleRecords.size() + 1), true, "label == UNKNOWN", "Review/Unsorted", "new rule");
            ruleRecords.add(rule);
            persistRules();
        });
        Button remove = actionButton("Remove", e -> {
            Rule selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ruleRecords.remove(selected);
                persistRules();
            }
        });

        HBox controls = new HBox(8, add, remove);
        VBox box = new VBox(10, controls, table);
        box.getStyleClass().add("page");
        return box;
    }

    private Node buildReviewPage() {
        reviewTable = new TableView<>();
        reviewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        reviewTable.getColumns().addAll(
                textColumn("Status", r -> r.getStatus().name()),
                textColumn("FileId", FileRecord::shortId),
                textColumn("Path", FileRecord::getSourcePath),
                textColumn("Label", FileRecord::getLabel),
                textColumn("Confidence", r -> formatDouble(r.getConfidence()))
        );
        reviewFiltered = new FilteredList<>(fileRecords, r -> r.getStatus() == FileStatus.REVIEW);
        reviewTable.setItems(reviewFiltered);
        reviewTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateInspector(newVal));

        Button movePeople = actionButton("Family/People", e -> reviewMoveSelected("Private/Family/People"));
        Button movePhotos = actionButton("Photos/Other", e -> reviewMoveSelected("Private/Photos/Other"));
        Button moveInvoices = actionButton("Work/Invoices", e -> reviewMoveSelected("Work/Invoices"));
        Button moveDocs = actionButton("Work/Documents", e -> reviewMoveSelected("Work/Documents"));
        Button moveScreens = actionButton("Work/Screenshots", e -> reviewMoveSelected("Work/Screenshots"));
        Button custom = actionButton("Custom...", e -> reviewMoveCustom());
        Button requeue = actionButton("Re-run", e -> reviewRequeue());
        Button ignore = actionButton("Ignore", e -> reviewIgnore());

        HBox actions = new HBox(8, movePeople, movePhotos, moveInvoices, moveDocs, moveScreens, custom, requeue, ignore);
        VBox box = new VBox(10, actions, reviewTable);
        box.getStyleClass().add("page");
        return box;
    }

    private Node buildAuditPage() {
        auditFiltered = new FilteredList<>(auditRecords, a -> true);
        auditTable = new TableView<>(auditFiltered);
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        auditTable.getColumns().addAll(
                textColumn("Time", a -> formatTime(a.getTimestamp())),
                textColumn("FileId", AuditRecord::getFileId),
                textColumn("From", AuditRecord::getFromPath),
                textColumn("To", AuditRecord::getToPath),
                textColumn("Rule", AuditRecord::getRuleName),
                textColumn("Label", AuditRecord::getLabel)
        );
        auditSearchField = new TextField();
        auditSearchField.setPromptText("Search audit...");
        auditSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.toLowerCase();
            auditFiltered.setPredicate(a -> matchesAudit(a, query));
        });
        TextField undoCount = new TextField("5");
        undoCount.setPrefWidth(60);
        Button undo = actionButton("Undo", e -> {
            try {
                int count = Integer.parseInt(undoCount.getText().trim());
                undoMoves(count);
            } catch (Exception ex) {
                showAlert("Undo error", ex.getMessage());
            }
        });
        VBox box = new VBox(10, new HBox(8, auditSearchField, new Label("Undo N"), undoCount, undo), auditTable);
        box.getStyleClass().add("page");
        return box;
    }

    private VBox buildInspector() {
        VBox inspector = new VBox(8);
        inspector.getStyleClass().add("inspector");
        Label title = new Label("Inspector");
        title.getStyleClass().add("inspector-title");

        inspectorImage = new ImageView();
        inspectorImage.setFitWidth(240);
        inspectorImage.setFitHeight(240);
        inspectorImage.setPreserveRatio(true);
        inspectorImage.getStyleClass().add("inspector-image");

        inspectorPath = new Label("-");
        inspectorMeta = new Label("-");
        inspectorLabel = new Label("-");
        inspectorDestination = new Label("-");
        inspectorRule = new Label("-");

        VBox meta = new VBox(4,
                labelled("Path", inspectorPath),
                labelled("Meta", inspectorMeta),
                labelled("Label", inspectorLabel),
                labelled("Rule", inspectorRule),
                labelled("Destination", inspectorDestination)
        );

        inspector.getChildren().addAll(title, inspectorImage, meta);
        return inspector;
    }

    private VBox buildBottom() {
        VBox bottom = new VBox(6);
        bottom.getStyleClass().add("bottom");

        statusCounts = new Label("No project loaded");
        queueDepthLabel = new Label("Queue: 0");
        deviceCountLabel = new Label("Devices: 0");
        HBox statusRow = new HBox(12, statusCounts, queueDepthLabel, deviceCountLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getStyleClass().add("status-row");

        Button toggleLogs = actionButton("Logs", e -> toggleLogs());
        HBox statusBar = new HBox(12, statusRow, toggleLogs);
        statusBar.setAlignment(Pos.CENTER_LEFT);

        logView = new ListView<>(logService.getLogLines());
        logView.setVisible(false);
        logView.setManaged(false);
        logView.setPrefHeight(140);

        bottom.getChildren().addAll(statusBar, logView);
        return bottom;
    }

    private void registerNavHandlers(Node navNode, Node monitor, Node sorter, Node rules, Node review, Node audit) {
        ToggleGroup group = (ToggleGroup) navNode.getUserData();
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            ToggleButton button = (ToggleButton) newVal;
            String text = button.getText();
            monitor.setVisible("Monitor".equals(text));
            sorter.setVisible("Sorter".equals(text));
            rules.setVisible("Rules".equals(text));
            review.setVisible("Review".equals(text));
            audit.setVisible("Audit".equals(text));
        });
    }

    private void refreshUI() {
        if (projectManager.getConfig() != null) {
            List<FileRecord> updated = projectManager.getRecords().stream()
                    .sorted(Comparator.comparing(FileRecord::getSourcePath))
                    .toList();
            fileRecords.setAll(updated);
            auditRecords.setAll(projectManager.getAuditRecords());
            if (reviewFiltered != null) {
                reviewFiltered.setPredicate(r -> r.getStatus() == FileStatus.REVIEW);
            }
            if (sorterFiltered != null && filterBox != null) {
                sorterFiltered.setPredicate(r -> filterRecord(r, filterBox.getValue()));
            }
            statusCounts.setText(buildCounts());
            queueDepthLabel.setText("Queue: " + dispatcher.getQueueSize());
        }
        deviceCountLabel.setText("Devices: " + deviceManager.getDevices().size());
        sorterTable.refresh();
        if (reviewTable != null) {
            reviewTable.refresh();
        }
        if (auditTable != null) {
            auditTable.refresh();
        }
    }

    private String buildCounts() {
        long total = fileRecords.size();
        long review = fileRecords.stream().filter(r -> r.getStatus() == FileStatus.REVIEW).count();
        long moved = fileRecords.stream().filter(r -> r.getStatus() == FileStatus.MOVED).count();
        long errors = fileRecords.stream().filter(r -> r.getStatus() == FileStatus.ERROR).count();
        return "Files: " + total + " | Review: " + review + " | Moved: " + moved + " | Errors: " + errors;
    }

    private void loadLastProject() {
        String last = settings.getLastProjectPath();
        if (last == null || last.isBlank()) {
            return;
        }
        try {
            projectManager.loadProject(Path.of(last));
            loadProjectIntoUI();
        } catch (Exception ex) {
            logService.getLogger().warning("Failed to load last project: " + ex.getMessage());
        }
    }

    private void newProject(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Create RigSort Project Folder");
        File dir = chooser.showDialog(stage);
        if (dir == null) {
            return;
        }
        ProjectConfig config = new ProjectConfig();
        config.setRules(ProjectManager.defaultRules());
        config.setSourceRoot(pickFolder("Select Source Root"));
        config.setDestinationRoot(pickFolder("Select Destination Root"));
        try {
            projectManager.createProject(dir.toPath(), config);
            settings.setLastProjectPath(dir.toPath().toString());
            loadProjectIntoUI();
        } catch (Exception ex) {
            showAlert("Project error", ex.getMessage());
        }
    }

    private void openProject(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open RigSort Project Folder");
        File dir = chooser.showDialog(stage);
        if (dir == null) {
            return;
        }
        try {
            projectManager.loadProject(dir.toPath());
            settings.setLastProjectPath(dir.toPath().toString());
            loadProjectIntoUI();
        } catch (Exception ex) {
            showAlert("Project error", ex.getMessage());
        }
    }

    private void saveProject() {
        try {
            if (projectManager.getConfig() != null) {
                projectManager.saveConfig();
                showAlert("Saved", "Project saved.");
            }
        } catch (Exception ex) {
            showAlert("Save error", ex.getMessage());
        }
    }

    private void loadProjectIntoUI() {
        ProjectConfig config = projectManager.getConfig();
        if (config == null) {
            return;
        }
        sourceField.setText(config.getSourceRoot());
        destField.setText(config.getDestinationRoot());
        if (batchField != null) {
            batchField.setText(String.valueOf(config.getBatchSize()));
        }
        if (inFlightField != null) {
            inFlightField.setText(String.valueOf(config.getMaxInFlight()));
        }
        fileRecords.setAll(projectManager.getRecords().stream()
                .sorted(Comparator.comparing(FileRecord::getSourcePath))
                .toList());
        ruleRecords.setAll(config.getRules());
        auditRecords.setAll(projectManager.getAuditRecords());
        rulesEngine.setRules(config.getRules());
        dispatcher.refreshQueue();
    }

    private void scanSource() {
        if (projectManager.getConfig() == null) {
            showAlert("No project", "Create or open a project first.");
            return;
        }
        ProjectConfig config = projectManager.getConfig();
        if (config.getSourceRoot() == null || config.getSourceRoot().isBlank()) {
            showAlert("Source missing", "Set Source Root.");
            return;
        }
        ioExecutor.submit(() -> {
            FileScanner scanner = new FileScanner();
            try {
                scanner.scan(Path.of(config.getSourceRoot()), config.isStrictHash(), record -> {
                    try {
                        FileRecord existing = projectManager.getRecord(record.getFileId());
                        if (existing != null) {
                            return;
                        }
                        projectManager.upsertRecord(record);
                    } catch (Exception ex) {
                        logService.getLogger().warning("Scan persist failed: " + ex.getMessage());
                    }
                });
                Platform.runLater(() -> {
                    fileRecords.setAll(projectManager.getRecords());
                    dispatcher.refreshQueue();
                });
            } catch (Exception ex) {
                logService.getLogger().warning("Scan failed: " + ex.getMessage());
            }
        });
    }

    private void startProcessing() {
        if (projectManager.getConfig() == null) {
            showAlert("No project", "Create or open a project first.");
            return;
        }
        dispatcher.start();
        logService.getLogger().info("Processing started");
    }

    private void stopProcessing() {
        dispatcher.stop();
        logService.getLogger().info("Processing stopped");
    }

    private void validateRules() {
        for (Rule rule : ruleRecords) {
            String err = rulesEngine.validateCondition(rule.getCondition());
            if (err != null) {
                showAlert("Rule error", "Rule " + rule.getId() + ": " + err);
                return;
            }
        }
        showAlert("Rules", "All rules look valid.");
    }

    private void applyLimits() {
        if (projectManager.getConfig() == null) {
            return;
        }
        try {
            int batch = Integer.parseInt(batchField.getText().trim());
            int inFlight = Integer.parseInt(inFlightField.getText().trim());
            projectManager.getConfig().setBatchSize(Math.max(1, batch));
            projectManager.getConfig().setMaxInFlight(Math.max(1, inFlight));
            projectManager.saveConfig();
        } catch (Exception ex) {
            showAlert("Limits error", ex.getMessage());
        }
    }

    private void forwardPorts() {
        for (DeviceInfo device : deviceManager.getDevices()) {
            if (!"device".equalsIgnoreCase(device.getAdbState())) {
                continue;
            }
            workerClient.checkHealth(device);
        }
    }

    private void installWorkerApk() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select RigSort Worker APK");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("APK", "*.apk"));
        File apk = chooser.showOpenDialog(primaryStage);
        if (apk == null) {
            return;
        }
        ioExecutor.submit(() -> {
            for (DeviceInfo device : deviceManager.getDevices()) {
                if (!"device".equalsIgnoreCase(device.getAdbState())) {
                    continue;
                }
                ADBService.ExecResult res = adbService.runAdb(List.of(
                        "-s", device.getSerial(), "install", "-r", apk.getAbsolutePath()
                ), Duration.ofSeconds(120));
                if (res.timedOut || res.exitCode != 0) {
                    logService.getLogger().warning("Install failed on " + device.getSerial() + ": " + res.stderr);
                } else {
                    logService.getLogger().info("Worker installed on " + device.getSerial());
                    startWorkerService(device.getSerial());
                }
            }
        });
    }

    private void startWorkers() {
        ioExecutor.submit(() -> {
            for (DeviceInfo device : deviceManager.getDevices()) {
                if (!"device".equalsIgnoreCase(device.getAdbState())) {
                    continue;
                }
                startWorkerService(device.getSerial());
            }
        });
    }

    private void stopWorkers() {
        ioExecutor.submit(() -> {
            for (DeviceInfo device : deviceManager.getDevices()) {
                if (!"device".equalsIgnoreCase(device.getAdbState())) {
                    continue;
                }
                adbService.runAdb(List.of("-s", device.getSerial(), "shell", "am", "stopservice",
                        "-n", "com.rigsort.worker/.WorkerService"), Duration.ofSeconds(10));
            }
        });
    }

    private void startWorkerService(String serial) {
        ADBService.ExecResult res = adbService.runAdb(List.of("-s", serial, "shell", "am", "start-foreground-service",
                "-n", "com.rigsort.worker/.WorkerService"), Duration.ofSeconds(10));
        if (res.exitCode != 0) {
            adbService.runAdb(List.of("-s", serial, "shell", "am", "startservice",
                    "-n", "com.rigsort.worker/.WorkerService"), Duration.ofSeconds(10));
        }
    }

    private void reloadRules() {
        if (projectManager.getConfig() == null) {
            return;
        }
        ruleRecords.setAll(projectManager.getConfig().getRules());
        rulesEngine.setRules(ruleRecords);
    }

    private void persistRules() {
        if (projectManager.getConfig() == null) {
            return;
        }
        projectManager.getConfig().setRules(ruleRecords);
        try {
            projectManager.saveConfig();
        } catch (Exception ex) {
            showAlert("Save error", ex.getMessage());
        }
    }

    private void reviewMoveSelected(String destination) {
        FileRecord record = reviewTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            return;
        }
        if (projectManager.getConfig() == null) {
            return;
        }
        FileMover.MoveResult result = fileMover.move(record, projectManager.getConfig(), destination);
        if (result.isOk()) {
            String originalPath = record.getSourcePath();
            record.setDestinationPath(result.getDestination());
            record.setRuleName("Manual");
            record.setStatus(FileStatus.MOVED);
            record.setMoveStatus("MOVED");
            writeAudit(record, result.getDestination(), originalPath);
            record.setSourcePath(result.getDestination());
            if (!projectManager.getConfig().getPreviewPolicy().isKeepPreviews() && record.getPreviewPath() != null) {
                try {
                    Files.deleteIfExists(Path.of(record.getPreviewPath()));
                } catch (Exception ignored) {
                }
            }
            try {
                projectManager.upsertRecord(record);
            } catch (Exception ex) {
                logService.getLogger().warning("Review move persist failed: " + ex.getMessage());
            }
        } else {
            record.setStatus(FileStatus.ERROR);
            record.setError(result.getError());
        }
    }

    private void reviewMoveCustom() {
        FileRecord record = reviewTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog("Review/Custom");
        dialog.setTitle("Custom Destination");
        dialog.setHeaderText("Enter a destination relative to Destination Root");
        dialog.setContentText("Destination:");
        dialog.showAndWait().ifPresent(dest -> reviewMoveSelected(dest));
    }

    private void reviewRequeue() {
        FileRecord record = reviewTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            return;
        }
        record.setStatus(FileStatus.NEW);
        try {
            projectManager.upsertRecord(record);
        } catch (Exception ex) {
            logService.getLogger().warning("Requeue failed: " + ex.getMessage());
        }
    }

    private void reviewIgnore() {
        FileRecord record = reviewTable.getSelectionModel().getSelectedItem();
        if (record == null) {
            return;
        }
        record.setStatus(FileStatus.IGNORED);
        try {
            projectManager.upsertRecord(record);
        } catch (Exception ex) {
            logService.getLogger().warning("Ignore failed: " + ex.getMessage());
        }
    }

    private void showPreviewSettings() {
        if (projectManager.getConfig() == null) {
            showAlert("No project", "Create or open a project first.");
            return;
        }
        PreviewPolicy policy = projectManager.getConfig().getPreviewPolicy();
        ProjectConfig config = projectManager.getConfig();

        TextField maxEdge = new TextField(String.valueOf(policy.getMaxLongEdgePx()));
        TextField quality = new TextField(String.valueOf(policy.getQuality()));
        ComboBox<String> format = new ComboBox<>();
        format.getItems().addAll("WEBP", "JPEG");
        format.getSelectionModel().select(policy.getFormat().toUpperCase());
        CheckBox keep = new CheckBox("Keep previews");
        keep.setSelected(policy.isKeepPreviews());
        TextField threshold = new TextField(String.valueOf(config.getConfidenceThreshold()));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Max Long Edge"), maxEdge);
        grid.addRow(1, new Label("Quality"), quality);
        grid.addRow(2, new Label("Format"), format);
        grid.addRow(3, new Label("Confidence Threshold"), threshold);
        grid.addRow(4, keep);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Preview Settings");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    policy.setMaxLongEdgePx(Integer.parseInt(maxEdge.getText().trim()));
                    policy.setQuality(Integer.parseInt(quality.getText().trim()));
                    policy.setFormat(format.getSelectionModel().getSelectedItem());
                    policy.setKeepPreviews(keep.isSelected());
                    config.setPreviewPolicy(policy);
                    config.setConfidenceThreshold(Double.parseDouble(threshold.getText().trim()));
                    projectManager.saveConfig();
                } catch (Exception ex) {
                    showAlert("Preview settings error", ex.getMessage());
                }
            }
        });
    }

    private void undoMoves() {
        undoMoves(5);
    }

    private void undoMoves(int count) {
        List<AuditRecord> audit = projectManager.getAuditRecords();
        if (audit.isEmpty()) {
            return;
        }
        int fromIndex = Math.max(0, audit.size() - count);
        List<AuditRecord> subset = audit.subList(fromIndex, audit.size());
        List<AuditRecord> reversed = new java.util.ArrayList<>(subset);
        java.util.Collections.reverse(reversed);
        fileMover.undo(reversed);
    }

    private void chooseSource() {
        String path = pickFolder("Select Source Root");
        if (path == null) {
            return;
        }
        sourceField.setText(path);
        updateSourceFromField();
    }

    private void chooseDestination() {
        String path = pickFolder("Select Destination Root");
        if (path == null) {
            return;
        }
        destField.setText(path);
        updateDestinationFromField();
    }

    private void updateSourceFromField() {
        if (projectManager.getConfig() == null) {
            return;
        }
        projectManager.getConfig().setSourceRoot(sourceField.getText().trim());
        try {
            projectManager.saveConfig();
        } catch (Exception ex) {
            showAlert("Save error", ex.getMessage());
        }
    }

    private void updateDestinationFromField() {
        if (projectManager.getConfig() == null) {
            return;
        }
        projectManager.getConfig().setDestinationRoot(destField.getText().trim());
        try {
            projectManager.saveConfig();
        } catch (Exception ex) {
            showAlert("Save error", ex.getMessage());
        }
    }

    private String pickFolder(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File dir = chooser.showDialog(primaryStage);
        return dir == null ? null : dir.getAbsolutePath();
    }

    private void updateInspector(FileRecord record) {
        if (record == null) {
            inspectorPath.setText("-");
            inspectorMeta.setText("-");
            inspectorLabel.setText("-");
            inspectorRule.setText("-");
            inspectorDestination.setText("-");
            inspectorImage.setImage(null);
            return;
        }
        inspectorPath.setText(record.getSourcePath());
        String exif = record.getExifDateTime() == null ? "" : (" | " + record.getExifDateTime());
        inspectorMeta.setText(record.getFileType() + " | " + formatSize(record.getSizeBytes()) + exif);
        String label = Optional.ofNullable(record.getLabel()).orElse("-");
        inspectorLabel.setText(label + " (" + formatDouble(record.getConfidence()) + ")");
        inspectorRule.setText(Optional.ofNullable(record.getRuleName()).orElse("-"));
        inspectorDestination.setText(Optional.ofNullable(record.getDestinationPath()).orElse("-"));
        if (record.getPreviewPath() != null && Files.exists(Path.of(record.getPreviewPath()))) {
            inspectorImage.setImage(new Image(Path.of(record.getPreviewPath()).toUri().toString(), 240, 240, true, true));
        } else {
            inspectorImage.setImage(null);
        }
    }

    private void writeAudit(FileRecord record, String destination, String originalPath) {
        AuditRecord audit = new AuditRecord();
        audit.setFileId(record.getFileId());
        audit.setFromPath(originalPath);
        audit.setToPath(destination);
        audit.setTimestamp(System.currentTimeMillis());
        audit.setRuleId(record.getRuleId());
        audit.setRuleName(record.getRuleName());
        audit.setLabel(record.getLabel());
        audit.setConfidence(record.getConfidence());
        audit.setStatus("MOVED");
        try {
            projectManager.appendAudit(audit);
            auditRecords.setAll(projectManager.getAuditRecords());
        } catch (Exception ex) {
            logService.getLogger().warning("Audit failed: " + ex.getMessage());
        }
    }

    private boolean filterRecord(FileRecord record, String filter) {
        if (filter == null || "ALL".equalsIgnoreCase(filter)) {
            return true;
        }
        return record.getStatus().name().equalsIgnoreCase(filter);
    }

    private boolean matchesAudit(AuditRecord record, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase();
        return (record.getFileId() != null && record.getFileId().toLowerCase().contains(q))
                || (record.getFromPath() != null && record.getFromPath().toLowerCase().contains(q))
                || (record.getToPath() != null && record.getToPath().toLowerCase().contains(q))
                || (record.getRuleName() != null && record.getRuleName().toLowerCase().contains(q))
                || (record.getLabel() != null && record.getLabel().toLowerCase().contains(q));
    }

    private void toggleLogs() {
        boolean show = !logView.isVisible();
        logView.setVisible(show);
        logView.setManaged(show);
    }

    private VBox ribbonGroup(String title, Button... buttons) {
        VBox group = new VBox(6);
        group.getStyleClass().add("ribbon-group");
        for (Button b : buttons) {
            group.getChildren().add(b);
        }
        Label label = new Label(title);
        label.getStyleClass().add("ribbon-title");
        group.getChildren().add(label);
        return group;
    }

    private Button actionButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setOnAction(handler);
        button.getStyleClass().add("ribbon-button");
        return button;
    }

    private ToggleButton navButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private <T> TableColumn<T, String> textColumn(String title, java.util.function.Function<T, String> mapper) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        return column;
    }

    private HBox labelled(String label, Label value) {
        Label l = new Label(label + ":");
        l.getStyleClass().add("inspector-label");
        value.getStyleClass().add("inspector-value");
        HBox box = new HBox(6, l, value);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    private String formatTime(long epoch) {
        if (epoch <= 0) {
            return "-";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(epoch));
    }

    private String formatDouble(double value) {
        if (Double.isNaN(value)) {
            return "-";
        }
        return new DecimalFormat("0.00").format(value);
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void shutdown() {
        stopProcessing();
        workerMonitor.stop();
        deviceManager.shutdown();
        dispatcher.shutdown();
        ioExecutor.shutdownNow();
        logService.shutdown();
        Platform.exit();
    }

    @Override
    public void stop() {
        shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
