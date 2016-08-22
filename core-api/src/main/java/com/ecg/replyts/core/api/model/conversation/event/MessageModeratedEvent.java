package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.command.MessageModeratedCommand;
import com.ecg.replyts.core.api.util.Assert;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

/**
 * Event to indicate the state change of a
 * {@link com.ecg.replyts.core.api.model.conversation.Message} in a
 * {@link com.ecg.replyts.core.api.model.conversation.Conversation}
 * due to a filter or pre-processor.
 */
public class MessageModeratedEvent extends ConversationEvent {
    private final String messageId;
    private final ModerationResultState humanResultState;
    private final String editor;

    public MessageModeratedEvent(MessageModeratedCommand command) {
        this(command.getMessageId(),
                command.getDecidedAt(),
                command.getModerationAction().getModerationResultState(),
                command.getModerationAction().getEditor().orNull());
    }

    @JsonCreator
    public MessageModeratedEvent(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("decidedAt") DateTime decidedAt,
            @JsonProperty("humanResultState") ModerationResultState humanResultState,
            @JsonProperty(value = "editor", required = false) String editor
    ) {
        super(eventIdForMessage(MessageModeratedEvent.class, messageId, decidedAt), decidedAt);
        Assert.notNull(messageId);
        Assert.notNull(humanResultState);
        this.messageId = messageId;
        this.humanResultState = humanResultState;
        this.editor = editor;
    }

    public String getMessageId() {
        return messageId;
    }

    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    public DateTime getDecidedAt() {
        return getConversationModifiedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageModeratedEvent that = (MessageModeratedEvent) o;

        return Pairwise.pairsAreEqual(
                humanResultState, that.humanResultState,
                messageId, that.messageId,
                getEventId(), that.getEventId(),
                getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messageId, humanResultState, getEventId(), getConversationModifiedAt().getMillis());
    }

    public String getEditor() {
        return editor;
    }
}
