package com.ecg.replyts.app.mailreceiver;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;

public interface MessageProcessor {

    /**
     * Processes one message. Should adequately react on external interruption by
     * either rethrowing the {@see InterruptedException} or by restoring the interruption flag and terminating
     * gracefully.
     */
    void processNext() throws InterruptedException;

    default void destroy(){
    }
}
