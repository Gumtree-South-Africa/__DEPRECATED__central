package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

public class FilterWithShadowComparison implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(FilterWithShadowComparison.class);

    private final Filter authority;
    private final InterruptibleFilter underTest;
    private final DifferenceReporter differenceReporter;
    private final Duration secondFilterTimeout;

    private final ExecutorService remoteFilterExecutorService = Executors.newFixedThreadPool(
            RemoteFilter.NR_OF_COMAAS_THREADS,
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("remoteFilterProxy-%s").build()
    );

    private FilterWithShadowComparison(Filter authority, InterruptibleFilter underTest, DifferenceReporter differenceReporter, Duration secondFilterTimeout) {
        this.authority = authority;
        this.underTest = underTest;
        this.differenceReporter = differenceReporter;
        this.secondFilterTimeout = secondFilterTimeout;
    }

    public static FilterWithShadowComparison create(Filter authority, InterruptibleFilter underTest) {
        return new FilterWithShadowComparison(
                authority,
                underTest,
                (main, test) -> LOG.warn("Different filter results for class={}; \nref={}, test={}", authority.getClass(), main, test),
                Duration.ofSeconds(10)
        );
    }

    static FilterWithShadowComparison createForTesting(Filter authority, InterruptibleFilter underTest, DifferenceReporter differenceReporter, Duration secondFilterTimeout) {
        return new FilterWithShadowComparison(
                authority,
                underTest,
                differenceReporter,
                secondFilterTimeout
        );
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        Future<List<FilterFeedback>> testFuture = this.remoteFilterExecutorService.submit(
                () -> this.underTest.filter(context)
        );

        /* We block indefinitely, even if the authoritative filter blocks forever (and we know it might).
         * This is because a .get(timeout) would result in a leak: we know they don't respect interrupts, so we cannot kill them anyway.
         *
         * For this reason, KafkaNewMessageProcessor#processMessage does convoluted stuff to detect this and throw HangingThreadException to
         * restart the application. And we want to give this mechanism the change to do its work. That means that as a proxy, we must also hang.
         * The caller should/will fix this by detecting this and restarting the JVM.
         */
        List<FilterFeedback> mainResult = this.authority.filter(context);

        Try.of(
                () -> {
                    try {
                        // we can time out on the filter unterTest, as it's interruptible.
                        return testFuture.get(secondFilterTimeout.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw e.getCause(); // propagate filter exception as if there never was a Future.
                    } catch (TimeoutException e) {
                        testFuture.cancel(true);
                        throw new RuntimeException("Execution of remote filter timed out", e);
                    }
                })
                .onFailure(e -> LOG.warn("Remote filter executing of testFilter {} failed (ignoring it)", this.underTest, e))
                .onSuccess(testResult -> logDifferences(mainResult, testResult));

        // FIXME: add metrics

        return mainResult;
    }

    public void logDifferences(List<FilterFeedback> ref, List<FilterFeedback> test) {
        if (!ref.equals(test)) {
            differenceReporter.reportDetectedDifference(ref, test);
        }
    }

    /**
     * Do some side effect to report difference
     */
    public interface DifferenceReporter {
        void reportDetectedDifference(List<FilterFeedback> ref, List<FilterFeedback> test);
    }
}
