package com.antlab.rigcontrol;

public enum DeviceStatus {
    DEVICE("device"),
    OFFLINE("offline"),
    UNAUTHORIZED("unauthorized"),
    DISCONNECTED("disconnected"),
    UNKNOWN("unknown");

    private final String adbLabel;

    DeviceStatus(String adbLabel) {
        this.adbLabel = adbLabel;
    }

    public String getAdbLabel() {
        return adbLabel;
    }

    public static DeviceStatus fromAdbState(String state) {
        if (state == null) {
            return UNKNOWN;
        }
        String s = state.trim().toLowerCase();
        for (DeviceStatus status : values()) {
            if (status.adbLabel.equals(s)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
