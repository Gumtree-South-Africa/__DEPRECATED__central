package com.ecg.replyts.core.api.persistence;

import com.google.common.base.Joiner;

/**
 * The key to identify an object in the ConversationIndexBucket by from-email (buyer), to-email (seller) and adID.
 *
 */
public class ConversationIndexKey {
    private final String from;
    private final String to;
    private final String adId;

    private static final Joiner ELEMENT_JOINER = Joiner.on("|");

    public ConversationIndexKey(String from, String to, String adId) {
        this.from = from;
        this.to = to;
        this.adId = adId;
    }

    public String serialize() {
        return ELEMENT_JOINER.join(from.toLowerCase(), to.toLowerCase(), adId);
    }

    @Override
    public int hashCode() {
        return serialize().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ConversationIndexKey) {
            return serialize().equals(((ConversationIndexKey) obj).serialize());
        }
        return false;
    }
}
