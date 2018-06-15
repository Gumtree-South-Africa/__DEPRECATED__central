package com.ecg.replyts.app.mailreceiver.kafka;

import com.codahale.metrics.Histogram;
import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Read from the <tenant>_messages_retry topic, wait until the next consumption time, and put the message back into the <tenant>_messages topic
 */
public class KafkaRetryMessageProcessor extends KafkaMessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaRetryMessageProcessor.class);

    // Report the time between the message's next consumption time and the time it was actually processed
    private static final Histogram RETRY_LAG = TimingReports.newHistogram("kafka.retry.lag");

    KafkaRetryMessageProcessor(QueueService queueService, KafkaMessageConsumerFactory kafkaMessageConsumerFactory,
                               int retryOnFailedMessagePeriodMinutes, String shortTenant) {
        super(queueService, kafkaMessageConsumerFactory, retryOnFailedMessagePeriodMinutes, shortTenant);
    }

    @Override
    protected void processMessage(Message message) {
        LOG.debug("Found a message {} in the retry topic with next consumption time {}", message.getCorrelationId(), message.getNextConsumptionTime());

        Instant nextConsumptionTime = Instant.ofEpochSecond(
                message.getNextConsumptionTime().getSeconds(),
                message.getNextConsumptionTime().getNanos());
        if (!sleepUntilInstant(nextConsumptionTime)) {
            LOG.warn("The thread has been interrupted while sleeping before making an attempt to retry message with correlationId: {}", message.getCorrelationId());
            return;
        }

        RETRY_LAG.update(ChronoUnit.MILLIS.between(nextConsumptionTime, Instant.now()));

        LOG.debug("Putting message back to incoming topic {}", message.getCorrelationId());
        retryMessage(message);
    }

    private boolean sleepUntilInstant(Instant until) {
        long sleepTimeMs = ChronoUnit.MILLIS.between(Instant.now(), until);
        if (sleepTimeMs <= 0) {
            return true;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(sleepTimeMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    protected String getTopicName() {
        return KafkaTopicService.getTopicRetry(shortTenant);
    }
}
