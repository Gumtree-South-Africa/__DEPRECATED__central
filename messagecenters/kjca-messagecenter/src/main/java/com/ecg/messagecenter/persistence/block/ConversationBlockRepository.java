package com.ecg.messagecenter.persistence.block;

import org.joda.time.DateTime;

public interface ConversationBlockRepository {
    ConversationBlock byId(String conversationId);

    void write(ConversationBlock conversationBlock);

    void cleanup(DateTime deleteBlocksBefore);
}
