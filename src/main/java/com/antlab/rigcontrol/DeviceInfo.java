package com.antlab.rigcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.IntegerProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DeviceInfo {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final StringProperty serial = new SimpleStringProperty("");
    private final StringProperty adbState = new SimpleStringProperty("unknown");
    private final StringProperty model = new SimpleStringProperty("");
    private final StringProperty androidVersion = new SimpleStringProperty("");
    private final StringProperty lastSeen = new SimpleStringProperty("");
    private final StringProperty pingStatus = new SimpleStringProperty("-");
    private final StringProperty lastPing = new SimpleStringProperty("");
    private final StringProperty tag = new SimpleStringProperty("");
    private final StringProperty expected = new SimpleStringProperty("no");
    private final StringProperty workerStatus = new SimpleStringProperty("-");
    private final StringProperty workerVersion = new SimpleStringProperty("-");
    private final StringProperty workerError = new SimpleStringProperty("");
    private final IntegerProperty workerQueueDepth = new SimpleIntegerProperty(0);
    private final IntegerProperty forwardedPort = new SimpleIntegerProperty(0);
    private final StringProperty workerUptime = new SimpleStringProperty("-");

    public DeviceInfo(String serial) {
        this.serial.set(serial);
    }

    public String getSerial() {
        return serial.get();
    }

    public StringProperty serialProperty() {
        return serial;
    }

    public String getAdbState() {
        return adbState.get();
    }

    public void setAdbState(String state) {
        adbState.set(state);
    }

    public StringProperty adbStateProperty() {
        return adbState;
    }

    public StringProperty modelProperty() {
        return model;
    }

    public void setModel(String model) {
        this.model.set(model == null ? "" : model);
    }

    public StringProperty androidVersionProperty() {
        return androidVersion;
    }

    public void setAndroidVersion(String version) {
        this.androidVersion.set(version == null ? "" : version);
    }

    public StringProperty lastSeenProperty() {
        return lastSeen;
    }

    public void markLastSeen(Instant instant) {
        lastSeen.set(TS_FORMAT.format(instant));
    }

    public StringProperty pingStatusProperty() {
        return pingStatus;
    }

    public void setPingStatus(String status) {
        pingStatus.set(status);
    }

    public StringProperty lastPingProperty() {
        return lastPing;
    }

    public void markLastPing(Instant instant) {
        lastPing.set(TS_FORMAT.format(instant));
    }

    public StringProperty tagProperty() {
        return tag;
    }

    public void setTag(String value) {
        tag.set(value == null ? "" : value);
    }

    public StringProperty expectedProperty() {
        return expected;
    }

    public void setExpected(boolean value) {
        expected.set(value ? "yes" : "no");
    }

    public StringProperty workerStatusProperty() {
        return workerStatus;
    }

    public void setWorkerStatus(String value) {
        workerStatus.set(value == null ? "-" : value);
    }

    public StringProperty workerVersionProperty() {
        return workerVersion;
    }

    public void setWorkerVersion(String value) {
        workerVersion.set(value == null || value.isBlank() ? "-" : value);
    }

    public StringProperty workerErrorProperty() {
        return workerError;
    }

    public void setWorkerError(String value) {
        workerError.set(value == null ? "" : value);
    }

    public IntegerProperty workerQueueDepthProperty() {
        return workerQueueDepth;
    }

    public void setWorkerQueueDepth(int value) {
        workerQueueDepth.set(Math.max(0, value));
    }

    public StringProperty workerUptimeProperty() {
        return workerUptime;
    }

    public void setWorkerUptime(long seconds) {
        if (seconds <= 0) {
            workerUptime.set("-");
        } else {
            workerUptime.set(seconds + "s");
        }
    }

    public IntegerProperty forwardedPortProperty() {
        return forwardedPort;
    }

    public int getForwardedPort() {
        return forwardedPort.get();
    }

    public void setForwardedPort(int port) {
        forwardedPort.set(port);
    }
}
