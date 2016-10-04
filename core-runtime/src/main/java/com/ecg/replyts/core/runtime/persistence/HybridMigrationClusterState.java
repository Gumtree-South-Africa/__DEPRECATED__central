package com.ecg.replyts.core.runtime.persistence;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class HybridMigrationClusterState {
    private static final Long OBJECT_LOCK_SECONDS = 30L;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * Try to claim a given ID on the Hazelcast cluster for up to 30 seconds. If we're the first to claim it, migrate it.
     * Subsequent attempts to retrieve this object from the primary store (Cassandra) should return again after this.
     *
     * @param clazz
     * @param id
     * @return boolean
     */
    public boolean tryClaim(Class clazz, String id) {
        IMap map = hazelcastInstance.getMap("instances-" + clazz.getName());

        UUID claimId = UUID.randomUUID();
        UUID putId = (UUID) map.putIfAbsent(id, claimId, OBJECT_LOCK_SECONDS, TimeUnit.SECONDS);

        return putId == null || claimId.equals(putId);
    }
}
