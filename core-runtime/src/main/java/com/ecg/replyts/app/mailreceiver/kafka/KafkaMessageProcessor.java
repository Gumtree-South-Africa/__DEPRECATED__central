package com.ecg.replyts.app.mailreceiver.kafka;

import com.codahale.metrics.Counter;
import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.replyts.app.mailreceiver.MessageProcessor;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.google.protobuf.Timestamp;
import kafka.common.ThreadShutdownException;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.ecg.replyts.core.runtime.prometheus.MessageProcessingMetrics.*;

@NotThreadSafe
abstract class KafkaMessageProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageProcessor.class);

    private static final Counter MAIL_PROCESSING_TIMEDOUT_COUNTER = TimingReports.newCounter("mail-processing-timedout");

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

    protected Message deserialize(final byte[] data) throws IOException {
        return Message.parseFrom(data);
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
                setTaskFields();
                decodeMessage(messageRecord).ifPresent(message -> {
                    try {
                        processMessage(message);
                        doCommitSync();
                    } catch (HangingThreadException e) {
                        MAIL_PROCESSING_TIMEDOUT_COUNTER.inc();
                        doCommitSync();
                        shutdown();
                    }
                });
            });
        } catch (WakeupException e) {
            // This exception is raised when the consumer is in its poll() loop, waking it up. We can ignore it here,
            // because this very likely means that we are shutting down Comaas.
            LOG.debug("WakeupException process next", e);
        } catch (Exception e) {
            // In cause we failed to write to retry queue we are likely to fail to make progress.
            // Considering this is fine.  Failing to write to abandoned queue would not stop the processing.
            LOG.error("Process next failed", e);
        }
    }

    private void shutdown() {
        try {
            closeConsumer();
        } finally {
            stopApplication();
        }
    }

    private void closeConsumer() {
        try {
            if (consumer != null) {
                consumer.close();
            }
        } finally {
            consumer = null;
        }
    }

    void stopApplication() {
        System.exit(1);
    }


    private Optional<Message> decodeMessage(ConsumerRecord<String, byte[]> messageRecord) {
        final byte[] data = messageRecord.value();
        final Message retryableMessage;
        try {
            retryableMessage = deserialize(data);
        } catch (IOException e) {
            failMessage(data, e);
            return Optional.empty();
        }
        return Optional.of(retryableMessage);
    }


    protected abstract void processMessage(Message message);

    private void publishToTopic(final String topic, final Message retryableMessage) {
        try {
            queueService.publishSynchronously(topic, retryableMessage);
        } catch (Exception ex) {
            LOG.error("Failed to publish message to retry topic!", ex);
        }
    }

    // Some messages are unparseable, so we don't even retry, instead they go on the unparseable topic
    void unparseableMessage(final Message retryableMessage) {
        incMsgUnparseableCounter();
        publishToTopic(KafkaTopicService.getTopicUnparseable(shortTenant), retryableMessage);
    }

    // Put the message in the retry topic with a specific delay
    void delayRetryMessage(final Message retryableMessage) {
        incMsgRetriedCounter();
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
    }

    // Put the message back in the incoming topic
    void retryMessage(final Message retryableMessage) {
        queueService.publishSynchronously(KafkaTopicService.getTopicIncoming(shortTenant), retryableMessage);
    }

    // After n retries, we abandon the message by putting it in the abandoned topic
    void abandonMessage(final Message retryableMessage, final Exception e) {
        try {
            incMsgAbandonedCounter();
            LOG.warn("Mail processing abandoned for message with correlationId {}", retryableMessage.getCorrelationId(), e);
            publishToTopic(KafkaTopicService.getTopicAbandoned(shortTenant), retryableMessage);
        } catch (Exception failedEx) {
            LOG.error("Failed to write message to abandon queue", failedEx);
        }
    }

    // Don't know what to do... Put it in the failed queue! Most likely unparseable json
    private void failMessage(final byte[] payload, final Exception e) {
        LOG.warn("Could not handle message, writing raw value to failed topic", e);
        queueService.publishSynchronously(KafkaTopicService.getTopicFailed(shortTenant), payload);
    }

    private void doCommitSync() {
        try {
            consumer.commitSync();
        } catch (WakeupException e) {
            // we're shutting down, but finish the commit first and then
            // rethrow the exception so that the main loop can exit
            LOG.debug("WakeupException in commit sync", e);
            doCommitSync();
            throw e;
        } catch (CommitFailedException e) {
            // the commit failed with an unrecoverable error. if there is any
            // internal state which depended on the commit, you can clean it
            // up here. otherwise it's reasonable to ignore the error and go on
            LOG.debug("Commit sync failed", e);
        } catch (Exception e) {
            LOG.warn("Commit sync failed", e);
        }
    }
}
