package com.ecg.replyts.core.runtime.indexer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

class SingleRunGuard {
    private final HazelcastInstance hazelcast;

    @Autowired
    SingleRunGuard(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    boolean runExclusivelyOrSkip(IndexingMode mode, Runnable runnable) {
        ILock lock = hazelcast.getLock(mode.name());
        boolean tryLock;
        try {
            tryLock = lock.tryLock(1l, TimeUnit.SECONDS);
            if (tryLock) {
                runnable.run();
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

}
