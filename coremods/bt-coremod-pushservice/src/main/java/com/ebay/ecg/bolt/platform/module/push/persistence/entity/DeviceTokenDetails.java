package com.ebay.ecg.bolt.platform.module.push.persistence.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class DeviceTokenDetails {
    @JsonSerialize
    @JsonDeserialize
    private String deviceToken;

    @JsonSerialize
    @JsonDeserialize
    private String publicKey;

    @JsonSerialize
    @JsonDeserialize
    private String secret;

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}