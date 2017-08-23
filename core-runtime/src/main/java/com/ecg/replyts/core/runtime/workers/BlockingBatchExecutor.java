package com.ecg.replyts.core.runtime.workers;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor.ErrorHandlingPolicy.SKIP_ERRORS;
import static java.lang.String.format;

/**
 * utility for running batch jobs concurrently. <strong>Not threadsafe</strong> this executor will launch a thread pool and work on a list of data objects that are converted to runnables by a given (specificed) function:
 * <pre>
 * conversationDeleterBatch.executeAll(conversationIdsToDelete, new Function&lt;String, Runnable&gt;() {
 *  @Override
 *  public Runnable apply(final  String conversationId) {
 *      return new Runnable() {
 *          @Override public void run() {
 *              service.deleteById(conversationId);
 *          }
 *      };
 *  }
 * });
 * </pre>
 */
public class BlockingBatchExecutor<T> {
    private final String taskName;
    private final int threadCount;
    private final int maxDurationTime;
    private final TimeUnit maxDurationTimeUnit;

    private static final Logger LOG = LoggerFactory.getLogger(BlockingBatchExecutor.class);

    public BlockingBatchExecutor(String taskName, int threadCount, int maxDurationTime, TimeUnit maxDurationTimeUnit) {
        this.taskName = taskName;
        this.threadCount = threadCount;
        this.maxDurationTime = maxDurationTime;
        this.maxDurationTimeUnit = maxDurationTimeUnit;
    }

    /**
     * converts the given input into runnables that will be executed in a pool using the specified conversation function. blocks until all input has been processed.
     */
    public void executeAll(Iterable<T> input, Function<T, Runnable> transformerFunction) {
        executeAll(input, transformerFunction, SKIP_ERRORS);
    }

    /**
     * converts the given input into runnables that will be executed in a pool using the specified conversation function. blocks until all input has been processed.
     */
    public void executeAll(Iterable<T> input, Function<T, Runnable> transformerFunction, ErrorHandlingPolicy errorHandlingPolicy) {


        FailFastCapablePoolExecutor executorService = new FailFastCapablePoolExecutor(threadCount, taskName, errorHandlingPolicy);
        for (T data : input) {
            executorService.execute(transformerFunction.apply(data));
        }

        shutdownAndAwaitTermination(executorService);

        if (executorService.failed()) {
            throw new BatchFailedException("Bulk Execution failed due to exception, please see logs");
        }
    }


    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(maxDurationTime, maxDurationTimeUnit)) {
                LOG.error("Cronjob {} took over {} {}. Cancelling Execution (the next run will continue to clean up where this one stopped)", taskName, maxDurationTime, maxDurationTimeUnit);
                pool.shutdownNow();
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new RuntimeException(format("Job %s died and may still be running. It ran longer than %d %s", taskName, maxDurationTime, maxDurationTimeUnit));
                }
            }
        } catch (InterruptedException ie) {
            LOG.warn("interrupted while waiting for the pool to be shut down");
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public enum ErrorHandlingPolicy {
        SKIP_ERRORS,
        FAIL_FAST_ON_ERRORS;
    }


    private static class FailFastCapablePoolExecutor extends ThreadPoolExecutor {
        private final ErrorHandlingPolicy policy;
        private final AtomicBoolean jobFailed = new AtomicBoolean(false);

        private FailFastCapablePoolExecutor(int threadCount, String taskName, ErrorHandlingPolicy policy) {
            super(threadCount, threadCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat(taskName + "-%s").build());
            this.policy = policy;
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            if (throwable == null) {
                return;
            }

            if (policy == SKIP_ERRORS) {
                LOG.info("Batch Executor: Error on processing Chunk. Skipping it", throwable);
            } else {
                LOG.error("Batch Executor: Error on processing Chunk. Failing Batch and cancelling execution", throwable);
                jobFailed.set(true);
                shutdownNow();
            }
        }

        public boolean failed() {
            return jobFailed.get();
        }
    }

    public static class BatchFailedException extends RuntimeException {
        public BatchFailedException(String s) {
            super(s);
        }
    }


}
