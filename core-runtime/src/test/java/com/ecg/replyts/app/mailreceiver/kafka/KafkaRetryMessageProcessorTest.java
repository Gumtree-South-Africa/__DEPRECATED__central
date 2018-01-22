package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.ecg.replyts.core.runtime.persistence.kafka.RetryableMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Collections;
import java.util.HashMap;

import static com.ecg.replyts.app.mailreceiver.kafka.RetryableMessageComparisonUtil.compareMessages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KafkaRetryMessageProcessorTest {
    private static final String TENANT = "mp";
    private static final String TOPIC_UNPARSEABLE = KafkaTopicService.getTopicUnparseable(TENANT);
    private static final String TOPIC_RETRY = KafkaTopicService.getTopicRetry(TENANT);
    private static final String TOPIC_ABANDONED = KafkaTopicService.getTopicAbandoned(TENANT);
    private static final String TOPIC_FAILED = KafkaTopicService.getTopicFailed(TENANT);
    private static final String TOPIC_INCOMING = KafkaTopicService.getTopicIncoming(TENANT);
    private static final String CORRELATION_ID = "corr";
    private static final byte[] PAYLOAD = "some payload".getBytes();
    public static final int RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES = 5;

    private KafkaRetryMessageProcessor kafkaRetryMessageProcessor;

    @Mock
    private MessageProcessingCoordinator messageProcessingCoordinator;

    @Mock
    private QueueService queueService;

    @Mock
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    public MockConsumer<String, byte[]> consumer;

    @Captor
    private ArgumentCaptor<String> topicNameCaptor;

    @Captor
    private ArgumentCaptor<RetryableMessage> retryableMessageCaptor;

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

    private static long retryOffset = 0;

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Before
    public void setUp() throws Exception {
        HashMap<TopicPartition, Long> beginningOffsets = new HashMap<>();
        beginningOffsets.put(new TopicPartition(TOPIC_RETRY, 0), 0L);
//        beginningOffsets.put(new TopicPartition(topicService.getTopicRetry(), 0), 0L);

        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        consumer.updateBeginningOffsets(beginningOffsets);
        when(kafkaMessageConsumerFactory.createConsumer(any())).thenReturn(consumer);

        kafkaRetryMessageProcessor = new KafkaRetryMessageProcessor(queueService, kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, TENANT);
    }

    private void sendRetryMessage(final RetryableMessage retryableMessage) throws JsonProcessingException {
        consumer.assign(Collections.singletonList(new TopicPartition(TOPIC_RETRY, 0)));
        consumer.addRecord(new ConsumerRecord<>(TOPIC_RETRY, 0, retryOffset++, "someKey", mapper.writeValueAsBytes(retryableMessage)));
    }

    private RetryableMessage setUpTest(final int triedCount) throws Exception {
        RetryableMessage wanted = new RetryableMessage(Instant.now(), Instant.now(), PAYLOAD, triedCount, CORRELATION_ID);
        when(queueService.deserialize(any())).thenReturn(wanted);
        sendRetryMessage(wanted);
        return wanted;
    }

    @Test
    public void undeserializableMessageWritesToFailedTopic() throws Exception {
        setUpTest(99);
        when(queueService.deserialize(any())).thenThrow(IOException.class);

        kafkaRetryMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_RETRY));
        verify(queueService).publish(topicNameCaptor.capture(), payloadCaptor.capture());
        verify(queueService).deserialize(any());
        verifyNoMoreInteractions(queueService);
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_FAILED);
    }

    @Test
    public void retriedMessageWritesToIncomingTopic() throws Exception {
        RetryableMessage wanted = setUpTest(4);

        kafkaRetryMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_RETRY));
        verify(queueService).publish(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        verify(queueService).deserialize(any());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_INCOMING);

        compareMessages(wanted, retryableMessageCaptor.getValue());
    }
}