package com.ecg.replyts.core.runtime.indexer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

public class SingleRunGuard {

    private static final Logger LOG = LoggerFactory.getLogger(SingleRunGuard.class);
    private final HazelcastInstance hazelcast;

    @Autowired
    SingleRunGuard(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    public boolean runExclusivelyOrSkip(IndexingMode mode, Runnable runnable) {
        ILock lock = hazelcast.getLock(mode.name());
        boolean tryLock;
        try {
            tryLock = lock.tryLock(1L, TimeUnit.SECONDS);
            if (tryLock) {
                runnable.run();
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting on a lock", e);
            throw new RuntimeException(e);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    public boolean isExecuting(IndexingMode mode) {
        return hazelcast.getLock(mode.name()).isLocked();
    }

}
