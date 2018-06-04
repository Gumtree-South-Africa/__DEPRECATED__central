package com.ecg.comaas.mde.postprocessor.demandreporting.domain;

public final class EmailContactEvent implements Event {

    private final String senderMailAddress;
    private final String receiverMailAddress;
    private final String replyToMailAddress;
    private final String source;
    private final String subject;
    private final Long adId;
    private final String content;
    private final CommonEventData commonEventData;
    // Just for GSON deserialization...
    // Previous package before copying dependencies from tenants to comaas
    private final String eventType = "de.mobile.analytics.domain.contact.EmailContactEvent";

    private EmailContactEvent(Builder builder) {
        adId = builder.adId;
        senderMailAddress = builder.senderMailAddress;
        receiverMailAddress = builder.receiverMailAddress;
        replyToMailAddress = builder.replyToMailAddress;
        source = builder.source;
        subject = builder.subject;
        content = builder.content;
        commonEventData = builder.commonEventData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getAdId() {
        return adId;
    }

    public String getContent() {
        return content;
    }

    public String getReceiverMailAddress() {
        return receiverMailAddress;
    }

    public String getSenderMailAddress() {
        return senderMailAddress;
    }

    public String getSource() {
        return source;
    }

    public String getReplyToMailAddress() {
        return replyToMailAddress;
    }

    public String getSubject() {
        return subject;
    }

    public CommonEventData getCommonEventData() {
        return commonEventData;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    public static final class Builder {
        private Long adId;
        private String senderMailAddress;
        private String receiverMailAddress;
        private String replyToMailAddress;
        private String source;
        private String subject;
        private String content;
        private CommonEventData commonEventData;

        private Builder() {
        }

        public Builder adId(Long adId) {
            this.adId = adId;
            return this;
        }

        public Builder senderMailAddress(String senderMailAddress) {
            this.senderMailAddress = senderMailAddress;
            return this;
        }

        public Builder receiverMailAddress(String receiverMailAddress) {
            this.receiverMailAddress = receiverMailAddress;
            return this;
        }

        public Builder replyToMailAddress(String replyToMailAddress) {
            this.replyToMailAddress = replyToMailAddress;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public EmailContactEvent buildWithCommonEventData(CommonEventData commonEventData) {
            this.commonEventData = commonEventData;
            return new EmailContactEvent(this);
        }
    }
}