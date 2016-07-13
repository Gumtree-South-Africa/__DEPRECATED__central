package com.ecg.messagecenter.persistence;

public enum MessageType {

    ASQ,
    ABQ,
    BID,
    CHAT,
    EMAIL;

    public static MessageType get(String value) {
        return value == null ? EMAIL : MessageType.valueOf(value.toUpperCase());
    }
}
