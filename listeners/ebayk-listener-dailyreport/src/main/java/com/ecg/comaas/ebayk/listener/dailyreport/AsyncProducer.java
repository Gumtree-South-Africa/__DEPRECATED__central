package com.ecg.comaas.ebayk.listener.dailyreport;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;

/**
 * Send records to Kafka async in fire-forget style.
 * <p>
 * Prevent issues when Kafka isn't available. In this case the producer hangs to wait for fetching meta data.
 *
 * @param <K> Type of key
 * @param <V> Type of value
 */
public class AsyncProducer<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncProducer.class);

    // The max number of records to buffer
    private static final int MAX_BUFFERED_RECORDS = 5_000;

    private final Producer<K, V> producer;

    private final ExecutorService executor;

    private final ExceptionCollector exceptionCollector = new ExceptionCollector();

    public AsyncProducer(Producer<K, V> producer) {

        Objects.requireNonNull(producer);

        this.producer = producer;
        this.executor = new ThreadPoolExecutor(5, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(MAX_BUFFERED_RECORDS),
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("kafka-async-producer-%d").build(),
                new LoggingRejectedExecutionHandler());
    }


    public void send(ProducerRecord<K, V> record) {

        executor.submit(setTaskFields(() -> safeSend(record), "AsyncProducer-send"));
    }

    private void safeSend(ProducerRecord<K, V> record) {

        try {
            producer.send(record);
        } catch (RuntimeException e) {
            exceptionCollector.recogniseException(e);
        }
    }

    public void close() {

        executor.shutdown();
        try {
            boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!terminated) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for the pool to be shut down");
            Thread.currentThread().interrupt();
        }
        producer.close();
    }

    private static class LoggingRejectedExecutionHandler implements RejectedExecutionHandler {

        // Used only to track the failure in the exception collector.
        private static final Exception EXCEPTION_TO_REPORT = new Exception("Discard event!");

        private final ExceptionCollector exceptionCollector = new ExceptionCollector();

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            exceptionCollector.recogniseException(EXCEPTION_TO_REPORT);
        }
    }

}
