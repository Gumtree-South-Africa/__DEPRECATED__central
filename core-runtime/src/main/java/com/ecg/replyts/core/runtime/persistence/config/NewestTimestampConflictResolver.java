package com.ecg.replyts.core.runtime.persistence.config;

import com.basho.riak.client.cap.ConflictResolver;

import java.util.Collection;

/**
 * compares conflicting configuration versions based on the timestamp in milliseconds they were inserted to and picks the newest one.
 * if timestamps are pairsAreEqual, a random one is picked.
 */
public class NewestTimestampConflictResolver implements ConflictResolver<Configurations> {

    @Override
    public Configurations resolve(Collection<Configurations> siblings) {
        if (siblings.isEmpty()) {
            return null;
        }
        long maxTs = -1;
        Configurations latestVersion = null;
        for (Configurations c : siblings) {
            if (c.getTimestamp() > maxTs) {
                latestVersion = c;
                maxTs = c.getTimestamp();
            }
        }
        return latestVersion;
    }
}
