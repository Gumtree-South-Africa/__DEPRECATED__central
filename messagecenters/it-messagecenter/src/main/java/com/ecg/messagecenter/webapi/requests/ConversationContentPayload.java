package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;

/**
 * Created by jaludden on 27/11/15.
 */
public class ConversationContentPayload {

    private Long adId;
    private String username;
    private ConversationRole role;
    private String message;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Deprecated public ConversationRole getRole() {
        return role;
    }

    @Deprecated public void setRole(ConversationRole role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public String getGreating() {
        return "Ecco cosa ti ha scritto:";
    }

    public String getType() {
        return "messagio";
    }

    public void cleanupMessage() {
        message = RequestUtil.cleanText(message);
    }
}
