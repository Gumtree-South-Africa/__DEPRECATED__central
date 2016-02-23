package com.ecg.messagecenter.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by pragone
 * Created on 18/10/15 at 5:59 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public class PostBoxUnreadInfo {

    private final Map<String, Integer> unreadMap;

    public PostBoxUnreadInfo(Map<String, Integer> unreadMap) {
        this.unreadMap = Collections.unmodifiableMap(unreadMap);
    }

    public int getNumUnreadConversations() {
        return unreadMap.size();
    }

    public int getNumUnreadMessages() {
        final AtomicInteger acum = new AtomicInteger(0);
        unreadMap.forEach((key, val) -> acum.addAndGet(val));
        return acum.get();
    }

    public int getNumUnreadMessages(String conversation_id) {
        return unreadMap.getOrDefault(conversation_id, 0);
    }

    public Map<String, Integer> getUnreadMap() {
        return unreadMap;
    }
}
