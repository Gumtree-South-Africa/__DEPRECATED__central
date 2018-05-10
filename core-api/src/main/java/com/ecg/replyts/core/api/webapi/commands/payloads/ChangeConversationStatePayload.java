package com.ecg.replyts.core.api.webapi.commands.payloads;

import com.ecg.replyts.core.api.model.conversation.ConversationState;

/**
 * JSON Payload for moderate message command
 */
public class ChangeConversationStatePayload {

    private ConversationState state;
    private String issuerEmail;
    private Boolean deleteForIssuer;

    public String getIssuerEmail() {
        return issuerEmail;
    }

    public void setIssuerEmail(String issuerEmail) {
        this.issuerEmail = issuerEmail;
    }

    public ConversationState getState() {
        return state;
    }

    public void setState(ConversationState state) {
        this.state = state;
    }

    public Boolean isDeleteForIssuer() {
        if (deleteForIssuer == null) {
            return false;
        }
        return deleteForIssuer;
    }

    public void setDeleteForIssuer(Boolean deleteForIssuer) {
        this.deleteForIssuer = deleteForIssuer;
    }
}
