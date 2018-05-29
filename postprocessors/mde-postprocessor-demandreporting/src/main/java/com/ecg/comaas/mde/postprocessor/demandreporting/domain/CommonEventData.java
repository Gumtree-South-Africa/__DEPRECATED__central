package com.ecg.comaas.mde.postprocessor.demandreporting.domain;

import java.util.*;

public class CommonEventData {

    public static final String NO_VARIANT = "NO_VARIANT";

    private final Date creationTime;
    private final String uuid;
    private final String userId;
    private final String deviceId;
    private final String publisher;
    private final String userAgent;
    private final String ip;
    private final String txId;
    private final String referrer;
    private final Map<String, String> activeAbTestVariants = new HashMap<>();

    private CommonEventData(Builder b) {
        creationTime = new Date();
        uuid = UUID.randomUUID().toString();
        userId = b.userId;
        deviceId = b.deviceId;
        publisher = b.publisher;
        userAgent = b.userAgent;
        ip = b.ip;
        txId = b.txId;
        referrer = b.referrer;
        activeAbTestVariants.putAll(b.activeAbTestsVariants);
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIp() {
        return ip;
    }

    public String getTxId() {
        return txId;
    }

    public boolean hasAbTest(String abTestName) {
        return activeAbTestVariants.containsKey(abTestName);
    }

    public String getActiveVariantOfAbTest(String abTestName) {
        String variant = activeAbTestVariants.get(abTestName);
        return variant == null ? NO_VARIANT : variant;
    }

    public Collection<String> getAllAbTests() {
        return new ArrayList<>(activeAbTestVariants.keySet());
    }

    public Map<String, String> getAllAbTestsWithActiveVariant() {
        return new HashMap<>(activeAbTestVariants);
    }

    public String getReferrer() {
        return referrer;
    }

    public static CommonEventData.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String userId;
        private String deviceId;
        private String publisher;
        private String userAgent;
        private String ip;
        private String txId;
        private String referrer;
        public Map<String, String> activeAbTestsVariants = new HashMap<>();

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder txId(String txId) {
            this.txId = txId;
            return this;
        }

        public Builder referrer(String referrer) {
            this.referrer = referrer;
            return this;
        }

        public Builder activeVariantForAbTest(String activeVariant, String abTest) {
            this.activeAbTestsVariants.put(abTest, activeVariant);
            return this;
        }

        public Builder abTestsWithActiveVariants(Map<String, String> activeAbTestsVariants) {
            this.activeAbTestsVariants = new HashMap<>(activeAbTestsVariants);
            return this;
        }

        public CommonEventData build() {
            return new CommonEventData(this);
        }
    }
}
