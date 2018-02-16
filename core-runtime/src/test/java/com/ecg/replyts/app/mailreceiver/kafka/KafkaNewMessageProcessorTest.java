package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.ecg.replyts.core.runtime.persistence.kafka.RetryableMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KafkaNewMessageProcessorTest {
    private static final String SHORT_TENANT = "ek";
    private static final String TOPIC_UNPARSEABLE = KafkaTopicService.getTopicUnparseable(SHORT_TENANT);
    private static final String TOPIC_RETRY = KafkaTopicService.getTopicRetry(SHORT_TENANT);
    private static final String TOPIC_INCOMING = KafkaTopicService.getTopicIncoming(SHORT_TENANT);
    private static final String TOPIC_FAILED = KafkaTopicService.getTopicFailed(SHORT_TENANT);
    private static final String TOPIC_ABANDONED = KafkaTopicService.getTopicAbandoned(SHORT_TENANT);
    private static final String CORRELATION_ID = "corr";
    private static final byte[] PAYLOAD = "some payload".getBytes();
    private static final int RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES = 5;
    private static final int MAX_RETRIES = 5;

    private KafkaNewMessageProcessor kafkaNewMessageProcessor;

    public MockConsumer<String, byte[]> consumer;

    @Mock
    private MessageProcessingCoordinator messageProcessingCoordinator;

    @Mock
    private QueueService queueService;

    @Mock
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    @Captor
    private ArgumentCaptor<String> topicNameCaptor;

    @Captor
    private ArgumentCaptor<RetryableMessage> retryableMessageCaptor;

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

    private static long incomingOffset = 0;


    @Before
    public void setUp() throws Exception {
        HashMap<TopicPartition, Long> beginningOffsets = new HashMap<>();
        beginningOffsets.put(new TopicPartition(TOPIC_INCOMING, 0), 0L);
        beginningOffsets.put(new TopicPartition(TOPIC_RETRY, 0), 0L);

        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        consumer.updateBeginningOffsets(beginningOffsets);
        when(kafkaMessageConsumerFactory.createConsumer(any())).thenReturn(consumer);

        kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService, kafkaMessageConsumerFactory,
                RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT, true);
    }

    private void sendIncomingMessage(final byte[] rawMessage) {
        consumer.assign(Collections.singletonList(new TopicPartition(TOPIC_INCOMING, 0)));
        consumer.addRecord(new ConsumerRecord<>(TOPIC_INCOMING, 0, incomingOffset++, "someKey", rawMessage));
    }

    private RetryableMessage setUpTest(final int triedCount) throws IOException {
        Instant now = Instant.now();
        RetryableMessage wanted = new RetryableMessage(now, now, PAYLOAD, triedCount, CORRELATION_ID);
        when(queueService.deserialize(any())).thenReturn(wanted);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sendIncomingMessage(mapper.writeValueAsBytes(wanted));
        return wanted;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void unparseableMessageWritesToUnparseableTopic() throws Exception {
        when(messageProcessingCoordinator.accept(any())).thenThrow(ParsingException.class);
        final RetryableMessage wanted = setUpTest(0);

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publish(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        verify(queueService).deserialize(any());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_UNPARSEABLE);

        assertThat(retryableMessageCaptor.getValue()).isEqualToComparingFieldByField(wanted);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedMessageWritesToAbandonedTopicIfTooManyRetries() throws Exception {
        when(messageProcessingCoordinator.accept(any())).thenThrow(IOException.class);
        final RetryableMessage wanted = setUpTest(MAX_RETRIES);

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publish(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        verify(queueService).deserialize(any());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_ABANDONED);

        assertThat(retryableMessageCaptor.getValue()).isEqualToComparingFieldByField(wanted);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedMessageWritesToRetryTopicIfRetryCountAllows() throws Exception {
        when(messageProcessingCoordinator.accept(any())).thenThrow(IOException.class);
        final RetryableMessage wanted = setUpTest(2);

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publish(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        verify(queueService).deserialize(any());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_RETRY);

        RetryableMessage actualRetryableMessage = retryableMessageCaptor.getValue();
        assertThat(actualRetryableMessage.getTriedCount()).isEqualTo(wanted.getTriedCount() + 1);
        assertThat(actualRetryableMessage.getNextConsumptionTime())
                .isEqualTo(wanted.getNextConsumptionTime().plus(RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, ChronoUnit.MINUTES));
        assertThat(actualRetryableMessage.getCorrelationId()).isEqualTo(wanted.getCorrelationId());
        assertThat(actualRetryableMessage.getPayload()).isEqualTo(wanted.getPayload());
        assertThat(actualRetryableMessage.getMessageReceivedTime()).isEqualTo(wanted.getMessageReceivedTime());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void undeserializableMessageWritesToFailedTopic() throws Exception {
        when(queueService.deserialize(any())).thenThrow(IOException.class);
        sendIncomingMessage(PAYLOAD);

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publish(topicNameCaptor.capture(), payloadCaptor.capture());
        verify(queueService).deserialize(any());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_FAILED);
        assertThat(payloadCaptor.getValue()).isEqualTo(PAYLOAD);
    }
}