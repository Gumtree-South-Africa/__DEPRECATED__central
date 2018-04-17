package com.ecg.replyts.core.runtime.prometheus;

public enum ExternalServiceType {

    MY_SQL("mysql"),
    RABBIT_MQ("rabbitmq"),
    AUTO_GATE("autogate");

    private String label;

    ExternalServiceType(String label) {
        this.label = label;
    }

    String getLabel() {
        return label;
    }
}
