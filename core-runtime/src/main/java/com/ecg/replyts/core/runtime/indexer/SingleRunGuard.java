package com.ecg.replyts.core.runtime.indexer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SingleRunGuard {
    private static final Logger LOG = LoggerFactory.getLogger(SingleRunGuard.class);

    @Autowired
    private HazelcastInstance hazelcast;

    public boolean runExclusivelyOrSkip(IndexingMode mode, Runnable runnable) {
        ILock lock = hazelcast.getLock(mode.name());

        try {
            if (lock.tryLock(1L, TimeUnit.SECONDS)) {
                runnable.run();

                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isLocked()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    LOG.error("Failed to release the lock", e);
                }
            }
        }
    }
}
