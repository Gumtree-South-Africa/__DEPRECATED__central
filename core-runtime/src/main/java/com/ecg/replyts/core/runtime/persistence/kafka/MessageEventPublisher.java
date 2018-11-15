package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.replyts.app.ContentOverridingPostProcessorService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.util.ConversationEventConverter;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.ecg.replyts.core.api.model.conversation.MessageState.IGNORED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static java.util.Arrays.asList;

@Component
public class MessageEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventPublisher.class);

    private static final String CUST_HEADER_BUYER_NAME = "buyer-name";
    private static final String CUST_HEADER_SELLER_NAME = "seller-name";

    private final BlockUserRepository blockUserRepository;
    private final ConversationEventService conversationEventService;
    private final UserIdentifierService userIdentifierService;
    private final ContentOverridingPostProcessorService contentOverridingPostProcessorService;
    private final String shortTenant;

    @Autowired
    public MessageEventPublisher(
            BlockUserRepository blockUserRepository,
            ConversationEventService conversationEventService,
            UserIdentifierService userIdentifierService,
            ContentOverridingPostProcessorService contentService,
            @Value("${replyts.tenant.short}") String shortTenant) {

        this.blockUserRepository = blockUserRepository;
        this.conversationEventService = conversationEventService;
        this.userIdentifierService = userIdentifierService;
        this.contentOverridingPostProcessorService = contentService;
        this.shortTenant = shortTenant;
    }

    public void publish(MessageProcessingContext context, Conversation conversation, Message message) {
        if (Objects.isNull(context) || Objects.isNull(conversation)
                || Objects.isNull(conversation.getMessages()) || Objects.isNull(message)) {

            LOG.warn("Cannot send a conversation event, one of the mandatory fields is null: Context {}, Conversation {}, Messages {}, Message {} ",
                    Objects.isNull(context), Objects.isNull(conversation), Objects.isNull(conversation.getMessages()), Objects.isNull(message));
            return;
        }

        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            LOG.debug("Conversation is DEAD_ON_ARRIVAL and publishing events is ignored: MessageId {}, ConversationId {}",
                    message.getId(), conversation.getId());
            return;
        }

        if (message.getState() == IGNORED) {
            LOG.debug("Message is IGNORED and publishing events is ignored: MessageId {}, ConversationId {}",
                    message.getId(), conversation.getId());
            return;
        }

        Optional<String> senderIdOpt = getSenderUserId(conversation, message);
        if (!senderIdOpt.isPresent()) {
            LOG.warn("Sender ID is null: MessageId {}, ConversationId {}", message.getId(), conversation.getId());
            return;
        }

        try {
            internalPublish(conversation, message, context.getTransport(), context.getOriginTenant(), senderIdOpt.get());
        } catch (InterruptedException e) {
            LOG.warn("Aborting mail processing flow because thread is interrupted.");
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            LOG.error("failed to submit the conversation into the messaging events queue", e);
            throw e;
        }
    }

    private void internalPublish(Conversation conversation, Message message, MessageTransport transport, String originTenant, String senderId)
            throws InterruptedException {

        if (conversation.getMessages().size() == 1) {
            conversationEventService.sendConversationCreatedEvent(shortTenant, conversation.getAdId(),
                    conversation.getId(), conversation.getCustomValues(), getParticipants(conversation), conversation.getCreatedAt());
        }

        String cleanedMessage = contentOverridingPostProcessorService.getCleanedMessage(conversation, message);

        List<String> receivingUsers = getReceivingUsers(conversation, message, senderId);

        conversationEventService.sendMessageAddedEvent(shortTenant, conversation.getId(), senderId, message.getId(),
                cleanedMessage, message.getHeaders(), transport, originTenant, message.getReceivedAt(), receivingUsers);
    }

    private List<String> getReceivingUsers(Conversation conversation, Message message, String senderId) {
        List<String> receivers = new ArrayList<>();
        receivers.add(senderId);

        String buyerUserId = getBuyerUserId(conversation.getCustomValues(), conversation.getBuyerId());
        String sellerUserId = getSellerUserId(conversation.getCustomValues(), conversation.getSellerId());

        String receiverId = buyerUserId.equals(senderId)
                ? sellerUserId
                : buyerUserId;

        if (messageShouldBeVisibleToReceiver(conversation, message, senderId, receiverId)) {
            receivers.add(receiverId);
        }

        return receivers;
    }

    public boolean messageShouldBeVisibleToReceiver(Conversation conv, Message msg, String senderId, String receiverId) {
        return isConversationActive(conv, msg) &&
                !blockUserRepository.hasBlocked(receiverId, senderId);
    }

    // note: it is arguably unintuitive that the classification if this message (SENT) determines the 'activeness' of conversation, but note that
    // this is terminology that's apparently used more broadly in our domain.
    private boolean isConversationActive(Conversation conv, Message msg) {
        return msg.getState() == SENT && conv.getState() != ConversationState.CLOSED;
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
