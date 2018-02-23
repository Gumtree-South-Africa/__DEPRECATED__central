package com.ecg.messagebox.resources.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {

    @ApiModelProperty(required = true)
    private final String errorType;
    @ApiModelProperty(required = true)
    private final String errorMessage;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> errors = new ArrayList<>();

    public ErrorResponse(String errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public void addValidationError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equal(errors, that.errors) &&
                Objects.equal(errorMessage, that.errorMessage) &&
                Objects.equal(errorType, that.errorType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(errors, errorMessage, errorType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("errors", errors)
                .add("errorMessage", errorMessage)
                .add("errorType", errorType)
                .toString();
    }
}