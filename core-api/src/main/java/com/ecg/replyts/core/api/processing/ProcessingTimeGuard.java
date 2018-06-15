package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.api.util.CurrentClock;

import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Guard the total processing time of an email.
 * <p>
 * The guard should enable RTS and the filters to protected against long running email processing. We saw mail processing
 * of over a hour due to tons of complex RegEx filter processed on spam mails with very long text lines.
 */
public class ProcessingTimeGuard {

    // Marker for infinite
    private static final long INFINITE = 0L;

    private final Date processingStart;
    private final long maxMessageProcessingTimeMs;
    private final Clock clock;

    /**
     * @param maxMessageProcessingTimeSeconds The max allowed processing time. '0' means infinite.
     */
    public ProcessingTimeGuard(long maxMessageProcessingTimeSeconds) {
        this(new Date(), maxMessageProcessingTimeSeconds, new CurrentClock());
    }

    ProcessingTimeGuard(Date processingStart, long maxTotalProcessingDurationSeconds, Clock clock) {

        checkNotNull(processingStart);
        checkNotNull(clock);
        checkArgument(maxTotalProcessingDurationSeconds >= 0L);

        this.processingStart = new Date(processingStart.getTime());
        this.maxMessageProcessingTimeMs = SECONDS.toMillis(maxTotalProcessingDurationSeconds);
        this.clock = clock;
    }

    /**
     * Check, if further processing allowed.
     *
     * @throws ProcessingTimeExceededException If processing time exceeded.
     */
    public void check() throws ProcessingTimeExceededException, ProcessingInterruptedException {
        if (Thread.interrupted()) {
            throw new ProcessingInterruptedException();
        }
        if (infinite()) {
            return;
        }
        long durationMs = clock.now().getTime() - processingStart.getTime();
        if (durationMs >= maxMessageProcessingTimeMs) {
            throw new ProcessingTimeExceededException(maxMessageProcessingTimeMs, durationMs);
        }
    }

    private boolean infinite() {
        return maxMessageProcessingTimeMs == INFINITE;
    }
}
