package com.ecg.replyts.core.runtime.cluster;

import com.datastax.driver.core.utils.UUIDs;

/**
 * Owns logic to create globally unique conversation ids
 */
public class ConversationGUID {
    public static String next() {
        return UUIDs.timeBased().toString();
    }
}
