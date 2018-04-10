package com.ecg.comaas.ebayk.listener.dailyreport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Count exceptions of same type and only log them periodically to prevent spamming the logs.
 * <p>
 * Knowing issue: A single sporadic exception won't be shown up until happens once again because logging will only
 * applied on {@link ExceptionCollector#recogniseException(Exception)}.
 * <p>
 * Implementation note: Currently, it seems that Kafka use the deserializer single threaded. But to not relay on
 * future implementation changes we implement is thread save.
 */
class ExceptionCollector {

    // Define the time how often to log exceptions
    private static final long EXPIRE_TIME_MS = 10_000L;

    private final Logger logger;

    private final Supplier<Long> clockSupplier;

    // The max allowed size of the collector.
    // Should't happen under normal conditions
    private static final int MAX_SIZE = 100;

    private final Map<Integer, Collector> collectors = new ConcurrentHashMap<>();

    // For testing
    private ExceptionCollector(Logger logger, Supplier<Long> clockSupplier) {
        this.logger = logger;
        this.clockSupplier = clockSupplier;
    }

    ExceptionCollector() {
        this(LoggerFactory.getLogger(ExceptionCollector.class), System::currentTimeMillis);
    }

    /**
     * Count exceptions of same type. Log them only periodically.
     *
     * @param exception The exception to recognize.
     */
    void recogniseException(Exception exception) {

        int hash = calculateHash(exception);
        Collector collector = collectors.computeIfAbsent(hash, key -> new Collector(exception, clockSupplier));
        collector.count.incrementAndGet();

        if (collector.isExpired()) {
            logger.error("Since {} ms collect {} exception(s) of type:", EXPIRE_TIME_MS, collector.count, collector.exception);
            collectors.remove(hash);
        }

        if (collectors.size() > MAX_SIZE) {
            // For the case exceptions goes crazy, remove the complete map to prevent memory leak
            collectors.clear();
        }
    }

    private int calculateHash(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        // we live with the case that different exceptions with empty
        // stacktrace elements produce the same hash
        return Arrays.hashCode(stackTrace);
    }

    private static class Collector {

        private final AtomicLong count = new AtomicLong();
        private final long start;
        private final Exception exception;
        private final Supplier<Long> clockSupplier;

        private Collector(Exception exception, Supplier<Long> clockSupplier) {
            this.start = clockSupplier.get();
            this.exception = exception;
            this.clockSupplier = clockSupplier;
        }

        private boolean isExpired() {
            return clockSupplier.get() - start > EXPIRE_TIME_MS;
        }
    }


}
