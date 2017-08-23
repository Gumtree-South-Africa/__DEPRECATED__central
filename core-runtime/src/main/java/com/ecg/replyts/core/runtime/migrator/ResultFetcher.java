package com.ecg.replyts.core.runtime.migrator;

import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


public class ResultFetcher {

    private final int completionTimeoutSec;
    private final AtomicInteger counter;
    private final Logger log;

    public ResultFetcher(int completionTimeoutSec, AtomicInteger processedBatchCounter, Logger log) {
        this.completionTimeoutSec = completionTimeoutSec;
        this.counter = processedBatchCounter;
        this.log = log;
    }

    public void waitForCompletion(List<Future> tasks) {
        tasks.parallelStream().forEach(t -> {
            try {
                counter.incrementAndGet();
                t.get(completionTimeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException in) {
                log.warn("Interrupted while waiting for future completion");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("ExecutionException", e);
            } catch (TimeoutException e) {
                log.error("Timed-out while waiting for future completion", e);
            } catch (Exception e) {
                log.error("Encountered execution", e);
            }
        });
    }
}
