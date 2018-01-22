package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.ecg.replyts.core.runtime.persistence.kafka.RetryableMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * docker-compose --project-name comaasdocker exec kafka bash
 * /opt/kafka/bin/kafka-topics.sh --list --zookeeper zookeeper:2181
 * /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic mp_messages_retry --from-beginning
 * /opt/kafka/bin/kafka-topics.sh --alter --zookeeper zookeeper:2181 --topic mp_messages --partitions 10
 *
 * https://github.corp.ebay.com/ecg-comaas/kafka-message-producer
 * docker run --rm --net=host docker-registry.ecg.so/comaas/kafka-message-producer:0.0.1 --tenant mp --delay 100 10
 */
public class KafkaNewMessageProcessor extends KafkaMessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaNewMessageProcessor.class);

    private final int maxRetries;
    private final boolean messageProcessingEnabled;
    private final MessageProcessingCoordinator messageProcessingCoordinator;

    KafkaNewMessageProcessor(MessageProcessingCoordinator messageProcessingCoordinator, QueueService queueService,
                             KafkaMessageConsumerFactory kafkaMessageConsumerFactory, int retryOnFailedMessagePeriodMinutes,
                             int maxRetries, String tenant, boolean messageProcessingEnabled) {

        super(queueService, kafkaMessageConsumerFactory, retryOnFailedMessagePeriodMinutes, tenant);
        this.maxRetries = maxRetries;
        this.messageProcessingCoordinator = messageProcessingCoordinator;
        this.messageProcessingEnabled = messageProcessingEnabled;
    }

    protected void processMessage(ConsumerRecord<String, byte[]> messageRecord) {
        setTaskFields();

        RetryableMessage retryableMessage;
        try {
            retryableMessage = decodeMessage(messageRecord);
        } catch (IOException e) {
            return;
        }

        LOG.debug("Found a message in the incoming topic {}, tried so far: {} times", retryableMessage.getCorrelationId(), retryableMessage.getTriedCount());

        try {
            if (messageProcessingEnabled) {
                messageProcessingCoordinator.accept(new ByteArrayInputStream(retryableMessage.getPayload()));
            } else {
                // Remove when we are done testing
                ShadowTestingFramework9000.maybeDoAThing();
            }
        } catch (ParsingException e) {
            unparseableMessage(retryableMessage);
        } catch (Exception e) {
            if (retryableMessage.getTriedCount() >= maxRetries) {
                abandonMessage(retryableMessage, e);
            } else {
                delayRetryMessage(retryableMessage);
            }
        }
    }

    @Override
    protected String getTopicName() {
        return KafkaTopicService.getTopicIncoming(tenant);
    }
}
