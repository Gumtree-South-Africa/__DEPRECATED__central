package com.ecg.messagecenter.it.persistence;

public enum Header {
    OfferId("X-Offerid"),
    Robot("X-Robot");

    private String value;

    Header(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}