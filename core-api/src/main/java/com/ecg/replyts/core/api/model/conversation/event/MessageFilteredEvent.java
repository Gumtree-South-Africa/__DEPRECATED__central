package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.command.MessageFilteredCommand;
import com.ecg.replyts.core.api.util.Assert;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Event to indicate the state change of a
 * {@link com.ecg.replyts.core.api.model.conversation.Message} in a
 * {@link com.ecg.replyts.core.api.model.conversation.Conversation} due to a filter or pre-processor.
 */
public class MessageFilteredEvent extends ConversationEvent {

    private final String messageId;
    private final FilterResultState filterResultState;
    private List<ProcessingFeedback> processingFeedback;

    public MessageFilteredEvent(MessageFilteredCommand command) {
        this(command.getMessageId(),
                command.getDecidedAt(),
                command.getFilterResultState(),
                command.getProcessingFeedback());
    }

    @JsonCreator
    public MessageFilteredEvent(@JsonProperty("messageId") String messageId, @JsonProperty("decidedAt") DateTime decidedAt, @JsonProperty("filterResultState") FilterResultState filterResultState,
                                @JsonProperty("processingFeedback") List<ProcessingFeedback> processingFeedback) {
        super(eventIdForMessage(MessageFilteredEvent.class, messageId, decidedAt), decidedAt);
        Assert.notNull(messageId);
        Assert.notNull(filterResultState);
        this.messageId = messageId;
        this.filterResultState = filterResultState;
        this.processingFeedback = processingFeedback;
    }

    public String getMessageId() {
        return messageId;
    }

    public DateTime getDecidedAt() {
        return getConversationModifiedAt();
    }

    public FilterResultState getFilterResultState() {
        return filterResultState;
    }

    public List<ProcessingFeedback> getProcessingFeedback() {
        return processingFeedback;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageFilteredEvent that = (MessageFilteredEvent) o;

        return Pairwise.pairsAreEqual(
                filterResultState, that.filterResultState,
                messageId, that.messageId,
                processingFeedback, that.processingFeedback,
                getEventId(), that.getEventId(),
                getConversationModifiedAt(), that.getConversationModifiedAt()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(filterResultState, messageId, processingFeedback, getEventId(), getConversationModifiedAt());
    }
}
