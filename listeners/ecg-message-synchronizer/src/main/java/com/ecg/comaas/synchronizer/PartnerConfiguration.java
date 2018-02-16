package com.ecg.comaas.synchronizer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("message.synchronizer")
class PartnerConfiguration {

    private Map<String, String> partners;

    public Map<String, String> getPartners() {
        return partners;
    }

    public void setPartners(Map<String, String> partners) {
        this.partners = partners;
    }

    String getAddress(String tenantName) {
        return partners.get(tenantName);
    }
}
