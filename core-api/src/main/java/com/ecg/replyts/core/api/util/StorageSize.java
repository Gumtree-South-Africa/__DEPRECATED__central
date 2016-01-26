package com.ecg.replyts.core.api.util;

import com.google.common.base.Objects;

/**
 * abstraction for a number of bytes bound together to a unit (e.g. 3 kilobytes)
 */
public class StorageSize {

    private final int byteValue;

    public StorageSize(int value, StorageUnit unit) {
        this.byteValue = unit.toByte(value);
    }

    public String toString() {
        return byteValue + " B";
    }

    public boolean isBiggerThan(StorageSize other) {
        return byteValue() > other.byteValue();
    }

    public boolean isSmallerThan(StorageSize other) {
        return byteValue() < other.byteValue();
    }

    public boolean isBiggerOrEqualThan(StorageSize other) {
        return byteValue() >= other.byteValue();
    }

    public boolean isSmallerOrEqualThan(StorageSize other) {
        return byteValue() <= other.byteValue();
    }

    public int byteValue() {
        return byteValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof StorageSize) {
            return byteValue() == ((StorageSize) o).byteValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(byteValue);
    }

}
