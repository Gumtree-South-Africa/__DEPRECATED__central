package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.replyts.app.ContentOverridingPostProcessorService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.util.ConversationEventConverter;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.Arrays.asList;

@Component
public class MessageEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventPublisher.class);

    private static final String CUST_HEADER_BUYER_NAME = "buyer-name";
    private static final String CUST_HEADER_SELLER_NAME = "seller-name";

    private final ConversationEventService conversationEventService;
    private final UserIdentifierService userIdentifierService;
    private final ContentOverridingPostProcessorService contentOverridingPostProcessorService;
    private final String shortTenant;

    @Autowired
    public MessageEventPublisher(
            ConversationEventService conversationEventService,
            UserIdentifierService userIdentifierService,
            ContentOverridingPostProcessorService contentService,
            @Value("${replyts.tenant.short}") String shortTenant) {

        this.conversationEventService = conversationEventService;
        this.userIdentifierService = userIdentifierService;
        this.contentOverridingPostProcessorService = contentService;
        this.shortTenant = shortTenant;
    }

    public void publish(MessageProcessingContext context, Conversation conversation, Message message) {
        if (Objects.isNull(context) || Objects.isNull(conversation) || Objects.isNull(message)) {
            return;
        }

        if (context.isTerminated()) {
            return;
        }

        try {
            internalPublish(conversation, message, context.getTransport(), context.getOriginTenant());
        } catch (InterruptedException e) {
            LOG.warn("Aborting mail processing flow because thread is interrupted.");
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            LOG.error("failed to submit the conversation into the messaging events queue", e);
            throw e;
        }
    }

    private void internalPublish(Conversation conversation, Message message, MessageTransport transport, String originTenant)
            throws InterruptedException {

        if (conversation == null || conversation.getMessages() == null) {
            LOG.warn("conversation.getMessages() == null");
            return;
        }

        if (conversation.getMessages().size() == 1) {
            conversationEventService.sendConversationCreatedEvent(shortTenant, conversation.getAdId(),
                    conversation.getId(), conversation.getCustomValues(), getParticipants(conversation), conversation.getCreatedAt());
        }

        String cleanedMessage = contentOverridingPostProcessorService.getCleanedMessage(conversation, message);
        Optional<String> senderIdOpt = getSenderUserId(conversation, message);

        conversationEventService.sendMessageAddedEvent(shortTenant, conversation.getId(), senderIdOpt, message.getId(),
                cleanedMessage, message.getHeaders(), transport, originTenant, message.getReceivedAt());
    }

    private Optional<String> getSenderUserId(Conversation conversation, Message message) {
        return message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER
                ? userIdentifierService.getBuyerUserId(conversation.getCustomValues())
                : userIdentifierService.getSellerUserId(conversation.getCustomValues());
    }

    private Set<com.ecg.comaas.events.Conversation.Participant> getParticipants(Conversation conversation) {
        com.ecg.comaas.events.Conversation.Participant buyer = ConversationEventConverter.createParticipant(
                getBuyerUserId(conversation.getCustomValues(), conversation.getBuyerId()),
                conversation.getCustomValues().get(CUST_HEADER_BUYER_NAME),
                conversation.getBuyerId(),
                com.ecg.comaas.events.Conversation.Participant.Role.BUYER,
                conversation.getBuyerSecret());
        com.ecg.comaas.events.Conversation.Participant seller = ConversationEventConverter.createParticipant(
                getSellerUserId(conversation.getCustomValues(), conversation.getSellerId()),
                conversation.getCustomValues().get(CUST_HEADER_SELLER_NAME),
                conversation.getSellerId(),
                com.ecg.comaas.events.Conversation.Participant.Role.SELLER,
                conversation.getSellerSecret());
        return new HashSet<>(asList(buyer, seller));
    }

    private String getBuyerUserId(Map<String, String> customValues, String buyerId) {
        return userIdentifierService.getBuyerUserId(customValues).orElse(buyerId);
    }

    private String getSellerUserId(Map<String, String> customValues, String sellerId) {
        return userIdentifierService.getSellerUserId(customValues).orElse(sellerId);
    }
}
