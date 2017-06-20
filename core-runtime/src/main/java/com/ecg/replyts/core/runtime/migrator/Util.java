package com.ecg.replyts.core.runtime.migrator;

import com.ecg.replyts.core.runtime.indexer.IndexingMode;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class Util {

    public static void waitForCompletion(List<? extends Future> tasks, AtomicInteger counter, Logger log, int completionTimeoutSec) {
        tasks.parallelStream().forEach(t -> {
            try {
                counter.incrementAndGet();
                t.get(completionTimeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException in) {
                log.error("Not completed", in);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("ExecutionException", e);
            } catch (Exception e) {
                log.error("Execution during comparison", e);
            }
        });
    }


    // This method acquires only one lock which is only released by migrateConversationsBetweenDates method once it's done
    static boolean execute(HazelcastInstance hazelcast, ThreadPoolExecutor executor, Runnable runnable, String msg, Logger log) {
        ILock lock = hazelcast.getLock(IndexingMode.MIGRATION.toString());
        try {
            if (lock.tryLock()) {
                executor.execute(runnable);
                log.info("Executing {}", msg);
                return true;
            } else {
                log.info("Skipped execution {} due to no lock", msg);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception while waiting on a lock", e);
            lock.unlock();
            executor.getQueue().clear();
            throw new RuntimeException(e);
        }
    }

}
