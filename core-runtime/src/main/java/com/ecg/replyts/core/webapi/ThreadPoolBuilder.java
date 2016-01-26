package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder for the Jetty thread pool.
 * <p/>
 * We want to provide configuration options for max threads and queue size. Keep complexity for this configuration in this class.
 */
public class ThreadPoolBuilder {

    // We want to keep the Jetty defaults for the other values
    private static final int MAX_THREADS_JETTY_DEFAULT = 200;
    private static final int MIN_THREADS_JETTY_DEFAULT = 8;
    private static final int IDLE_TIMEOUT_JETTY_DEFAULT = 60000;

    private Optional<Integer> maxThreads;
    private Optional<Integer> maxThreadQueueSize;

    /**
     * The maximum processing threads
     */
    public ThreadPoolBuilder withMaxThreads(Optional<Integer> value) {
        maxThreads = value;
        return this;
    }

    /**
     * The size of the queue
     */
    public ThreadPoolBuilder withQueueSize(Optional<Integer> value) {
        maxThreadQueueSize = value;
        return this;
    }

    /**
     * @return The thread pool configured with the given values.
     */
    public QueuedThreadPool build() {

        checkNotNull(maxThreads);
        checkNotNull(maxThreadQueueSize);

        BlockingArrayQueue<Runnable> queue = maxThreadQueueSize.isPresent() ? new BlockingArrayQueue<>(maxThreadQueueSize.get()) : new BlockingArrayQueue<>();
        int maxThreads = this.maxThreads.orElse(MAX_THREADS_JETTY_DEFAULT);

        return new QueuedThreadPool(maxThreads, MIN_THREADS_JETTY_DEFAULT, IDLE_TIMEOUT_JETTY_DEFAULT, queue);
    }
}
