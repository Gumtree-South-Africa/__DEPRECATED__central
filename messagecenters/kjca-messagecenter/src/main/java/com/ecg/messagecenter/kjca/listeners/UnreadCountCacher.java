package com.ecg.messagecenter.kjca.listeners;

import com.ecg.messagecenter.core.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.kjca.persistence.UnreadCountCachePopulater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Preemptively populates a cache in Box of the user's unread conversation counts, when they receive a new message.
 */
@Component
public class UnreadCountCacher implements AbstractSimplePostBoxInitializer.PostBoxWriteCallback {
    private final UnreadCountCachePopulater cachePopulater;

    @Autowired
    public UnreadCountCacher(UnreadCountCachePopulater cachePopulater) {
        this.cachePopulater = cachePopulater;
    }

    @Override
    public void success(String email, Long unreadCount, boolean markedAsUnread) {
        this.cachePopulater.populateCache(email);
    }
}
