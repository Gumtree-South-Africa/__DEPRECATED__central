package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import java.io.Serializable;

public class MailReceivedNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String mailAddress;
    private final String correlationId;

    public MailReceivedNotification(String mailAddress, String correlationId) {
        this.mailAddress = mailAddress;
        this.correlationId = correlationId;
    }

    public String getMailAddress() {
        return mailAddress;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
