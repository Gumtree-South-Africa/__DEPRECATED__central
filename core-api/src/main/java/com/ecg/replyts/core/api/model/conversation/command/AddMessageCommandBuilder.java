package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link AddMessageCommand}.
 */
public final class AddMessageCommandBuilder {
    private String conversationId;
    private String messageId;
    private MessageDirection messageDirection;
    private DateTime receivedAt = new DateTime();
    private MessageState state = MessageState.UNDECIDED;
    private String senderMessageIdHeader;
    private List<String> attachmentFilenames = Collections.emptyList();
    private String inResponseToMessageId;
    private Map<String, String> headers = new HashMap<>();
    private List<String> textParts = Collections.emptyList();

    private AddMessageCommandBuilder() {
    }

    public static AddMessageCommandBuilder anAddMessageCommand(String conversationId, String messageId) {
        AddMessageCommandBuilder result = new AddMessageCommandBuilder();
        result.conversationId = conversationId;
        result.messageId = messageId;
        return result;
    }

    public AddMessageCommandBuilder withMessageDirection(MessageDirection messageDirection) {
        this.messageDirection = messageDirection;
        return this;
    }

    public AddMessageCommandBuilder withAttachmentFilenames(List<String> attachments) {
        Preconditions.checkNotNull(attachments);
        this.attachmentFilenames = attachments;
        return this;
    }

    public AddMessageCommandBuilder withReceivedAt(DateTime receivedAt) {
        this.receivedAt = receivedAt;
        return this;
    }

    public AddMessageCommandBuilder withSenderMessageIdHeader(String senderMessageIdHeader) {
        this.senderMessageIdHeader = senderMessageIdHeader;
        return this;
    }

    public AddMessageCommandBuilder withInResponseToMessageId(String inResponseToMessageId) {
        this.inResponseToMessageId = inResponseToMessageId;
        return this;
    }

    public AddMessageCommandBuilder withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public AddMessageCommandBuilder addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public AddMessageCommandBuilder withTextParts(List<String> textParts) {
        this.textParts = textParts;
        return this;
    }

    public AddMessageCommand build() {
        return new AddMessageCommand(
            conversationId, messageId, state, messageDirection, receivedAt, senderMessageIdHeader,
                inResponseToMessageId, headers, attachmentFilenames, textParts
        );
    }
}
