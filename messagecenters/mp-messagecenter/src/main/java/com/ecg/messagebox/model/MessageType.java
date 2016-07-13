package com.ecg.messagebox.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MessageType {

    ASQ("asq"),
    ABQ("abq"),
    BID("bid"),
    CHAT("chat"),
    EMAIL("email");

    private String value;

    private static final Map<String, MessageType> lookup = new HashMap<>();

    static {
        for (MessageType v : EnumSet.allOf(MessageType.class))
            lookup.put(v.getValue(), v);
    }

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MessageType get(String value) {
        return lookup.get(value);
    }
}
