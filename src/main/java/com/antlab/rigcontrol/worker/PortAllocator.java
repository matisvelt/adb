package com.antlab.rigcontrol.worker;

public final class PortAllocator {
    private PortAllocator() {
    }

    public static int portForSerial(String serial) {
        if (serial == null || serial.isBlank()) {
            return 18080;
        }
        int hash = Math.abs(serial.hashCode());
        return 18080 + (hash % 1000);
    }
}
