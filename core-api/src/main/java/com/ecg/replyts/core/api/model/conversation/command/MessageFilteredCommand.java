package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

/**
 * Command to store results of filtering a
 * {@link com.ecg.replyts.core.api.model.conversation.Message} in a
 * {@link com.ecg.replyts.core.api.model.conversation.Conversation}.
 */
public class MessageFilteredCommand extends ConversationCommand {
    private final String messageId;
    private final DateTime decidedAt;
    private final FilterResultState filterResultState;
    private final List<ProcessingFeedback> processingFeedback;

    public MessageFilteredCommand(String conversationId, String messageId, DateTime decidedAt, FilterResultState filterResultState, List<ProcessingFeedback> processingFeedback) {
        super(conversationId);
//        Assert.noNullElements(new Object[]{state, decidedAt, filterResultState, processingFeedback});
        this.messageId = messageId;
        this.decidedAt = decidedAt;
        this.filterResultState = filterResultState;
        this.processingFeedback = Collections.unmodifiableList(processingFeedback);
    }

    public String getMessageId() {
        return messageId;
    }

    public DateTime getDecidedAt() {
        return decidedAt;
    }

    public FilterResultState getFilterResultState() {
        return filterResultState;
    }

    public List<ProcessingFeedback> getProcessingFeedback() {
        return processingFeedback;
    }
}
