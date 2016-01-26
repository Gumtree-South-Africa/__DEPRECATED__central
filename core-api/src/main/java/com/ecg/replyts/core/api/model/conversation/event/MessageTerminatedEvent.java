package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class MessageTerminatedEvent extends ConversationEvent {
    private final String messageId;
    private final String reason;
    private final String issuer;
    private final MessageState terminationState;

    public MessageTerminatedEvent(MessageTerminatedCommand command) {
        this(command.getMessageId(),
                DateTime.now(),
                command.getReason(),
                command.getIssuer().getName(),
                command.getTerminationState());
    }

    @JsonCreator
    public MessageTerminatedEvent(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("conversationModifiedAt") DateTime conversationModifiedAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("issuer") String issuer,
            @JsonProperty("terminationState") MessageState terminationState
    ) {
        super(eventIdForMessage(MessageTerminatedEvent.class, messageId, conversationModifiedAt),
                conversationModifiedAt);
        this.messageId = messageId;
        this.reason = reason;
        this.issuer = issuer;
        this.terminationState = terminationState;
    }

    public String getMessageId() {
        return messageId;
    }


    public String getReason() {
        return reason;
    }

    public String getIssuer() {
        return issuer;
    }

    public MessageState getTerminationState() {
        return terminationState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageTerminatedEvent that = (MessageTerminatedEvent) o;

        return Pairwise.pairsAreEqual(
                messageId, that.messageId,
                reason, that.reason,
                issuer, that.issuer,
                terminationState, that.terminationState,
                getEventId(), that.getEventId(),
                getConversationModifiedAt(), that.getConversationModifiedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messageId, reason, issuer, terminationState, getEventId(), getConversationModifiedAt());
    }
}
