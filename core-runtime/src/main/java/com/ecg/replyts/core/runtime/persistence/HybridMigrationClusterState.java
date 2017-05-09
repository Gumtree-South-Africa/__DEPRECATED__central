package com.ecg.replyts.core.runtime.persistence;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class HybridMigrationClusterState {

    private final long objectLockSeconds;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    public HybridMigrationClusterState(@Value("migration.object.lock.duration.seconds:30") String object_lock) {
        this.objectLockSeconds = Long.parseLong(object_lock);
    }

    public HybridMigrationClusterState() {
        this.objectLockSeconds = 30L;
    }
    /**
     * Try to claim a given ID on the Hazelcast cluster for up to #objectLockSeconds seconds. If we're the first to claim it, migrate it.
     * Subsequent attempts to retrieve this object from the primary store (Cassandra) should return again after this.
     */
    public boolean tryClaim(Class clazz, String id) {
        IMap map = hazelcastInstance.getMap("instances-" + clazz.getName());

        UUID claimId = UUID.randomUUID();
        UUID putId = (UUID) map.putIfAbsent(id, claimId, objectLockSeconds, TimeUnit.SECONDS);

        return putId == null || claimId.equals(putId);
    }
}
