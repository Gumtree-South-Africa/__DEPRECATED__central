package com.ecg.replyts.core.api.util;

/**
 * abstraction for storage units (gb, mb, kb and bytes)
 */
public enum StorageUnit {

    B(1),
    KB(1024),
    MB(1024 * 1024);

    private final int toByte;

    StorageUnit(int toByte) {
        this.toByte = toByte;
    }

    /**
     * calculates the number of bytes for this value in the specified unit
     */
    public int toByte(int value) {
        return value * toByte;
    }
}
