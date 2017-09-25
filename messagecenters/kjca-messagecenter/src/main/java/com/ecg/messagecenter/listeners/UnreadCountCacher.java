package com.ecg.messagecenter.listeners;

import com.ecg.messagecenter.persistence.UnreadCountCachePopulater;
import com.ecg.messagecenter.persistence.simple.AbstractSimplePostBoxInitializer;
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
