package com.ecg.comaas.gtuk.filter.volume;

/**
 * Author: bpadhiar
 */
public enum ElasticsearchCustomHeaderKeys {
    BUYER_COOKIE("buyercookie"),
    BUYER_IP("buyerip");

    private String customHeaderKey;

    ElasticsearchCustomHeaderKeys(String customHeaderKey) {
        this.customHeaderKey = customHeaderKey;
    }

    public String getCustomHeaderKey() {
        return customHeaderKey;
    }
}
