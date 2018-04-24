package com.ecg.messagecenter.kjca.persistence.block;

import org.joda.time.DateTime;

import java.util.List;

public interface ConversationBlockRepository {
    ConversationBlock byId(String conversationId);

    void write(ConversationBlock conversationBlock);

    void cleanup(DateTime deleteBlocksBefore);

    List<String> getIds();
}
