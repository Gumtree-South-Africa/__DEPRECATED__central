package com.ecg.replyts.app.mailreceiver;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;

public interface MessageProcessor {
    Counter UNPARSEABLE_COUNTER = TimingReports.newCounter("processing-unparseable-counter");
    // Note the misnomer, this is actually not yet a failed state
    Counter RETRIED_MESSAGE_COUNTER = TimingReports.newCounter("processing_failed");
    Counter ABANDONED_RETRY_COUNTER = TimingReports.newCounter("processing_failed_abandoned");

    /**
     * Processes one message. Should adequately react on external interruption by
     * either rethrowing the {@see InterruptedException} or by restoring the interruption flag and terminating
     * gracefully.
     */
    void processNext() throws InterruptedException;

    default void destroy(){
    }
}
