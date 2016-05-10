package com.ebay.ecg.replyts.robot.api.requests.payload;

import java.util.List;

/**
 * Created by maotero on 15/09/2015.
 */
public class GetConversationsResponsePayload {
    private String adId;
    private List<String> conversationIds;

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public List<String> getConversationIds() {
        return conversationIds;
    }

    public void setConversationIds(List<String> conversationIds) {
        this.conversationIds = conversationIds;
    }
}
