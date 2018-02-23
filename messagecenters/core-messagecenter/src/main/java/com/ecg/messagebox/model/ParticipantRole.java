package com.ecg.messagebox.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum ParticipantRole {

    BUYER, SELLER;

    private static final Map<String, ParticipantRole> lookup = new HashMap<>();

    static {
        for (ParticipantRole v : EnumSet.allOf(ParticipantRole.class))
            lookup.put(v.getValue(), v);
    }

    @JsonValue
    public String getValue() {
        return this.name().toLowerCase();
    }

    public static ParticipantRole get(String value) {
        return lookup.get(value);
    }

    public static Set<ParticipantRole> get(Set<String> values) {
        return values.stream().map(ParticipantRole::get).collect(Collectors.toSet());
    }
}