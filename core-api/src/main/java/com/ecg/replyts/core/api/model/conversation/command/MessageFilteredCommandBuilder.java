package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link MessageFilteredCommand}.
 */
public final class MessageFilteredCommandBuilder {
    private String conversationId;
    private String messageId;
    private DateTime decidedAt = new DateTime();
    private FilterResultState filterResultState = FilterResultState.OK;
    private List<ProcessingFeedback> processingFeedback = new ArrayList<ProcessingFeedback>();

    private MessageFilteredCommandBuilder() {
    }

    public static MessageFilteredCommandBuilder aMessageFilteredCommand(String conversationId, String messageId) {
        MessageFilteredCommandBuilder result = new MessageFilteredCommandBuilder();
        result.conversationId = conversationId;
        result.messageId = messageId;
        return result;
    }

    public MessageFilteredCommandBuilder withState(FilterResultState filterResultState) {
        this.filterResultState = filterResultState;
        return this;
    }

    public MessageFilteredCommandBuilder withDecidedAt(DateTime decidedAt) {
        this.decidedAt = decidedAt;
        return this;
    }

    public MessageFilteredCommandBuilder withProcessingFeedback(List<ProcessingFeedback> processingFeedback) {
        this.processingFeedback = processingFeedback;
        return this;
    }

    public MessageFilteredCommand build() {
        return new MessageFilteredCommand(conversationId, messageId, decidedAt, filterResultState, processingFeedback);
    }
}
