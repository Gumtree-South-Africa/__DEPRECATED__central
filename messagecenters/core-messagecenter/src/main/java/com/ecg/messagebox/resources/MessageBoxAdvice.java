package com.ecg.messagebox.resources;

import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.resources.exceptions.NotFoundException;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.ErrorResponseBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.beans.PropertyEditorSupport;

@ControllerAdvice
public class MessageBoxAdvice extends ResponseEntityExceptionHandler {

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(Visibility.class, new VisibilityEditor());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(NotFoundException ex) {
        ErrorResponse error = ErrorResponseBuilder.fromNotFoundException(ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse error = ErrorResponseBuilder.fromException(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ErrorResponse error = ErrorResponseBuilder.fromBinding(ex.getBindingResult());
        return ResponseEntity.badRequest().body(error);
    }

    private static class VisibilityEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(final String text) {
            setValue(Visibility.valueOf(text.toUpperCase()));
        }
    }
}