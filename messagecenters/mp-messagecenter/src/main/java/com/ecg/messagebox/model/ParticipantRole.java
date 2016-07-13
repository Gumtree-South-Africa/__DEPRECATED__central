package com.ecg.messagebox.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum ParticipantRole {

    BUYER("buyer"),
    SELLER("seller");

    private String value;

    private static final Map<String, ParticipantRole> lookup = new HashMap<>();

    static {
        for (ParticipantRole v : EnumSet.allOf(ParticipantRole.class))
            lookup.put(v.getValue(), v);
    }

    ParticipantRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ParticipantRole get(String value) {
        return lookup.get(value);
    }

    public static Set<ParticipantRole> get(Set<String> values) {
        return values.stream().map(ParticipantRole::get).collect(Collectors.toSet());
    }
}