package com.ecg.sync;

public class UserIdNotFoundException extends RuntimeException {

    private final boolean loggable;

    public UserIdNotFoundException(String message) {
        this(message, true);
    }

    public UserIdNotFoundException(String message, boolean loggable) {
        super(message);
        this.loggable = loggable;
    }

    public boolean isLoggable() {
        return loggable;
    }
}
