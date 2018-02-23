package com.ecg.messagebox.resources.exceptions;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class NotFoundException extends RuntimeException {

    private final String errorType;
    private final String errorMessage;

    public NotFoundException(String errorType, String errorMessage) {
        super(errorType + ":" + errorMessage);
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotFoundException that = (NotFoundException) o;
        return Objects.equal(errorType, that.errorType) &&
                Objects.equal(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(errorType, errorMessage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("errorType", errorType)
                .add("errorMessage", errorMessage)
                .toString();
    }
}