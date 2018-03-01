package com.ebay.ecg.replyts.robot.api.requests.payload;

import java.util.Set;

public final class GetConversationsResponsePayload {

    private final String adId;
    private final Set<String> conversationIds;

    public GetConversationsResponsePayload(String adId, Set<String> conversationIds) {
        this.adId = adId;
        this.conversationIds = conversationIds;
    }

    public String getAdId() {
        return adId;
    }

    public Set<String> getConversationIds() {
        return conversationIds;
    }
}
