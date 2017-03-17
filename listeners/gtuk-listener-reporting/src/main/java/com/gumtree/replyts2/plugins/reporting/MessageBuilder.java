package com.gumtree.replyts2.plugins.reporting;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import org.joda.time.DateTime;

import java.util.*;

public class MessageBuilder {

    private String messageId;

    private MessageDirection messageDirection;

    private MessageState messageState;

    private DateTime messageReceivedAt;

    private FilterResultState filterResultState;

    private ModerationResultState moderationResultState;

    private List<ProcessingFeedback> processingFeedbacks = new ArrayList<>();

    private DateTime lastModifiedAt;

    public MessageBuilder messageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public MessageBuilder messageDirection(MessageDirection messageDirection) {
        this.messageDirection = messageDirection;
        return this;
    }

    public MessageBuilder messageState(MessageState messageState) {
        this.messageState = messageState;
        return this;
    }

    public MessageBuilder messageReceivedAt(DateTime messageReceivedAt) {
        this.messageReceivedAt = messageReceivedAt;
        return this;
    }

    public MessageBuilder messageLastModifiedAt(DateTime messageLastModifiedAt) {
        this.lastModifiedAt = messageLastModifiedAt;
        return this;
    }

    public MessageBuilder filterResultState(FilterResultState filterResultState) {
        this.filterResultState = filterResultState;
        return this;
    }

    public MessageBuilder humanResultState(ModerationResultState moderationResultState) {
        this.moderationResultState = moderationResultState;
        return this;
    }

    public MessageBuilder addProcessingFeedback(ProcessingFeedbackBuilder processingFeedback) {
        processingFeedbacks.add(processingFeedback.createProcessingFeedback());
        return this;
    }

    public MessageBuilder addProcessingFeedback(ProcessingFeedback processingFeedback) {
        processingFeedbacks.add(processingFeedback);
        return this;
    }

    public Message createMessage() {
        return ImmutableMessage.Builder.aMessage()
                .withId(messageId)
                .withMessageDirection(messageDirection)
                .withState(messageState)
                .withReceivedAt(messageReceivedAt)
                .withHumanResultState(moderationResultState)
                .withFilterResultState(filterResultState)
                .withProcessingFeedback(processingFeedbacks)
                .addHeaders(new HashMap<String, String>())
                .withTextParts(Arrays.asList(""))
                .withLastEditor(Optional.empty())
                .withLastModifiedAt(lastModifiedAt)
                .build();
    }
}