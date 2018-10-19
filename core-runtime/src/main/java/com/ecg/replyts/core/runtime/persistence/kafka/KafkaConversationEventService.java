package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.comaas.events.Conversation.*;
import com.ecg.replyts.core.api.model.conversation.ProtoMapper;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "conversation.events.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConversationEventService implements ConversationEventService {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConversationEventService.class);

    private final QueueService queueService;

    @Autowired
    public KafkaConversationEventService(QueueService queueService) {
        this.queueService = queueService;
    }

    public void sendConversationCreatedEvent(String tenant, String adId, String conversationId, Map<String, String> metadata, Set<Participant> participants, DateTime createAt) throws InterruptedException {
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

        LOG.trace("ConversationCreatedEvent: \n" + envelope);
        queueService.publishSynchronously(KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, conversationId, envelope);
    }

    public void sendMessageAddedEvent(String tenant,
                                      String conversationId,
                                      Optional<String> senderUserId,
                                      String messageId,
                                      String message,
                                      Map<String, String> metadata,
                                      MessageProcessingContext context) throws InterruptedException {

        MessageAdded.Builder builder = MessageAdded.newBuilder()
                .setId(UUID.newBuilder()
                        .setUuid(messageId)
                        .build())
                .setText(ByteString.copyFrom(message, Charsets.UTF_8))
                .setTransport(ProtoMapper.messageTransportToProto(context.getTransport()))
                .putAllMetadata(metadata);

        senderUserId.ifPresent(builder::setSenderUserId);

        Envelope envelope = Envelope.newBuilder()
                .setTenant(tenant)
                .setOwner(context.getOwner() == null ? tenant : context.getOwner())
                .setConversationId(conversationId)
                .setMessageAdded(builder.build())
                .build();

        LOG.trace("MessageAddedEvent: \n" + envelope);
        queueService.publishSynchronously(KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, conversationId, envelope);
    }

    @Override
    public void sendConversationDeletedEvent(String tenant, String conversationId, Participant participant) throws InterruptedException {
        ConversationDeleted conversationDeleted = ConversationDeleted.newBuilder()
                .addParticipants(participant)
                .build();

        Envelope envelope = Envelope.newBuilder()
                .setTenant(tenant)
                .setConversationId(conversationId)
                .setConversationDeleted(conversationDeleted)
                .build();

        LOG.trace("ConversationDeleteEvent: \n" + envelope);
        queueService.publishSynchronously(KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, conversationId, envelope);
    }
}
