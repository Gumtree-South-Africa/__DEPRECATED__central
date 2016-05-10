package com.ebay.ecg.replyts.robot.api.exception;

/**
 * Created by mdarapour.
 */
public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(String message) {
        super(message);
    }

    protected ConversationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
