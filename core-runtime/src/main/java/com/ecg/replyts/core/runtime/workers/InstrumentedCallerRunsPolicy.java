package com.ecg.replyts.core.runtime.workers;

import com.codahale.metrics.Counter;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static com.codahale.metrics.MetricRegistry.name;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

/**
 * A handler for rejected tasks that runs the rejected task
 * directly in the calling thread of the execute method.
 * When the executor has been shut down, a
 * [[RejectedExecutionException]] is thrown.
 * <p>
 * In addition, rejected tasks are counted.
 */
public class InstrumentedCallerRunsPolicy implements RejectedExecutionHandler {

    private final Counter callerRunsCounter;

    /**
     * Instrumented called runs policy with given metrics owner and name.
     */
    public InstrumentedCallerRunsPolicy(String owner, String name) {
        this.callerRunsCounter = newCounter(name(owner, name, "callerRunCount"));
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        callerRunsCounter.inc();
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Shutdown");
        } else {
            runnable.run();
        }
    }
}