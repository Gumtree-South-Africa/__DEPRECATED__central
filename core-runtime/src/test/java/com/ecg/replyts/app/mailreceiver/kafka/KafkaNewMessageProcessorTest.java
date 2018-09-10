package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.comaas.protobuf.MessageOuterClass.Payload;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.ProcessingContextFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
    private static final byte[] PAYLOAD = Message.newBuilder().build().toByteArray();
    private static final int RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES = 5;
    private static final int MAX_RETRIES = 5;

    public MockConsumer<String, byte[]> consumer;

    @Mock
    private MessageProcessingCoordinator messageProcessingCoordinator;

    @Mock
    private QueueService queueService;

    @Mock
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    @Mock
    private ProcessingContextFactory processingContextFactory;

    @Mock
    private UserIdentifierService userIdentifierService;

    @Mock
    private MutableConversationRepository mutableConversationRepository;

    @Mock
    private MutableConversation mutableConversation;

    @Mock
    private MessageProcessingContext messageProcessingContext;

    @Captor
    private ArgumentCaptor<String> topicNameCaptor;

    @Captor
    private ArgumentCaptor<Message> retryableMessageCaptor;

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
        consumer = Mockito.spy(consumer);
        when(kafkaMessageConsumerFactory.createConsumer(any())).thenReturn(consumer);
        when(kafkaMessageConsumerFactory.getMaxPollIntervalMs()).thenReturn(300000);

        when(mutableConversationRepository.getById(any())).thenReturn(mutableConversation);
        when(mutableConversation.getId()).thenReturn("conversationId");
        when(mutableConversation.getBuyerId()).thenReturn("userId");

        when(processingContextFactory.newContext(anyString(), anyCollection())).thenReturn(messageProcessingContext);
        when(messageProcessingContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(userIdentifierService.getBuyerUserId(any(Conversation.class))).thenReturn(Optional.of("userId"));
        when(userIdentifierService.getSellerUserId(any(Conversation.class))).thenReturn(Optional.of("userId"));
    }

    private void sendIncomingMessage(final byte[] rawMessage) {
        consumer.assign(Collections.singletonList(new TopicPartition(TOPIC_INCOMING, 0)));
        consumer.addRecord(new ConsumerRecord<>(TOPIC_INCOMING, 0, incomingOffset++, "someKey", rawMessage));
    }

    private Message setUpTest(final int triedCount) throws IOException {
        Payload payload = Payload.newBuilder().setMessage("message").setUserId("userId").build();
        Message wanted = Message.newBuilder().setPayload(payload).setRetryCount(triedCount).build();
        sendIncomingMessage(wanted.toByteArray());
        return wanted;
    }

    private String longRunningMessageProcessor(boolean canTerminate) {
        while (true) {
            if (canTerminate && Thread.interrupted()) {
                return "";
            }
        }
    }

    @Test
    public void longRunningTerminableMessage() throws Exception {
        when(messageProcessingCoordinator.handleContext(any(), any()))
                .thenAnswer((InvocationOnMock invocation) -> longRunningMessageProcessor(true));
        Message wanted = setUpTest(0);
        KafkaNewMessageProcessor kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService,
                kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT,
                mutableConversationRepository, processingContextFactory, userIdentifierService, 100L, 100L) {
            @Override
            protected Message deserialize(byte[] data) throws IOException {
                return wanted;
            }

            @Override
            void stopApplication() {

            }
        };

        kafkaNewMessageProcessor.processNext();

        //Kafka consumer checks
        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(consumer).commitSync();
        verify(consumer, never()).close();

        //kafka topic checks
        verify(queueService).publishSynchronously(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_RETRY);

        checkRetryMessage(wanted);
    }

    @Test
    public void longRunningNonTerminableMessage() throws Exception {
        when(messageProcessingCoordinator.handleContext(any(), any()))
                .thenAnswer((InvocationOnMock invocation) -> longRunningMessageProcessor(false));
        Message wanted = setUpTest(0);
        KafkaNewMessageProcessor kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService,
                kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT,
                mutableConversationRepository, processingContextFactory, userIdentifierService, 100L, 100L) {
            @Override
            protected Message deserialize(byte[] data) throws IOException {
                return wanted;
            }

            @Override
            void stopApplication() {

            }
        };

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));

        verify(queueService).publishSynchronously(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_ABANDONED);
        assertThat(retryableMessageCaptor.getValue()).isEqualToComparingFieldByField(wanted);
        verify(consumer).commitSync();
        assertThat(consumer.closed()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void unparseableMessageWritesToUnparseableTopic() throws Exception {
        when(messageProcessingCoordinator.handleContext(any(), any())).thenThrow(ParsingException.class);
        Message message = setUpTest(0);
        KafkaNewMessageProcessor kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService,
                kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT,
                mutableConversationRepository, processingContextFactory, userIdentifierService, 100L, 100L) {
            @Override
            protected Message deserialize(byte[] data) throws IOException {
                return message;
            }

            @Override
            void stopApplication() {

            }
        };

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publishSynchronously(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_UNPARSEABLE);

    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedMessageWritesToAbandonedTopicIfTooManyRetries() throws Exception {
        when(messageProcessingCoordinator.handleContext(any(), any())).thenThrow(IOException.class);
        final Message wanted = setUpTest(MAX_RETRIES);
        KafkaNewMessageProcessor kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService,
                kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT,
                mutableConversationRepository, processingContextFactory, userIdentifierService, 100L, 100L) {
            @Override
            protected Message deserialize(byte[] data) throws IOException {
                return wanted;
            }

            @Override
            void stopApplication() {

            }
        };

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publishSynchronously(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_ABANDONED);

        assertThat(retryableMessageCaptor.getValue()).isEqualToComparingFieldByField(wanted);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedMessageWritesToRetryTopicIfRetryCountAllows() throws Exception {
        when(messageProcessingCoordinator.handleContext(any(), any())).thenThrow(IOException.class);
        when(mutableConversationRepository.getById(any())).thenReturn(mutableConversation);

        final Message wanted = setUpTest(2);
        KafkaNewMessageProcessor kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService,
                kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT,
                mutableConversationRepository, processingContextFactory, userIdentifierService, 100L, 100L) {
            @Override
            protected Message deserialize(byte[] data) throws IOException {
                return wanted;
            }

            @Override
            void stopApplication() {

            }
        };

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publishSynchronously(topicNameCaptor.capture(), retryableMessageCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_RETRY);

        checkRetryMessage(wanted);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void undeserializableMessageWritesToFailedTopic() throws Exception {
        KafkaNewMessageProcessor kafkaNewMessageProcessor = new KafkaNewMessageProcessor(messageProcessingCoordinator, queueService,
                kafkaMessageConsumerFactory, RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES, MAX_RETRIES, SHORT_TENANT,
                mutableConversationRepository, processingContextFactory, userIdentifierService, 100L, 100L) {
            @Override
            protected Message deserialize(byte[] data) throws IOException {
                throw new IOException();
            }

            @Override
            void stopApplication() {

            }
        };
        sendIncomingMessage(PAYLOAD);

        kafkaNewMessageProcessor.processNext();

        verify(kafkaMessageConsumerFactory).createConsumer(eq(TOPIC_INCOMING));
        verify(queueService).publishSynchronously(topicNameCaptor.capture(), payloadCaptor.capture());
        assertThat(topicNameCaptor.getValue()).isEqualTo(TOPIC_FAILED);
        assertThat(payloadCaptor.getValue()).isEqualTo(PAYLOAD);
    }

    private void checkRetryMessage(Message wanted) {
        Message actualMessage = retryableMessageCaptor.getValue();
        assertThat(actualMessage.getRetryCount()).isEqualTo(wanted.getRetryCount() + 1);
        assertThat(actualMessage.getNextConsumptionTime().getSeconds())
                .isEqualTo(wanted.getNextConsumptionTime().getSeconds() + TimeUnit.MINUTES.toSeconds(RETRY_ON_FAILED_MESSAGE_PERIOD_MINUTES));
        assertThat(actualMessage.getCorrelationId()).isEqualTo(wanted.getCorrelationId());
        assertThat(actualMessage.getPayload()).isEqualTo(wanted.getPayload());
        assertThat(actualMessage.getReceivedTime()).isEqualTo(wanted.getReceivedTime());
    }

}