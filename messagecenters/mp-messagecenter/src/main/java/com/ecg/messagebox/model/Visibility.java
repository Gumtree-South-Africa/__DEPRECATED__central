package com.ecg.messagebox.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Visibility {

    RECENT(1),
    ARCHIVED(2);

    private int code;

    private static final Map<Integer, Visibility> lookup = new HashMap<>();

    static {
        for (Visibility v : EnumSet.allOf(Visibility.class))
            lookup.put(v.getCode(), v);
    }

    Visibility(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Visibility get(int code) {
        return lookup.get(code);
    }
}