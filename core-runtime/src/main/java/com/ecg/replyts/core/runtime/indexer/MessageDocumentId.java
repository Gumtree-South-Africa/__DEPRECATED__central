package com.ecg.replyts.core.runtime.indexer;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.util.Iterator;

/**
 * wrapper for message document format (in elastic search) that is able to generate message id's (based on conversation and message id)
 * and retrieve them from the index again.
 *
 * @author mhuttar
 */
public class MessageDocumentId {
    private final String conversationId;
    private final String messageId;

    private static final Splitter SLASH_SLITTER = Splitter.on('/').limit(2);

    private static final Joiner SLASH_JOINER = Joiner.on('/');

    public MessageDocumentId(String conversationId, String messageId) {
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(messageId);
        this.conversationId = conversationId;
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public static final MessageDocumentId parse(String completeId) {
        Iterator<String> split = SLASH_SLITTER.split(completeId).iterator();
        return new MessageDocumentId(split.next(), split.next());
    }

    public String build() {
        return SLASH_JOINER.join(conversationId, messageId);
    }

    @Override
    public int hashCode() {
        return build().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return build().equals(((MessageDocumentId) o).build());
    }
}