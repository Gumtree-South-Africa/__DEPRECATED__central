package com.ecg.messagecenter.core.persistence.simple;

import com.ecg.messagecenter.core.persistence.AbstractConversationThread;

public interface AbstractJsonToPostBoxConverter<T extends AbstractConversationThread> {
    PostBox<T> toPostBox(String key, String jsonString);
}
