package com.ecg.comaas.gtau.filter.volumefilter;

public class MailReceivedEvent {

    private String mailAddress;

    public MailReceivedEvent(String mailAddress) {
        this.mailAddress = mailAddress;
    }

    public String getMailAddress() {
        return mailAddress;
    }
}
