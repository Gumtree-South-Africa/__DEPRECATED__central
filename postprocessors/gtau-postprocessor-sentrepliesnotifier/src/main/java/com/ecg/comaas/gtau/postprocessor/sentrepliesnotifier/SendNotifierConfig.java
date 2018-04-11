package com.ecg.comaas.gtau.postprocessor.sentrepliesnotifier;

public class SendNotifierConfig {
    private String endpointUrl;

    public SendNotifierConfig(final String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
