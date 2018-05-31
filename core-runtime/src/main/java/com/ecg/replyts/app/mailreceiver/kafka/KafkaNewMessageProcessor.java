package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.ProcessingContextFactory;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.google.protobuf.ByteString;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;

/**
 * docker-compose --project-name comaasdocker exec kafka bash
 * /opt/kafka/bin/kafka-topics.sh --list --zookeeper zookeeper:2181
 * /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic mp_messages_retry --from-beginning
 * /opt/kafka/bin/kafka-topics.sh --alter --zookeeper zookeeper:2181 --topic mp_messages --partitions 10
 * <p>
 * https://github.corp.ebay.com/ecg-comaas/kafka-message-producer
 * docker run --rm --net=host dock.es.ecg.tools/comaas/kafka-message-producer:0.0.1 --tenant mp --delay 100 10
 */
public class KafkaNewMessageProcessor extends KafkaMessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaNewMessageProcessor.class);

    private final int maxRetries;
    private final boolean messageProcessingEnabled;
    private final MessageProcessingCoordinator messageProcessingCoordinator;
    private final MutableConversationRepository conversationRepository;
    private final ProcessingContextFactory processingContextFactory;
    private final UserIdentifierService userIdentifierService;

    KafkaNewMessageProcessor(MessageProcessingCoordinator messageProcessingCoordinator, QueueService queueService,
                             KafkaMessageConsumerFactory kafkaMessageConsumerFactory, int retryOnFailedMessagePeriodMinutes,
                             int maxRetries, String shortTenant, boolean messageProcessingEnabled,
                             MutableConversationRepository conversationRepository,
                             ProcessingContextFactory processingContextFactory, UserIdentifierService userIdentifierService) {
        super(queueService, kafkaMessageConsumerFactory, retryOnFailedMessagePeriodMinutes, shortTenant);
        this.maxRetries = maxRetries;
        this.messageProcessingCoordinator = messageProcessingCoordinator;
        this.messageProcessingEnabled = messageProcessingEnabled;
        this.conversationRepository = conversationRepository;
        this.processingContextFactory = processingContextFactory;
        this.userIdentifierService = userIdentifierService;
    }

    @Override
    protected void processMessage(ConsumerRecord<String, byte[]> messageRecord) {
        setTaskFields();

        Message retryableMessage;
        try {
            retryableMessage = decodeMessage(messageRecord);
        } catch (IOException e) {
            return;
        }

        LOG.debug("Found a message in the incoming topic {}, tried so far: {} times", retryableMessage.getCorrelationId(), retryableMessage.getRetryCount());

        try {
            if (messageProcessingEnabled) {
                processMessage(retryableMessage);
            } else {
                // Remove when we are done testing
                ShadowTestingFramework9000.maybeDoAThing();
            }
        } catch (ParsingException e) {
            unparseableMessage(retryableMessage);
        } catch (Exception e) {
            if (retryableMessage.getRetryCount() >= maxRetries) {
                abandonMessage(retryableMessage, e);
            } else {
                delayRetryMessage(retryableMessage);
            }
        }
    }

    private void processMessage(Message kafkaMessage) throws IOException, ParsingException {
        ByteString rawEmail = kafkaMessage.getRawEmail();
        if (rawEmail != null && !rawEmail.isEmpty()) {
            // A presence of a raw email represents the deprecated way of ingesting emails
            // See https://github.corp.ebay.com/ecg-comaas/comaas-adr/blob/master/adr-004-kmail-transition-period.md
            messageProcessingCoordinator.accept(new ByteArrayInputStream(rawEmail.toByteArray()));
        } else {
            MutableConversation conversation = getConversation(kafkaMessage.getPayload().getConversationId());
            MessageProcessingContext context = createContext(kafkaMessage.getPayload().getUserId(), conversation);
            context.addCommand(
                    createAddMessageCommand(kafkaMessage.getPayload().getMessage(), conversation.getId(), context,
                            kafkaMessage.getMetadataMap()));
            messageProcessingCoordinator.handleContext(Optional.empty(), context);
        }
    }

    private MutableConversation getConversation(String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            throw new IllegalStateException(String.format("Conversation not found for ID: %s", conversationId));
        }
        return conversation;
    }

    private MessageProcessingContext createContext(String userId, MutableConversation conversation) {
        MessageProcessingContext context = processingContextFactory.newContext(null, Guids.next());
        context.setConversation(conversation);
        String sellerId = userIdentifierService.getSellerUserId(conversation)
                .orElseThrow(() -> new IllegalArgumentException("Failed to infer a seller id"));
        String buyerId = userIdentifierService.getBuyerUserId(conversation)
                .orElseThrow(() -> new IllegalArgumentException("Failed to infer a buyer id"));
        if (userId.equals(sellerId)) {
            context.setMessageDirection(MessageDirection.SELLER_TO_BUYER);
        } else if (userId.equals(buyerId)) {
            context.setMessageDirection(MessageDirection.BUYER_TO_SELLER);
        } else {
            throw new IllegalArgumentException("User is not a participant");
        }
        return context;
    }

    private AddMessageCommand createAddMessageCommand(String message, String conversationId,
                                                      MessageProcessingContext context, Map<String, String> metadata) {
        return anAddMessageCommand(conversationId, context.getMessageId())
                .withMessageDirection(context.getMessageDirection())
                .withSenderMessageIdHeader(context.getMessageId())
                //.withInResponseToMessageId() TODO akobiakov: find out what this is used for
                .withHeaders(Collections.emptyMap())
                .withTextParts(Collections.singletonList(message))
                .withAttachmentFilenames(Collections.emptyList())
                .withHeaders(metadata)
                .build();
    }

    @Override
    protected String getTopicName() {
        return KafkaTopicService.getTopicIncoming(shortTenant);
    }
}
