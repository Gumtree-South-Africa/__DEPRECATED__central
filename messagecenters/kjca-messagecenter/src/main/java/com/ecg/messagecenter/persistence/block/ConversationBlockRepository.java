package com.ecg.messagecenter.persistence.block;

import org.joda.time.DateTime;

public interface ConversationBlockRepository {
    ConversationBlock byConversationId(String conversationId);

    void write(ConversationBlock conversationBlock);

    void cleanupOldConversationBlocks(DateTime deleteBlocksBefore);
}
