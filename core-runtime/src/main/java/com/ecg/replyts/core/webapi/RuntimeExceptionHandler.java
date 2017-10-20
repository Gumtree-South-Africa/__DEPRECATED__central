package com.ecg.replyts.core.webapi;

import com.ecg.replyts.core.runtime.ReplyTS;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.APPLICATION;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.REVISION;

@ControllerAdvice
public final class RuntimeExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeExceptionHandler.class);
    private static final String STATE = "state";
    private static final String FAILURE = "FAILURE";
    private static final String MESSAGE = "message";
    private static final String DETAILS = "details";

    @ExceptionHandler(value = { RuntimeException.class })
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        LOG.error("Exception on {}", request, ex);

        Map<String, String> response = new LinkedHashMap<>();
        response.put(STATE, FAILURE);
        response.put(MESSAGE, ex.getClass().getName() + ": " + ex.getMessage());
        response.put(APPLICATION, ReplyTS.class.getPackage().getImplementationTitle());
        response.put(REVISION, ReplyTS.class.getPackage().getImplementationVersion());

        MDC.getCopyOfContextMap().forEach((key, value) -> response.put(key, value));

        response.put(DETAILS, Throwables.getStackTraceAsString(ex));

        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
