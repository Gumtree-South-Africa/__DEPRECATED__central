package com.ecg.replyts.core.runtime.prometheus;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;

public final class MessageProcessingMetrics {

    private MessageProcessingMetrics() {
    }

    private static final Counter UNPARSEABLE_MESSAGE_COUNTER = TimingReports.newCounter("processing-unparseable-counter");
    // Note the misnomer, this is actually not yet a failed state
    private static final Counter RETRIED_MESSAGE_COUNTER = TimingReports.newCounter("processing_failed");
    private static final Counter ABANDONED_MESSAGE_RETRY_COUNTER = TimingReports.newCounter("processing_failed_abandoned");

    private static final io.prometheus.client.Counter MESSAGE_COUNTER = io.prometheus.client.Counter.build(
            "ingestion_message_processing",
            "Number of messages in different processing stages")
            .labelNames("status")
            .register();

    public static void incMsgRetriedCounter() {
        MESSAGE_COUNTER.labels("failed").inc();
        RETRIED_MESSAGE_COUNTER.inc();
    }

    public static void incMsgAbandonedCounter() {
        MESSAGE_COUNTER.labels("abandoned").inc();
        ABANDONED_MESSAGE_RETRY_COUNTER.inc();
    }

    public static void incMsgUnparseableCounter() {
        MESSAGE_COUNTER.labels("unparseable").inc();
        UNPARSEABLE_MESSAGE_COUNTER.inc();
    }

}
