package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.mailreceiver.MessageProcessingPoolManager;
import com.ecg.replyts.app.mailreceiver.MessageProcessor;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;


@Component
@ConditionalOnProperty(name = "mail.provider.strategy", havingValue = "kafka")
public class KafkaMessageProcessingPoolManager extends MessageProcessingPoolManager {
    private final int newMailProcessingThreads;
    private final int retryProcessingThreads;

    @Value("${mailreceiver.retries:5}")
    private int maxRetries;

    @Value("${mailreceiver.retrydelay.minutes:5}")
    private int retryOnFailedMessagePeriodMinutes;

    @Value("${replyts.tenant}")
    private String tenantLongName;

    @Value("${kafka.message.processing.enabled:false}")
    private boolean messageProcessingEnabled;

    @Autowired
    private MessageProcessingCoordinator messageProcessingCoordinator;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    @Autowired
    public KafkaMessageProcessingPoolManager(@Value("${replyts.threadpool.kafka.new.mail.size:2}") int newMailProcessingThreads,
                                             @Value("${replyts.threadpool.kafka.retry.mail.size:2}") int retryProcessingThreads,
                                             @Value("${replyts.threadpool.shutdown.await.ms:10000}") long gracefulShutdownTimeoutMs) {
        super(newMailProcessingThreads + retryProcessingThreads, gracefulShutdownTimeoutMs);
        this.newMailProcessingThreads = newMailProcessingThreads;
        this.retryProcessingThreads = retryProcessingThreads;
    }

    @Override
    protected Stream<MessageProcessor> createProcessorStream() {
        return Stream.concat(
                Stream.generate(this::createNewMessageConsumer).limit(newMailProcessingThreads),
                Stream.generate(this::createRetryMessageConsumer).limit(retryProcessingThreads)
        );
    }

    private KafkaNewMessageProcessor createNewMessageConsumer() {
        return new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService, kafkaMessageConsumerFactory,
                retryOnFailedMessagePeriodMinutes, maxRetries, tenantLongName, messageProcessingEnabled);
    }

    private KafkaRetryMessageProcessor createRetryMessageConsumer() {
        return new KafkaRetryMessageProcessor(queueService, kafkaMessageConsumerFactory, retryOnFailedMessagePeriodMinutes, tenantLongName);
    }
}