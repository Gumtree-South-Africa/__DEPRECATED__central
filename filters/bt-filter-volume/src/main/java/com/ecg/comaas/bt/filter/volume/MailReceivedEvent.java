package com.ecg.comaas.bt.filter.volume;

public class MailReceivedEvent {

    private String mailAddress;

    public MailReceivedEvent(String mailAddress) {
        this.mailAddress = mailAddress;
    }

    public String getMailAddress() {
        return mailAddress;
    }
}
