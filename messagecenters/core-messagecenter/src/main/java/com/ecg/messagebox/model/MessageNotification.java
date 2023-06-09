package com.ecg.messagebox.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MessageNotification {

    RECEIVE(1),
    MUTE(2);

    private int code;

    private static final Map<Integer, MessageNotification> lookup = new HashMap<>();

    static {
        for (MessageNotification v : EnumSet.allOf(MessageNotification.class))
            lookup.put(v.getCode(), v);
    }

    MessageNotification(int code) {
        this.code = code;
    }

    @JsonValue
    public String getValue() {
        return this.name().toLowerCase();
    }

    public int getCode() {
        return code;
    }

    public static MessageNotification get(int code) {
        return lookup.get(code);
    }
}