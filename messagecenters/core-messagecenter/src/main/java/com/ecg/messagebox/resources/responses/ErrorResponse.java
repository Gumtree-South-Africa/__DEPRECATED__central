package com.ecg.messagebox.resources.responses;

import com.ecg.replyts.core.Application;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    @ApiModelProperty(required = true)
    private final String application;
    @ApiModelProperty(required = true)
    private final String message;
    @ApiModelProperty(required = true)
    private final String revision;
    @ApiModelProperty
    private List<String> errors = new ArrayList<>();
    @ApiModelProperty
    private String details;
    @ApiModelProperty
    private Map<String, String> fields = new LinkedHashMap<>();

    public ErrorResponse(String message) {
        this.message = message;
        this.application = Application.class.getPackage().getImplementationTitle();
        this.revision = Application.class.getPackage().getImplementationVersion();
        this.fields = MDC.getCopyOfContextMap();
    }

    public String getMessage() {
        return message;
    }

    public String getApplication() {
        return application;
    }

    public String getRevision() {
        return revision;
    }

    @JsonAnyGetter
    public Map<String, String> getFields() {
        return fields;
    }

    public String getDetails() {
        return details;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addValidationError(String error) {
        errors.add(error);
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
