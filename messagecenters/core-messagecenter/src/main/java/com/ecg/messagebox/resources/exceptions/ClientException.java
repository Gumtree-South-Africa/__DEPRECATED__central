package com.ecg.messagebox.resources.exceptions;

import org.springframework.http.HttpStatus;

public class ClientException extends RuntimeException {

    private final HttpStatus httpStatus;

    public ClientException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
