package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

/**
 * Details we need to be able to send notification to potential buyers
 * when an ad is deleted.
 */
public class BuyerContactResponse {
    private String name;
    private String email;
    private String conversationId;
    private DateTime conversationLastUpdated;
    private ConversationState conversationState;

    public BuyerContactResponse(String name, String email, String conversationId,
                    DateTime conversationLastUpdated, ConversationState conversationState) {
        this.name = name;

        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(conversationLastUpdated);
        Preconditions.checkNotNull(conversationState);
        this.email = email;
        this.conversationId = conversationId;
        this.conversationLastUpdated = conversationLastUpdated;
        this.conversationState = conversationState;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getConversationLastUpdated() {
        return MessageCenterUtils
                        .toFormattedTimeISO8601ExplicitTimezoneOffset(conversationLastUpdated);
    }

    public ConversationState getConversationState() {
        return conversationState;
    }
}
