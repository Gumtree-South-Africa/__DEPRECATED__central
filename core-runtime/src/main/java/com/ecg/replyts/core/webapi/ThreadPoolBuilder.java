package com.ecg.replyts.core.webapi;

import com.ecg.replyts.core.runtime.MetricsService;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class ThreadPoolBuilder {
    // We want to keep the Jetty defaults for the other values
    private static final int MIN_THREADS_JETTY_DEFAULT = 8;
    private static final int IDLE_TIMEOUT_JETTY_DEFAULT = 60000;

    private int maxThreads;
    private int maxThreadQueueSize;
    private boolean instrumented;

    /**
     * The maximum processing threads
     */
    public ThreadPoolBuilder withMaxThreads(int value) {
        maxThreads = value;
        return this;
    }

    /**
     * The size of the queue
     */
    public ThreadPoolBuilder withQueueSize(int value) {
        maxThreadQueueSize = value;
        return this;
    }

    public ThreadPoolBuilder withInstrumentation(boolean instrumented) {
        this.instrumented = instrumented;
        return this;
    }

    /**
     * @return The thread pool configured with the given values.
     */
    public QueuedThreadPool build() {
        BlockingArrayQueue<Runnable> queue = new BlockingArrayQueue<>(maxThreadQueueSize);

        QueuedThreadPool threadPool;
        if (instrumented) {
            threadPool = new HostReportingInstrumentedQueuedTPool(MetricsService.getInstance().getRegistry(),
                    maxThreads, MIN_THREADS_JETTY_DEFAULT, IDLE_TIMEOUT_JETTY_DEFAULT, queue);
        } else {
            threadPool = new QueuedThreadPool(maxThreads, MIN_THREADS_JETTY_DEFAULT, IDLE_TIMEOUT_JETTY_DEFAULT, queue);
        }
        threadPool.setName("ThreadPool");
        return threadPool;
    }
}