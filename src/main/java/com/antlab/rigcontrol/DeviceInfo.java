package com.antlab.rigcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
}
