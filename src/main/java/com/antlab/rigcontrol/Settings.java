package com.antlab.rigcontrol;

import java.util.prefs.Preferences;

public class Settings {
    private static final String KEY_ADB_PATH = "adbPath";
    private static final String KEY_POLL_INTERVAL = "pollIntervalSeconds";
    private static final String KEY_PING_INTERVAL = "pingIntervalSeconds";
    private static final String KEY_ADB_TIMEOUT = "adbTimeoutSeconds";
    private static final String KEY_SIM_ENABLED = "simulationEnabled";
    private static final String KEY_SIM_FILE = "simulationFile";
    private static final String KEY_PING_MAX_CONCURRENCY = "pingMaxConcurrency";
    private static final String KEY_PING_BACKOFF = "pingFailureBackoffSeconds";
    private static final String KEY_ADB_RATE_LIMIT = "adbRateLimitPerInterval";
    private static final String KEY_ADB_RATE_INTERVAL = "adbRateIntervalSeconds";
    private static final String KEY_BACKOFF_MAX = "pingBackoffMaxSeconds";
    private static final String KEY_BACKOFF_STEP = "pingBackoffStepSeconds";
    private static final String KEY_LAST_WIDTH = "windowWidth";
    private static final String KEY_LAST_HEIGHT = "windowHeight";

    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    public String getAdbPath() {
        return prefs.get(KEY_ADB_PATH, "adb");
    }

    public void setAdbPath(String path) {
        prefs.put(KEY_ADB_PATH, path == null || path.isBlank() ? "adb" : path.trim());
    }

    public int getPollIntervalSeconds() {
        return prefs.getInt(KEY_POLL_INTERVAL, 3);
    }

    public void setPollIntervalSeconds(int seconds) {
        prefs.putInt(KEY_POLL_INTERVAL, Math.max(1, seconds));
    }

    public int getPingIntervalSeconds() {
        return prefs.getInt(KEY_PING_INTERVAL, 10);
    }

    public void setPingIntervalSeconds(int seconds) {
        prefs.putInt(KEY_PING_INTERVAL, Math.max(3, seconds));
    }

    public int getAdbTimeoutSeconds() {
        return prefs.getInt(KEY_ADB_TIMEOUT, 5);
    }

    public void setAdbTimeoutSeconds(int seconds) {
        prefs.putInt(KEY_ADB_TIMEOUT, Math.max(2, seconds));
    }

    public boolean isSimulationEnabled() {
        return prefs.getBoolean(KEY_SIM_ENABLED, false);
    }

    public void setSimulationEnabled(boolean enabled) {
        prefs.putBoolean(KEY_SIM_ENABLED, enabled);
    }

    public String getSimulationFile() {
        return prefs.get(KEY_SIM_FILE, "");
    }

    public void setSimulationFile(String path) {
        prefs.put(KEY_SIM_FILE, path == null ? "" : path.trim());
    }

    public int getPingMaxConcurrency() {
        return prefs.getInt(KEY_PING_MAX_CONCURRENCY, 6);
    }

    public void setPingMaxConcurrency(int value) {
        prefs.putInt(KEY_PING_MAX_CONCURRENCY, Math.max(1, value));
    }

    public int getPingFailureBackoffSeconds() {
        return prefs.getInt(KEY_PING_BACKOFF, 10);
    }

    public void setPingFailureBackoffSeconds(int seconds) {
        prefs.putInt(KEY_PING_BACKOFF, Math.max(2, seconds));
    }

    public int getAdbRateLimitPerInterval() {
        return prefs.getInt(KEY_ADB_RATE_LIMIT, 20);
    }

    public void setAdbRateLimitPerInterval(int value) {
        prefs.putInt(KEY_ADB_RATE_LIMIT, Math.max(5, value));
    }

    public int getAdbRateIntervalSeconds() {
        return prefs.getInt(KEY_ADB_RATE_INTERVAL, 5);
    }

    public void setAdbRateIntervalSeconds(int value) {
        prefs.putInt(KEY_ADB_RATE_INTERVAL, Math.max(1, value));
    }

    public int getPingBackoffMaxSeconds() {
        return prefs.getInt(KEY_BACKOFF_MAX, 120);
    }

    public void setPingBackoffMaxSeconds(int value) {
        prefs.putInt(KEY_BACKOFF_MAX, Math.max(10, value));
    }

    public int getPingBackoffStepSeconds() {
        return prefs.getInt(KEY_BACKOFF_STEP, 10);
    }

    public void setPingBackoffStepSeconds(int value) {
        prefs.putInt(KEY_BACKOFF_STEP, Math.max(5, value));
    }

    public double getWindowWidth() {
        return prefs.getDouble(KEY_LAST_WIDTH, 1200);
    }

    public void setWindowWidth(double value) {
        prefs.putDouble(KEY_LAST_WIDTH, Math.max(800, value));
    }

    public double getWindowHeight() {
        return prefs.getDouble(KEY_LAST_HEIGHT, 700);
    }

    public void setWindowHeight(double value) {
        prefs.putDouble(KEY_LAST_HEIGHT, Math.max(600, value));
    }
}
