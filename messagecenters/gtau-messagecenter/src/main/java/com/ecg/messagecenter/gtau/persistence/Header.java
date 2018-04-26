package com.ecg.messagecenter.gtau.persistence;

/**
 * Created by maotero on 7/09/2015.
 */
public enum Header {
    OfferId("X-Offerid"),
    Robot("X-Robot"),
    MessageLinks("X-Message-Links"),
    Autogate("X-Cust-Http-Account-Name"),
    MessageSender("X-Message-Sender"),
    RichTextMessage("X-RichText-Message"),
    RichTextLinks("X-RichText-Links");

    private String value;

    Header(String value) {
        this.value = value;
    }

    public String getValue() { return this.value; }
}
