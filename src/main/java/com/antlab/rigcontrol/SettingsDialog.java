package com.antlab.rigcontrol;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsDialog {
    public static void show(Stage owner, Settings settings, DeviceManager deviceManager) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Settings");

        TextField adbPathField = new TextField(settings.getAdbPath());
        TextField pollField = new TextField(String.valueOf(settings.getPollIntervalSeconds()));
        TextField pingField = new TextField(String.valueOf(settings.getPingIntervalSeconds()));
        TextField timeoutField = new TextField(String.valueOf(settings.getAdbTimeoutSeconds()));
        TextField pingConcurrencyField = new TextField(String.valueOf(settings.getPingMaxConcurrency()));
        TextField pingBackoffField = new TextField(String.valueOf(settings.getPingFailureBackoffSeconds()));
        TextField backoffStepField = new TextField(String.valueOf(settings.getPingBackoffStepSeconds()));
        TextField backoffMaxField = new TextField(String.valueOf(settings.getPingBackoffMaxSeconds()));
        TextField rateLimitField = new TextField(String.valueOf(settings.getAdbRateLimitPerInterval()));
        TextField rateIntervalField = new TextField(String.valueOf(settings.getAdbRateIntervalSeconds()));
        CheckBox simulationCheck = new CheckBox("Use simulation file");
        simulationCheck.setSelected(settings.isSimulationEnabled());
        TextField simulationFileField = new TextField(settings.getSimulationFile());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.addRow(0, new Label("ADB path"), adbPathField);
        grid.addRow(1, new Label("Discovery interval (s)"), pollField);
        grid.addRow(2, new Label("Ping interval (s)"), pingField);
        grid.addRow(3, new Label("ADB timeout (s)"), timeoutField);
        grid.addRow(4, new Label("Ping concurrency"), pingConcurrencyField);
        grid.addRow(5, new Label("Ping failure backoff (s)"), pingBackoffField);
        grid.addRow(6, new Label("Backoff step (s)"), backoffStepField);
        grid.addRow(7, new Label("Backoff max (s)"), backoffMaxField);
        grid.addRow(8, new Label("ADB rate limit (per interval)"), rateLimitField);
        grid.addRow(9, new Label("ADB rate interval (s)"), rateIntervalField);
        grid.addRow(10, simulationCheck, simulationFileField);

        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        HBox buttons = new HBox(8, saveButton, cancelButton);

        VBox root = new VBox(12, grid, buttons);
        root.setPadding(new Insets(12));

        saveButton.setOnAction(e -> {
            settings.setAdbPath(adbPathField.getText());
            settings.setPollIntervalSeconds(parseInt(pollField.getText(), settings.getPollIntervalSeconds()));
            settings.setPingIntervalSeconds(parseInt(pingField.getText(), settings.getPingIntervalSeconds()));
            settings.setAdbTimeoutSeconds(parseInt(timeoutField.getText(), settings.getAdbTimeoutSeconds()));
            settings.setPingMaxConcurrency(parseInt(pingConcurrencyField.getText(), settings.getPingMaxConcurrency()));
            settings.setPingFailureBackoffSeconds(parseInt(pingBackoffField.getText(), settings.getPingFailureBackoffSeconds()));
            settings.setPingBackoffStepSeconds(parseInt(backoffStepField.getText(), settings.getPingBackoffStepSeconds()));
            settings.setPingBackoffMaxSeconds(parseInt(backoffMaxField.getText(), settings.getPingBackoffMaxSeconds()));
            settings.setAdbRateLimitPerInterval(parseInt(rateLimitField.getText(), settings.getAdbRateLimitPerInterval()));
            settings.setAdbRateIntervalSeconds(parseInt(rateIntervalField.getText(), settings.getAdbRateIntervalSeconds()));
            settings.setSimulationEnabled(simulationCheck.isSelected());
            settings.setSimulationFile(simulationFileField.getText());
            deviceManager.applySettings();
            dialog.close();
        });

        cancelButton.setOnAction(e -> dialog.close());

        Scene scene = new Scene(root, 640, 420);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
