package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.comaas.events.Conversation.ConversationCreated;
import com.ecg.comaas.events.Conversation.ConversationDeleted;
import com.ecg.comaas.events.Conversation.Envelope;
import com.ecg.comaas.events.Conversation.MessageAdded;
import com.ecg.comaas.events.Conversation.Participant;
import com.ecg.comaas.events.Conversation.UUID;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "conversation.events.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConversationEventService implements ConversationEventService {

    private final QueueService queueService;

    @Autowired
    public KafkaConversationEventService(QueueService queueService) {
        this.queueService = queueService;
    }

    public void sendConversationCreatedEvent(String tenant, String adId, String conversationId, Map<String, String> metadata, Set<Participant> participants, DateTime createAt) {
        Instant time = Instant.ofEpochMilli(createAt.getMillis());
        ConversationCreated conversationCreated = ConversationCreated.newBuilder()
                .setAdId(adId)
                .setCreationDate(Timestamp.newBuilder()
                        .setSeconds(time.getEpochSecond())
                        .setNanos(time.getNano())
                        .build())
                .addAllParticipants(participants)
                .putAllMetadata(metadata)
                .build();

        Envelope envelope = Envelope.newBuilder()
                .setTenant(tenant)
                .setConversationId(conversationId)
                .setConversationCreated(conversationCreated)
                .build();

        queueService.publishSynchronously(KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, conversationId, envelope);
    }

    public void sendMessageAddedEvent(String tenant, String conversationId, String senderUserId, String messageId, String message, Map<String, String> metadata) {
        MessageAdded messageAdded = MessageAdded.newBuilder()
                .setId(UUID.newBuilder()
                        .setUuid(messageId)
                        .build())
                .setSenderUserId(senderUserId)
                .setText(ByteString.copyFrom(message, Charsets.UTF_8))
                .putAllMetadata(metadata)
                .build();

        Envelope envelope = Envelope.newBuilder()
                .setTenant(tenant)
                .setConversationId(conversationId)
                .setMessageAdded(messageAdded)
                .build();

        queueService.publishSynchronously(KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, conversationId, envelope);
    }

    @Override
    public void sendConversationDeletedEvent(String tenant, String conversationId, Participant participant) {
        ConversationDeleted conversationDeleted = ConversationDeleted.newBuilder()
                .addParticipants(participant)
                .build();

        Envelope envelope = Envelope.newBuilder()
                .setTenant(tenant)
                .setConversationId(conversationId)
                .setConversationDeleted(conversationDeleted)
                .build();

        queueService.publishSynchronously(KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, conversationId, envelope);
    }
}
