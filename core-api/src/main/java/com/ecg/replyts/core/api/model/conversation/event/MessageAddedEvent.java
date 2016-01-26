package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.util.Assert;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Event to indicate a {@link com.ecg.replyts.core.api.model.conversation.Message}
 * was added to a {@link com.ecg.replyts.core.api.model.conversation.Conversation}.
 */
public class MessageAddedEvent extends ConversationEvent {
    private final String messageId;
    private final MessageDirection messageDirection;
    private final DateTime receivedAt;
    private final FilterResultState filterResultState;
    private final ModerationResultState humanResultState;
    private final Map<String, String> headers;
    private final String plainTextBody;
    private final MessageState state;
    private final String senderMessageIdHeader;
    private final String inResponseToMessageId;
    private List<String> attachments;

    @JsonCreator
    public MessageAddedEvent( // NOSONAR
                              @JsonProperty("messageId") String messageId,
                              @JsonProperty("messageDirection") MessageDirection messageDirection,
                              @JsonProperty("receivedAt") DateTime receivedAt,
                              @JsonProperty("state") MessageState state,
                              @JsonProperty("senderMessageIdHeader") String senderMessageIdHeader,
                              @JsonProperty("inResponseToMessageId") String inResponseToMessageId,
                              @JsonProperty("filterResultState") FilterResultState filterResultState,
                              @JsonProperty("humanResultState") ModerationResultState humanResultState,
                              @JsonProperty("headers") Map<String, String> headers,
                              @JsonProperty("plainTextBody") String plainTextBody,
                              @JsonProperty("attachments") List<String> attachments) {
        super(eventIdForMessage(MessageAddedEvent.class, messageId, receivedAt), receivedAt);
        Assert.notNull(messageDirection);
        Assert.notNull(receivedAt);
        Assert.notNull(filterResultState);
        Assert.notNull(humanResultState);
        this.messageId = messageId;
        this.attachments = attachments == null ? Collections.<String>emptyList() : attachments;
        this.messageDirection = messageDirection;
        this.receivedAt = receivedAt;
        this.filterResultState = filterResultState;
        this.humanResultState = humanResultState;
        this.headers = headers;
        this.plainTextBody = plainTextBody;
        this.state = state;
        this.senderMessageIdHeader = senderMessageIdHeader;
        this.inResponseToMessageId = inResponseToMessageId;
    }

    public MessageAddedEvent(AddMessageCommand command) {
        this(
                command.getMessageId(),
                command.getMessageDirection(),
                command.getReceivedAt(),
                command.getState(),
                command.getSenderMessageIdHeader(),
                command.getInResponseToMessageId(),
                FilterResultState.OK,
                ModerationResultState.GOOD,
                command.getHeaders(),
                command.getPlainTextBody(),
                command.getAttachments()
        );
    }

    public String getMessageId() {
        return messageId;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    public DateTime getReceivedAt() {
        return receivedAt;
    }

    public FilterResultState getFilterResultState() {
        return filterResultState;
    }

    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getPlainTextBody() {
        return plainTextBody;
    }

    public MessageState getState() {
        return state;
    }

    public String getSenderMessageIdHeader() {
        return senderMessageIdHeader;
    }

    public String getInResponseToMessageId() {
        return inResponseToMessageId;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageAddedEvent that = (MessageAddedEvent) o;

        return Pairwise.pairsAreEqual(filterResultState, that.filterResultState,
                headers, that.headers,
                humanResultState, that.humanResultState,
                messageDirection, that.messageDirection,
                messageId, that.messageId,
                plainTextBody, that.plainTextBody,
                receivedAt, that.receivedAt,
                state, that.state,
                senderMessageIdHeader, that.senderMessageIdHeader,
                inResponseToMessageId, that.inResponseToMessageId,
                getEventId(), that.getEventId(),
                getConversationModifiedAt(), that.getConversationModifiedAt()
        );

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messageId, messageDirection, receivedAt, filterResultState, humanResultState, headers, plainTextBody, state, senderMessageIdHeader, inResponseToMessageId, getEventId(), getConversationModifiedAt());
    }

}
