package com.ecg.messagecenter.core.persistence.simple;

import com.ecg.messagecenter.core.persistence.AbstractConversationThread;

public interface AbstractPostBoxToJsonConverter<T extends AbstractConversationThread> {
    String toJson(PostBox<T> p);
}
