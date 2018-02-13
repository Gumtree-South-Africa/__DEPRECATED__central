package com.ebay.ecg.bolt.domain.service.push.model;

public class PWAInfo {
    private String endPoint;

    private String publicKey;

    private String secret;

    private String appType;

    public PWAInfo(String endPoint, String publicKey, String secret, String appType) {
        this.endPoint = endPoint;
        this.publicKey = publicKey;
        this.secret = secret;
        this.appType = appType;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
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

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }
}