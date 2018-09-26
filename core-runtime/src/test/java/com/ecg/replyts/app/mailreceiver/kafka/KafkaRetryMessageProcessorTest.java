package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.comaas.protobuf.MessageOuterClass.Payload;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KafkaRetryMessageProcessorTest {

    private static final String SHORT_TENANT = "mp";
    private static final String TOPIC_RETRY = KafkaTopicService.getTopicRetry(SHORT_TENANT);
    private static final String TOPIC_FAILED = KafkaTopicService.getTopicFailed(SHORT_TENANT);
    private static final String TOPIC_INCOMING = KafkaTopicService.getTopicIncoming(SHORT_TENANT);
    private static final String CORRELATION_ID = "corr";
    private static final byte[] PAYLOAD = "some payload".getBytes();
    private static final int RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES = 5;

    @Mock
    private QueueService queueService;

    @Mock
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    public MockConsumer<String, byte[]> consumer;

    @Captor
    private ArgumentCaptor<String> topicNameCaptor;

    @Captor
    private ArgumentCaptor<Message> retryableMessageCaptor;

    private static long retryOffset = 0;

    @Before
    public void setUp() throws Exception {
        HashMap<TopicPartition, Long> beginningOffsets = new HashMap<>();
        beginningOffsets.put(new TopicPartition(TOPIC_RETRY, 0), 0L);

        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        consumer.updateBeginningOffsets(beginningOffsets);
        when(kafkaMessageConsumerFactory.createConsumer(any())).thenReturn(consumer);
    }

    private void sendRetryMessage(final Message retryableMessage) throws JsonProcessingException {
        consumer.assign(Collections.singletonList(new TopicPartition(TOPIC_RETRY, 0)));
        consumer.addRecord(new ConsumerRecord<>(TOPIC_RETRY, 0, retryOffset++, "someKey", retryableMessage.toByteArray()));
    }

    private Message message(final int triedCount) throws IOException {
        Payload payload = Payload.newBuilder().build();
        Message wanted = Message.newBuilder().setPayload(payload).setRetryCount(triedCount).setCorrelationId(CORRELATION_ID).build();
        sendRetryMessage(wanted);
        return wanted;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void undeserializableMessageWritesToFailedTopic() throws Exception {
        message(99);
        KafkaRetryMessageProcessor kafkaRetryMessageProcessor = new KafkaRetryMessageProcessor(queueService, kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, SHORT_TENANT) {
            @Override
            protected Message deserialize(byte[] data) throws InvalidProtocolBufferException {
                throw new InvalidProtocolBufferException("mocking a failure");
            }
        };

        kafkaRetryMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_RETRY));
        verify(queueService).publishSynchronously(eq(TOPIC_FAILED), any(byte[].class));
    }

    @Test
    public void retriedMessageWritesToIncomingTopic() throws Exception {
        Message wanted = message(4);
        KafkaRetryMessageProcessor kafkaRetryMessageProcessor = new KafkaRetryMessageProcessor(queueService, kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, SHORT_TENANT) {
            @Override
            protected Message deserialize(byte[] data) {
                return wanted;
            }
        };

        kafkaRetryMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_RETRY));
        verify(queueService).publishSynchronously(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_INCOMING);

        assertThat(retryableMessageCaptor.getValue()).isEqualToComparingFieldByField(wanted);
    }
}