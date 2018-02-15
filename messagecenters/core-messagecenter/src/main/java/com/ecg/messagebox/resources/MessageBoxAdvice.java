package com.ecg.messagebox.resources;

import com.ecg.messagebox.resources.exceptions.NotFoundException;
import com.ecg.messagebox.resources.responses.ValidationError;
import com.ecg.messagebox.resources.responses.ValidationErrorBuilder;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
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

@ControllerAdvice
public class MessageBoxAdvice extends ResponseEntityExceptionHandler {

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleEntityNotFoundException() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ValidationError error = ValidationErrorBuilder.fromBinding(ex.getBindingResult());
        return super.handleExceptionInternal(ex, error, headers, status, request);
    }
}