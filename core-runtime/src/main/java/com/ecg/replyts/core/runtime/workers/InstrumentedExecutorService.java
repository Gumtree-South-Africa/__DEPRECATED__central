package com.ecg.replyts.core.runtime.workers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static com.codahale.metrics.MetricRegistry.name;
import static com.ecg.replyts.core.runtime.TimingReports.*;

/**
 * Decorator for {@link java.util.concurrent.ExecutorService}s to add metrics.
 * <p>
 * In case the executorService is a {@link java.util.concurrent.ThreadPoolExecutor},
 * consider configuring it with a {@link InstrumentedCallerRunsPolicy}.
 */
public class InstrumentedExecutorService implements ExecutorService {

    private final ExecutorService delegate;
    private final String owner;
    private final String name;

    private final Counter submitted;
    private final Counter running;
    private final Counter completed;
    private final Timer runDelay;
    private final Timer duration;

    /**
     * Instrumented executor service with given metrics owner and name.
     * <p>
     * In case the executorService is a {@link java.util.concurrent.ThreadPoolExecutor},
     * consider configuring it with a {@link InstrumentedCallerRunsPolicy}.
     */
    public InstrumentedExecutorService(ExecutorService executorService, String owner, String name) {
        this.delegate = executorService;
        this.owner = owner;
        this.name = name;

        this.submitted = newCounter(mName("submitted"));
        this.running = newCounter(mName("running"));
        this.completed = newCounter(mName("completed"));
        this.runDelay = newTimer(mName("runDelay"));
        this.duration = newTimer(mName("duration"));

        if (executorService instanceof ThreadPoolExecutor) {
            registerThreadPoolGauges((ThreadPoolExecutor) executorService);
        } else if (executorService instanceof ForkJoinPool) {
            registerForkJoinGauges((ForkJoinPool) executorService);
        }
    }

    private void registerForkJoinGauges(ForkJoinPool executorService) {
        newGauge(mName("activeThreadCount"), (Gauge<Integer>) executorService::getActiveThreadCount);
        newGauge(mName("parallelism"), (Gauge<Integer>) executorService::getParallelism);
        newGauge(mName("poolSize"), (Gauge<Integer>) executorService::getPoolSize);
        newGauge(mName("queuedSubmissionCount"), (Gauge<Integer>) executorService::getQueuedSubmissionCount);
        newGauge(mName("queuedTaskCount"), (Gauge<Long>) executorService::getQueuedTaskCount);
        newGauge(mName("stealCount"), (Gauge<Long>) executorService::getStealCount);
        newGauge(mName("runningThreadCountSer"), (Gauge<Integer>) executorService::getRunningThreadCount);
    }

    private void registerThreadPoolGauges(ThreadPoolExecutor executorService) {
        newGauge(mName("maxPoolSize"), (Gauge<Integer>) executorService::getMaximumPoolSize);
        newGauge(mName("activeCount"), (Gauge<Integer>) executorService::getActiveCount);
        newGauge(mName("largestPoolSize"), (Gauge<Integer>) executorService::getLargestPoolSize);
        newGauge(mName("poolSize"), (Gauge<Integer>) executorService::getPoolSize);
        newGauge(mName("taskCount"), (Gauge<Long>) executorService::getTaskCount);
    }

    private String mName(String metricName) {
        return name(this.owner, this.name, metricName);
    }

    @Override
    public void execute(Runnable runnable) {
        submitted.inc();
        delegate.execute(wrap(runnable));
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        submitted.inc();
        return delegate.submit(wrap(runnable));
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T result) {
        submitted.inc();
        return delegate.submit(wrap(runnable), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitted.inc();
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submitted.inc(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        submitted.inc(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        submitted.inc(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        submitted.inc(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumented.add(wrap(task));
        }
        return instrumented;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return delegate.awaitTermination(l, timeUnit);
    }

    public ExecutorService getDelegate() {
        return delegate;
    }

    protected InstrumentedRunnable wrap(Runnable runnable) {
        return new InstrumentedRunnable(runnable);
    }

    private final class InstrumentedRunnable implements Runnable {

        private final long start;
        private final Runnable task;

        private InstrumentedRunnable(Runnable task) {
            this.start = System.nanoTime();
            this.task = task;
        }

        @Override
        public void run() {
            runDelay.update(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            running.inc();
            final Timer.Context context = duration.time();
            try {
                task.run();
            } finally {
                context.stop();
                running.dec();
                completed.inc();
            }
        }
    }

    protected <T> InstrumentedCallable<T> wrap(Callable<T> task) {
        return new InstrumentedCallable<T>(task);
    }

    private final class InstrumentedCallable<T> implements Callable<T> {

        private final long start;
        private final Callable<T> callable;

        private InstrumentedCallable(Callable<T> callable) {
            this.start = System.nanoTime();
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            runDelay.update(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            running.inc();
            final Timer.Context context = duration.time();
            try {
                return callable.call();
            } finally {
                context.stop();
                running.dec();
                completed.inc();
            }
        }
    }
}