package com.ecg.replyts.core.runtime.migrator;

import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


public class Util {

    public static void waitForCompletion(List<Future> tasks, AtomicInteger counter, Logger log) {
        tasks.parallelStream().forEach(t -> {
            try {
                counter.incrementAndGet();
                t.get();
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
}
