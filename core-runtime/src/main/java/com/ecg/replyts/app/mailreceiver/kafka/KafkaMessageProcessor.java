package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.app.mailreceiver.MessageProcessor;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.ecg.replyts.core.runtime.persistence.kafka.RetryableMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@NotThreadSafe
abstract class KafkaMessageProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageProcessor.class);

    private final static long KAFKA_POLL_TIMEOUT_MS = 1000;
    private final int retryOnFailedMessagePeriodMinutes;

    private final QueueService queueService;

    final private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    final String tenant;

    protected Consumer<String, byte[]> consumer;

    KafkaMessageProcessor(QueueService queueService, KafkaMessageConsumerFactory kafkaMessageConsumerFactory,
                          int retryOnFailedMessagePeriodMinutes, String tenant) {
        this.queueService = queueService;
        this.kafkaMessageConsumerFactory = kafkaMessageConsumerFactory;
        this.retryOnFailedMessagePeriodMinutes = retryOnFailedMessagePeriodMinutes;
        this.tenant = tenant;
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

    RetryableMessage decodeMessage(ConsumerRecord<String, byte[]> messageRecord) throws IOException {
        final byte[] messageJson = messageRecord.value();
        final RetryableMessage retryableMessage;
        try {
            retryableMessage = queueService.deserialize(messageJson);
        } catch (IOException e) {
            failMessage(messageJson, e);
            throw e;
        }
        return retryableMessage;
    }

    protected abstract void processMessage(ConsumerRecord<String, byte[]> messageRecord);

    private void publishToTopic(final String topic, final RetryableMessage retryableMessage) throws JsonProcessingException {
        queueService.publish(topic, retryableMessage);
    }

    // Some messages are unparseable, so we don't even retry, instead they go on the unparseable topic
    void unparseableMessage(final RetryableMessage retryableMessage) {
        UNPARSEABLE_COUNTER.inc();
        try {
            publishToTopic(KafkaTopicService.getTopicUnparseable(tenant), retryableMessage);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize message before putting it in the unparseable queue, correlationId: {}", retryableMessage.getCorrelationId(), e);
        }
    }

    // Put the message in the retry topic with a specific delay
    void delayRetryMessage(final RetryableMessage retryableMessage) {
        RETRIED_MESSAGE_COUNTER.inc();
        try {
            RetryableMessage retriedMessage = new RetryableMessage(
                    retryableMessage.getMessageReceivedTime(),
                    Instant.now().plus(retryOnFailedMessagePeriodMinutes, ChronoUnit.MINUTES),
                    retryableMessage.getPayload(),
                    retryableMessage.getTriedCount() + 1,
                    retryableMessage.getCorrelationId());
            publishToTopic(KafkaTopicService.getTopicRetry(tenant), retriedMessage);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize message before putting it in the retry queue, correlationId: {}", retryableMessage.getCorrelationId(), e);
        }
    }

    // Put the message back in the incoming topic
    void retryMessage(final RetryableMessage retryableMessage) {
        try {
            queueService.publish(KafkaTopicService.getTopicIncoming(tenant), retryableMessage);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize message before putting it back in the incoming queue after retry, correlationId: {}", retryableMessage.getCorrelationId(), e);
        }
    }

    // After n retries, we abandon the message by putting it in the abandoned topic
    void abandonMessage(final RetryableMessage retryableMessage, final Exception e) {
        ABANDONED_RETRY_COUNTER.inc();
        LOG.error("Mail processing abandoned for message with correlationId {}", retryableMessage.getCorrelationId(), e);
        try {
            publishToTopic(KafkaTopicService.getTopicAbandoned(tenant), retryableMessage);
        } catch (JsonProcessingException e1) {
            LOG.warn("Could not serialize message before putting it in the abandon queue, correlationId: {}", retryableMessage.getCorrelationId(), e1);
        }
    }

    // Don't know what to do... Put it in the failed queue! Most likely unparseable json
    void failMessage(final byte[] payload, final Exception e) {
        LOG.error("Could not handle message, writing raw value to failed topic", e);
        queueService.publish(KafkaTopicService.getTopicFailed(tenant), payload);
    }
}