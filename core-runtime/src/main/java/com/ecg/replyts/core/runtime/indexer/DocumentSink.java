package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.Conversation;

import java.util.List;

public interface DocumentSink {
    void sink(List<Conversation> conversations);

    void sink(Conversation conversation);

    void sink(Conversation conversation, String messageId);
}
