package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.app.textcleanup.ExtractedText;
import com.ecg.replyts.core.api.model.conversation.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
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
    private final List<String> attachments;
    private final List<String> textParts;
    private final List<ProcessingFeedback> processingFeedback;

    private final String lastEditor;
    private final int version;

    private UUID eventTimeUUID;

    ImmutableMessage(Builder bdr) {
        Preconditions.checkNotNull(bdr.messageDirection);
        Preconditions.checkNotNull(bdr.state);
        Preconditions.checkNotNull(bdr.receivedAt);
        Preconditions.checkNotNull(bdr.lastModifiedAt);
        Preconditions.checkNotNull(bdr.filterResultState);
        Preconditions.checkNotNull(bdr.humanResultState);
        Preconditions.checkNotNull(bdr.headers);
        Preconditions.checkNotNull(bdr.processingFeedback);

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
        // ImmutableSortedMap.copyOf() never returns null even though this is not documented anywhere
        this.headers = ImmutableSortedMap.copyOf(bdr.headers, String.CASE_INSENSITIVE_ORDER);
        this.processingFeedback = ImmutableList.copyOf(bdr.processingFeedback);
        this.lastEditor = bdr.lastEditor;
        this.attachments = bdr.attachments != null ? ImmutableList.copyOf(bdr.attachments) : Collections.emptyList();
        this.textParts = bdr.textParts != null ? ImmutableList.copyOf(bdr.textParts) : Collections.emptyList();
        this.plainTextBody = this.textParts.isEmpty() ? "" : this.textParts.get(0);
        this.eventTimeUUID = bdr.eventTimeUUID;
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
    public Map<String, String> getCaseInsensitiveHeaders() {
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
        return Optional.ofNullable(lastEditor);
    }

    @Override
    public UUID getEventTimeUUID() {
        return eventTimeUUID;
    }

    @Override
    public List<String> getTextParts() {
        return textParts;
    }

    @Override
    public String toString() {
        return "ImmutableMessage{" +
                "id='" + id + '\'' +
                ", messageDirection=" + messageDirection +
                ", state=" + state +
                ", receivedAt=" + receivedAt +
                ", lastModifiedAt=" + lastModifiedAt +
                ", senderMessageIdHeader='" + senderMessageIdHeader + '\'' +
                ", inResponseToMessageId='" + inResponseToMessageId + '\'' +
                ", filterResultState=" + filterResultState +
                ", humanResultState=" + humanResultState +
                ", headers=" + headers +
                ", plainTextBody='" + plainTextBody + '\'' +
                ", attachments=" + attachments +
                ", textParts=" + textParts +
                ", processingFeedback=" + processingFeedback +
                ", lastEditor=" + lastEditor +
                ", version=" + version +
                ", eventTimeUUID=" + eventTimeUUID +
                '}';
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
        private Map<String, String> headers = new HashMap<>();
        private List<ProcessingFeedback> processingFeedback = new ArrayList<>();
        private String lastEditor;
        private UUID eventTimeUUID;
        private String senderMessageIdHeader;
        private String inResponseToMessageId;
        private List<String> attachments;
        private List<String> textParts;

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
                    withHeaders(message.getCaseInsensitiveHeaders()).
                    withLastEditor(message.getLastEditor().orElse(null)).
                    withInResponseToMessageId(message.getInResponseToMessageId()).
                    withSenderMessageIdHeader(message.getSenderMessageIdHeader()).
                    withAttachments(message.getAttachmentFilenames()).
                    withProcessingFeedback(message.getProcessingFeedback()).
                    withEventTimeUUID(message.getEventTimeUUID());

            builder.version = message.getVersion() + 1;
            return builder;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withLastEditor(String lastEditor) {
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

        public Builder withTextParts(List<String> textParts) {
            this.textParts = textParts;
            return this;
        }

        public Builder withEventTimeUUID(UUID eventTimeUUID) {
            this.eventTimeUUID = eventTimeUUID;
            return this;
        }

        public Message build() {
            return new ImmutableMessage(this);
        }
    }
}