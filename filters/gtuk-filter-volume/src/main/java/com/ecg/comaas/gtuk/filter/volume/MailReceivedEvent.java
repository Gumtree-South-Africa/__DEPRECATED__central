package com.ecg.comaas.gtuk.filter.volume;

public class MailReceivedEvent {
    private String volumeFieldValue;

    MailReceivedEvent(String volumeFieldValue) {
        this.volumeFieldValue = volumeFieldValue;
    }

    public String getVolumeFieldValue() {
        return volumeFieldValue;
    }
}