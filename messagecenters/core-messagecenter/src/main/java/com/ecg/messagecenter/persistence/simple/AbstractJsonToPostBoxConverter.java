package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;

public interface AbstractJsonToPostBoxConverter<T extends AbstractConversationThread> {
    PostBox<T> toPostBox(String key, String jsonString, int maxAgeDays);
}
