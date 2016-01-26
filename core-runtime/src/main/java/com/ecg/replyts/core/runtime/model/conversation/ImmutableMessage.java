package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.app.textcleanup.ExtractedText;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.*;

public class ImmutableMessage implements Message {

    private final String id;
    private final MessageDirection messageDirection;
    private final MessageState state;
    private final DateTime receivedAt;
    private final DateTime lastModifiedAt;
    private final String senderMessageIdHeader;
    private final String inResponseToMessageId;
    private final FilterResultState filterResultState;
    private final ModerationResultState humanResultState;
    private final Map<String, String> headers;
    private final String plainTextBody;
    private List<String> attachments;
    private final List<ProcessingFeedback> processingFeedback;

    private final Optional<String> lastEditor;
    private final int version;

    ImmutableMessage(Builder bdr) {
        Preconditions.checkNotNull(bdr.messageDirection);
        Preconditions.checkNotNull(bdr.state);
        Preconditions.checkNotNull(bdr.receivedAt);
        Preconditions.checkNotNull(bdr.lastModifiedAt);
        Preconditions.checkNotNull(bdr.filterResultState);
        Preconditions.checkNotNull(bdr.humanResultState);
        Preconditions.checkNotNull(bdr.headers);
        Preconditions.checkNotNull(bdr.plainTextBody);
        Preconditions.checkNotNull(bdr.processingFeedback);
        Preconditions.checkNotNull(bdr.lastEditor);

        this.version = bdr.version;
        this.id = bdr.id;
        this.messageDirection = bdr.messageDirection;
        this.state = bdr.state;
        this.receivedAt = bdr.receivedAt;
        this.lastModifiedAt = bdr.lastModifiedAt;
        this.senderMessageIdHeader = bdr.senderMessageIdHeader;
        this.inResponseToMessageId = bdr.inResponseToMessageId;
        this.filterResultState = bdr.filterResultState;
        this.humanResultState = bdr.humanResultState;
        this.headers = ImmutableMap.copyOf(bdr.headers);
        this.plainTextBody = bdr.plainTextBody;
        this.processingFeedback = ImmutableList.copyOf(bdr.processingFeedback);
        this.lastEditor = bdr.lastEditor;
        this.attachments = bdr.attachments!= null ? ImmutableList.copyOf(bdr.attachments) : Collections.<String>emptyList();
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    @Override
    public MessageState getState() {
        return state;
    }

    @Override
    public DateTime getReceivedAt() {
        return receivedAt;
    }

    @Override
    public DateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public String getSenderMessageIdHeader() {
        return senderMessageIdHeader;
    }

    @Override
    public String getInResponseToMessageId() {
        return inResponseToMessageId;
    }

    @Override
    public FilterResultState getFilterResultState() {
        return filterResultState;
    }

    @Override
    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getPlainTextBody() {
        return plainTextBody;
    }

    @Override
    public List<ProcessingFeedback> getProcessingFeedback() {
        return processingFeedback;
    }

    @Override
    public List<String> getAttachmentFilenames() {
        return attachments;
    }

    @Override
    @Deprecated
    public String getPlainTextBodyDiff(Conversation conversation) {
        return ExtractedText.getNewText(this).in(conversation);
    }

    @Override
    public Optional<String> getLastEditor() {
        return lastEditor;
    }

    public static final class Builder {
        private int version = 0;
        private String id;
        private MessageDirection messageDirection;
        private MessageState state = MessageState.UNDECIDED;
        private DateTime receivedAt;
        private DateTime lastModifiedAt;
        private FilterResultState filterResultState = FilterResultState.OK;
        private ModerationResultState humanResultState = ModerationResultState.UNCHECKED;
        private Map<String, String> headers = new HashMap<String, String>();
        private String plainTextBody;
        private List<ProcessingFeedback> processingFeedback = new ArrayList<ProcessingFeedback>();
        private Optional<String> lastEditor = Optional.absent();
        private String senderMessageIdHeader;
        private String inResponseToMessageId;
        public List<String> attachments;

        private Builder() {
        }

        public static Builder aMessage() {
            return new Builder();
        }

        public static Builder aMessage(Message message) {

            Builder builder = new Builder().
                    withId(message.getId()).
                    withMessageDirection(message.getMessageDirection()).
                    withState(message.getState()).
                    withReceivedAt(message.getReceivedAt()).
                    withLastModifiedAt(message.getLastModifiedAt()).
                    withFilterResultState(message.getFilterResultState()).
                    withHumanResultState(message.getHumanResultState()).
                    withHeaders(message.getHeaders()).
                    withLastEditor(message.getLastEditor()).
                    withInResponseToMessageId(message.getInResponseToMessageId()).
                    withSenderMessageIdHeader(message.getSenderMessageIdHeader()).
                    withAttachments(message.getAttachmentFilenames()).
                    withProcessingFeedback(message.getProcessingFeedback());


            builder.version= message.getVersion()+1;
            return builder;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withLastEditor(Optional<String> lastEditor) {
            Preconditions.checkNotNull(lastEditor);
            this.lastEditor = lastEditor;
            return this;
        }

        public Builder withMessageDirection(MessageDirection messageDirection) {
            this.messageDirection = messageDirection;
            return this;
        }

        public Builder withState(MessageState state) {

            this.state = state;
            return this;
        }

        public Builder withReceivedAt(DateTime receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public Builder withLastModifiedAt(DateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder withFilterResultState(FilterResultState filterResultState) {
            this.filterResultState = filterResultState;
            return this;
        }

        public Builder withAttachments(List<String> attachments) {
            Preconditions.checkNotNull(attachments);
            this.attachments = attachments;
            return this;
        }

        public Builder withHumanResultState(ModerationResultState humanResultState) {
            this.humanResultState = humanResultState;
            return this;
        }

        public Builder withProcessingFeedback(List<ProcessingFeedback> processingFeedback) {
            if (processingFeedback != null && !processingFeedback.isEmpty()) {
                this.processingFeedback.addAll(processingFeedback);
            }
            return this;
        }

        public Builder withProcessingFeedback(ProcessingFeedbackBuilder processingFeedbackBuilder) {
            this.processingFeedback.add(processingFeedbackBuilder.build());
            return this;
        }

        public Builder withHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder withPlainTextBody(String plainTextBody) {
            this.plainTextBody = plainTextBody;
            return this;
        }

        public Builder withSenderMessageIdHeader(String senderMessageIdHeader) {
            this.senderMessageIdHeader = senderMessageIdHeader;
            return this;
        }

        public Builder withInResponseToMessageId(String inResponseToMessageId) {
            this.inResponseToMessageId = inResponseToMessageId;
            return this;
        }

        public Builder addHeaders(Map<String, String> additionalHeaders) {
            for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
                this.headers.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder withHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Message build() {
            return new ImmutableMessage(this);
        }

    }
}
