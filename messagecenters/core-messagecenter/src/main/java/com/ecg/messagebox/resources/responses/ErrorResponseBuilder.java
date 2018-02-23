package com.ecg.messagebox.resources.responses;

import com.ecg.messagebox.resources.exceptions.NotFoundException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

public class ErrorResponseBuilder {

    public static ErrorResponse fromBinding(Errors errors) {
        ErrorResponse error = new ErrorResponse("ValidationError", "Validation failed. " + errors.getErrorCount() + " error(s)");
        for (ObjectError objectError : errors.getAllErrors()) {
            error.addValidationError(objectError.getDefaultMessage());
        }
        return error;
    }

    public static ErrorResponse fromNotFoundException(NotFoundException ex) {
        return new ErrorResponse(ex.getErrorType(), ex.getErrorMessage());
    }

    public static ErrorResponse fromException(Exception ex) {
        return new ErrorResponse("UnknownError", ex.getMessage());
    }
}