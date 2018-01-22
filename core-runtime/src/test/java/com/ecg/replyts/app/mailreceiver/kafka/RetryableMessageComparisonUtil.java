package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.core.runtime.persistence.kafka.RetryableMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryableMessageComparisonUtil {
    static void compareMessages(final RetryableMessage wanted, final RetryableMessage actualRetryableMessage) {
        assertThat(actualRetryableMessage.getTriedCount()).isEqualTo(wanted.getTriedCount());
        compareInstantsWithoutMillis(wanted.getNextConsumptionTime(), actualRetryableMessage.getNextConsumptionTime());
        assertThat(actualRetryableMessage.getCorrelationId()).isEqualTo(wanted.getCorrelationId());
        assertThat(actualRetryableMessage.getPayload()).isEqualTo(wanted.getPayload());
        assertThat(actualRetryableMessage.getMessageReceivedTime()).isEqualTo(wanted.getMessageReceivedTime());
    }

    static void compareInstantsWithoutMillis(final Instant wanted, final Instant actual) {
        assertThat(actual.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(wanted.truncatedTo(ChronoUnit.SECONDS));
    }
}
