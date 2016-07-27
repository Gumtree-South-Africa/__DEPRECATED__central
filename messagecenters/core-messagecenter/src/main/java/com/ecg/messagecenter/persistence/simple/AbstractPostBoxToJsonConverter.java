package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;

public interface AbstractPostBoxToJsonConverter<T extends AbstractConversationThread> {
    String toJson(PostBox<T> p);
}
