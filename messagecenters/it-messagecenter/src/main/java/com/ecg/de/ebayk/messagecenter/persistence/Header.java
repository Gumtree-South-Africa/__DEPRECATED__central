package com.ecg.de.ebayk.messagecenter.persistence;

/**
 * Created by maotero on 7/09/2015.
 */
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
