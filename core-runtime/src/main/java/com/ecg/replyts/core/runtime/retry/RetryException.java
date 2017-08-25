package com.ecg.replyts.core.runtime.retry;

public class RetryException extends Exception {

    private static final long serialVersionUID = -5087671549316655314L;

    public RetryException(String message) {
        super(message);
    }

    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
