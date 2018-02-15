package com.ecg.messagecenter.diff;

class UserIdNotFoundException extends RuntimeException {

    private final boolean loggable;

    UserIdNotFoundException(String message) {
        this(message, true);
    }

    UserIdNotFoundException(String message, boolean loggable) {
        super(message);
        this.loggable = loggable;
    }

    public boolean isLoggable() {
        return loggable;
    }
}
