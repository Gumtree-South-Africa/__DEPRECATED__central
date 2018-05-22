package com.ecg.replyts.core.runtime.indexer;

import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Util {

    static void waitForCompletion(List<? extends Future> tasks, AtomicInteger counter, Logger log, int completionTimeoutSec) {
        tasks.parallelStream().forEach(t -> {
            try {
                counter.incrementAndGet();
                t.get(completionTimeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException in) {
                log.warn("Interrupted");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("ExecutionException", e);
            } catch (Exception e) {
                log.error("Execution during comparison", e);
            }
        });
    }
}
