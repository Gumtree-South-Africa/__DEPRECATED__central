package com.ecg.replyts.core.runtime.persistence.conversation;

import com.google.common.base.Joiner;

class ConversationResumeIndexValueBuilder {

    private static final Joiner ELEMENT_JOINER = Joiner.on("|");
    private final String indexValue;

    public ConversationResumeIndexValueBuilder(String from, String to, String adId) {
        indexValue = ELEMENT_JOINER.join(from.toLowerCase(), to.toLowerCase(), adId);
    }

    public String getIndexValue() {
        return indexValue;
    }
}
