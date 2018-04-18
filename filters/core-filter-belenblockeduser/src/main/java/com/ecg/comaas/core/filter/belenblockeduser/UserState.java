package com.ecg.comaas.core.filter.belenblockeduser;

enum UserState {

    ACTIVE, BLOCKED, UNDECIDED;

    public static UserState from(String state, UserState defaultState) {
        if ("ACTIVE".equalsIgnoreCase(state)) {
            return ACTIVE;
        } else if ("BLOCKED".equalsIgnoreCase(state)) {
            return BLOCKED;
        } else {
            return defaultState;
        }
    }
}
