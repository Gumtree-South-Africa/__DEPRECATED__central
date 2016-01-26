package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Command to add a message to a {@link com.ecg.replyts.core.api.model.conversation.Conversation}.
 */
public class AddMessageCommand extends ConversationCommand {
    private final String messageId;
    private final MessageState state;
    private final MessageDirection messageDirection;
    private final DateTime receivedAt;
    private final String senderMessageIdHeader;
    private final String inResponseToMessageId;
    private final Map<String, String> headers;
    private final String plainTextBody;
    private final List<String> attachments;

    public AddMessageCommand(String conversationId, String messageId, MessageState state, MessageDirection messageDirection, DateTime receivedAt, String senderMessageIdHeader, String inResponseToMessageId, Map<String, String> headers, String plainTextBody, List<String> attachments) {
        super(conversationId);

        if (messageDirection == null) throw new IllegalArgumentException();
        if (receivedAt == null) throw new IllegalArgumentException();
        if (state == null) throw new IllegalArgumentException();
        this.attachments = attachments;
        this.state = state;
        this.messageId = messageId;
        this.messageDirection = messageDirection;
        this.receivedAt = receivedAt;
        this.senderMessageIdHeader = senderMessageIdHeader;
        this.inResponseToMessageId = inResponseToMessageId;
        this.headers = Collections.unmodifiableMap(headers);
        this.plainTextBody = plainTextBody;
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

    public String getSenderMessageIdHeader() {
        return senderMessageIdHeader;
    }

    public String getInResponseToMessageId() {
        return inResponseToMessageId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public MessageState getState() {
        return state;
    }

    public String getPlainTextBody() {
        return plainTextBody;
    }

    public List<String> getAttachments() {
        return attachments;
    }
}
