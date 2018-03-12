package com.ecg.messagebox.resources;

import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.beans.PropertyEditorSupport;
import java.util.List;

@ControllerAdvice
public class MessageBoxAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MessageBoxAdvice.class);

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(Visibility.class, new VisibilityEditor());
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ErrorResponse> handleClientException(ClientException exception) {
        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException exception, WebRequest request) {
        LOG.error("Exception on {}", request, exception);

        ErrorResponse errorResponse = new ErrorResponse(exception.getClass().getName() + ": " + exception.getMessage());
        errorResponse.setDetails(Throwables.getStackTraceAsString(exception));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<ObjectError> allErrors = exception.getBindingResult().getAllErrors();

        ErrorResponse errorResponse = new ErrorResponse(String.format("Validation failed. %d error(s)", allErrors.size()));

        allErrors.stream().map(ObjectError::getDefaultMessage).forEach(errorResponse::addValidationError);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    private static class VisibilityEditor extends PropertyEditorSupport {
        @Override
        public void setAsText(final String text) {
            setValue(Visibility.valueOf(text.toUpperCase()));
        }
    }
}