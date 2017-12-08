package com.ecg.replyts.core.api.persistence;

public class MessageNotFoundException extends Exception {

    public MessageNotFoundException(String message) {
        super(message);
    }

    public MessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
