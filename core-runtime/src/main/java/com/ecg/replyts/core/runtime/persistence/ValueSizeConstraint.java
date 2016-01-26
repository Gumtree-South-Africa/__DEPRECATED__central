package com.ecg.replyts.core.runtime.persistence;

public final class ValueSizeConstraint {

    private final int maxSizeAllowed;

    private ValueSizeConstraint(int maxSizeAllowed) {
        this.maxSizeAllowed = maxSizeAllowed;
    }

    public static ValueSizeConstraint maxMb(int megs) {
        return new ValueSizeConstraint(megs * 1024 * 1024);
    }

    public static ValueSizeConstraint maxKb(int kbs) {
        return new ValueSizeConstraint(kbs * 1024);
    }

    public boolean isTooBig(int actualSize) {
        return actualSize > maxSizeAllowed;
    }

    public void validate(String description, int actualSize) {
        if (isTooBig(actualSize)) {
            throw new IllegalArgumentException(description + " is too large (" + actualSize + "), maximum is " + maxSizeAllowed);
        }
    }

}
