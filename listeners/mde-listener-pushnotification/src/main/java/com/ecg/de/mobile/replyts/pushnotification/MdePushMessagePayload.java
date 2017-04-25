package com.ecg.de.mobile.replyts.pushnotification;

import com.google.common.base.Objects;

public class MdePushMessagePayload {

    public MdePushMessagePayload(String conversationId, String adId, String message, String customerId, String title) {
        this.conversationId = conversationId;
        this.adId = adId;
        this.message = message;
        this.customerId = customerId;
        this.title = title;
    }

    private final String conversationId;
    private final String adId;
    private final String message;
    private final String customerId;
    private final String title;

    public String getConversationId() {
        return conversationId;
    }

    public String getAdId() {
        return adId;
    }

    public String getMessage() {
        return message;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MdePushMessagePayload that = (MdePushMessagePayload) o;
        return
                Objects.equal(this.conversationId, that.conversationId)
                        && Objects.equal(this.adId, that.adId)
                        && Objects.equal(this.message, that.message)
                        && Objects.equal(this.customerId, that.customerId)
                        && Objects.equal(this.title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                conversationId,
                adId,
                message,
                customerId,
                title
        );
    }
}
