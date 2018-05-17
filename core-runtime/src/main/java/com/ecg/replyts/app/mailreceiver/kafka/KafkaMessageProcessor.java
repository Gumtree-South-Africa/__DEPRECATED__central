package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.replyts.app.mailreceiver.MessageProcessor;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


import static com.ecg.replyts.core.runtime.prometheus.MessageProcessingMetrics.incMsgAbandonedCounter;
import static com.ecg.replyts.core.runtime.prometheus.MessageProcessingMetrics.incMsgRetriedCounter;
import static com.ecg.replyts.core.runtime.prometheus.MessageProcessingMetrics.incMsgUnparseableCounter;

@NotThreadSafe
abstract class KafkaMessageProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageProcessor.class);

    private static final long KAFKA_POLL_TIMEOUT_MS = 1000;
    private final int retryOnFailedMessagePeriodMinutes;

    private final QueueService queueService;

    private final KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    final String shortTenant;

    protected Consumer<String, byte[]> consumer;

    KafkaMessageProcessor(QueueService queueService, KafkaMessageConsumerFactory kafkaMessageConsumerFactory,
                          int retryOnFailedMessagePeriodMinutes, String shortTenant) {
        this.queueService = queueService;
        this.kafkaMessageConsumerFactory = kafkaMessageConsumerFactory;
        this.retryOnFailedMessagePeriodMinutes = retryOnFailedMessagePeriodMinutes;
        this.shortTenant = shortTenant;
    }

    @Override
    public void destroy() {
        consumer.wakeup();
    }

    protected void setTaskFields() {
        MDCConstants.setTaskFields("ReplyTS-worker-thread");
    }

    protected abstract String getTopicName();

    @Override
    public void processNext() {
        try {
            if (consumer == null) {
                consumer = kafkaMessageConsumerFactory.createConsumer(this.getTopicName());
            }

            consumer.poll(KAFKA_POLL_TIMEOUT_MS).forEach(messageRecord -> {
                try {
                    processMessage(messageRecord);
                } finally {
                    consumer.commitSync();
                }
            });
        } catch (WakeupException ignored) {
            // This exception is raised when the consumer is in its poll() loop, waking it up. We can ignore it here,
            // because this very likely means that we are shutting down Comaas.
        } catch (Exception e) {
            try {
                if (consumer != null) {
                    consumer.close();
                }
            } finally {
                consumer = null;
            }
        }
    }

    Message decodeMessage(ConsumerRecord<String, byte[]> messageRecord) throws IOException {
        final byte[] data = messageRecord.value();
        final Message retryableMessage;
        try {
            retryableMessage = queueService.deserialize(data);
        } catch (IOException e) {
            failMessage(data, e);
            throw e;
        }
        return retryableMessage;
    }

    protected abstract void processMessage(ConsumerRecord<String, byte[]> messageRecord);

    private void publishToTopic(final String topic, final Message retryableMessage) throws JsonProcessingException {
        queueService.publish(topic, retryableMessage);
    }

    // Some messages are unparseable, so we don't even retry, instead they go on the unparseable topic

    void unparseableMessage(final Message retryableMessage) {
        incMsgUnparseableCounter();
        try {
            publishToTopic(KafkaTopicService.getTopicUnparseable(shortTenant), retryableMessage);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize message before putting it in the unparseable queue, correlationId: {}", retryableMessage.getCorrelationId(), e);
        }
    }

    // Put the message in the retry topic with a specific delay
    void delayRetryMessage(final Message retryableMessage) {
        incMsgRetriedCounter();
        try {
            Instant currentConsumptionTime = Instant.ofEpochSecond(
                    retryableMessage.getNextConsumptionTime().getSeconds(),
                    retryableMessage.getNextConsumptionTime().getNanos())
                    .plus(retryOnFailedMessagePeriodMinutes, ChronoUnit.MINUTES);

            Timestamp nextConsumptionTime = Timestamp
                    .newBuilder()
                    .setSeconds(currentConsumptionTime.getEpochSecond())
                    .setNanos(currentConsumptionTime.getNano())
                    .build();

            int retryCount = retryableMessage.getRetryCount() + 1;

            Message retriedMessage = Message
                    .newBuilder(retryableMessage)
                    .setRetryCount(retryCount)
                    .setNextConsumptionTime(nextConsumptionTime)
                    .build();

            publishToTopic(KafkaTopicService.getTopicRetry(shortTenant), retriedMessage);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize message before putting it in the retry queue, correlationId: {}", retryableMessage.getCorrelationId(), e);
        }
    }

    // Put the message back in the incoming topic
    void retryMessage(final Message retryableMessage) {
        queueService.publish(KafkaTopicService.getTopicIncoming(shortTenant), retryableMessage);
    }

    // After n retries, we abandon the message by putting it in the abandoned topic
    void abandonMessage(final Message retryableMessage, final Exception e) {
        incMsgAbandonedCounter();
        LOG.error("Mail processing abandoned for message with correlationId {}", retryableMessage.getCorrelationId(), e);
        try {
            publishToTopic(KafkaTopicService.getTopicAbandoned(shortTenant), retryableMessage);
        } catch (JsonProcessingException e1) {
            LOG.warn("Could not serialize message before putting it in the abandon queue, correlationId: {}", retryableMessage.getCorrelationId(), e1);
        }
    }

    // Don't know what to do... Put it in the failed queue! Most likely unparseable json
    private void failMessage(final byte[] payload, final Exception e) {
        LOG.error("Could not handle message, writing raw value to failed topic", e);
        queueService.publish(KafkaTopicService.getTopicFailed(shortTenant), payload);
    }
}
