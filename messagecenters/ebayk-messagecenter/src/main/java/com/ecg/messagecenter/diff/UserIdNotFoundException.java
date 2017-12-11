package com.ecg.messagecenter.diff;

class UserIdNotFoundException extends RuntimeException {

    final static String UNKNOWN = "<unknown>";

    UserIdNotFoundException(String email) {
        this(email, UNKNOWN, UNKNOWN);
    }

    UserIdNotFoundException(String email, String role, String conversationId) {
        super(String.format("UserId was not found for email: %s, role: %s, conversation: %s", email, role, conversationId));
    }
}
