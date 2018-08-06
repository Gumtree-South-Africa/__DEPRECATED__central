package com.ecg.comaas.gtau.listener.messageevent;

import com.ebay.ecg.australia.events.entity.Entities;
import com.ebay.ecg.australia.events.event.message.MessageEvents;
import com.ebay.ecg.australia.events.origin.OriginDefinition;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler.reportExternalServiceFailure;

@Component
public class RTSRMQEventCreator {

    private static final Logger LOG = LoggerFactory.getLogger(RTSRMQEventCreator.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String REPLY_CHANNEL_HEADER = "X-Reply-Channel";
    private static final String USER_AGENT = "User-Agent";
    private static final String CATEGORY_ID = "categoryid";
    private static final String IP = "ip";

    private final RabbitMQEventHandlerClient eventHandlerClient;

    @Autowired
    public RTSRMQEventCreator(RabbitMQEventHandlerClient eventHandlerClient) {
        this.eventHandlerClient = eventHandlerClient;
    }

    public void messageEventEntry(final Conversation conversation, final Message message) {
        final MessageEvents.MessageCreatedEvent messageRequestCreatedEvent = createMessageRequestCreatedEvent(conversation, message);

        try {
            // publishSynchronously to RabbitMQ
            eventHandlerClient.fire(messageRequestCreatedEvent);
        } catch (Exception e) {
            reportExternalServiceFailure("rabbitmq_publish_message_created_event");
            LOG.error("Failed to publishSynchronously event to RabbitMQ for message {} in conversation {}", message.getId(), conversation.getId(), e);
        }

        if (LOG.isTraceEnabled()) {
            Entities.ConversationInfo conversationInfo = messageRequestCreatedEvent.getConversationInfo();
            LOG.trace("Sent MessageCreatedEvent conversationId: {}, messageId: {}, adId: {}, sellerMail: {}, buyerMail: {}",
                    conversationInfo.getMessageConversationInfo().getConversationId(),
                    conversationInfo.getMessageConversationInfo().getMessageId(),
                    conversationInfo.getAdId(),
                    conversationInfo.getSellerMail(),
                    conversationInfo.getBuyerMail());
        }
    }

    /**
     * Create the MessageCreatedEvent which will be sent to RabbitMQ.
     *
     * @param conversation the Conversation
     * @param message      the Message
     * @return MessageCreatedEvent the event to send to RabbitMQ
     */
    private static MessageEvents.MessageCreatedEvent createMessageRequestCreatedEvent(final Conversation conversation, final Message message) {
        final Entities.MessageConversationInfo messageConversationInfo = messageConversationInfo(conversation, message);
        final Entities.ConversationInfo conversationInfo = conversationInfo(conversation, messageConversationInfo);

        if (LOG.isTraceEnabled()) {
            String mciToString = MoreObjects.toStringHelper("MessageConversationInfo")
                    .add("conversationId", conversation.getId())
                    .add("messageDirection", messageConversationInfo.getMessageDirection())
                    .add("messageId", messageConversationInfo.getMessageId())
                    .add("messageState", messageConversationInfo.getMessageState())
                    .add("messageReceivedAt", messageConversationInfo.getMessageReceivedAt())
                    .add("replyChannel", messageConversationInfo.getReplyChannel())
                    .toString();
            LOG.trace(mciToString);

            String ciToString = MoreObjects.toStringHelper("ConversationInfo")
                    .add("conversationState", conversationInfo.getConversationState())
                    .add("adId", conversation.getAdId())
                    .add("sellerId", conversation.getSellerId())
                    .add("buyerId", conversation.getBuyerId())
                    .add("messageSize", conversation.getMessages().size())
                    .add("conversationCreatedAt", conversationInfo.getConversationCreatedAt())
                    .add("conversationLastModifiedAt", conversationInfo.getConversationLastModifiedDate())
                    .add("categoryId", conversationInfo.getCategoryId())
                    .add("IP", conversationInfo.getIp())
                    .add("userAgent", conversationInfo.getUserAgent())
                    .toString();
            LOG.trace(ciToString);
        }

        final OriginDefinition.Origin origin = OriginDefinition.Origin.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .build();

        return MessageEvents.MessageCreatedEvent.newBuilder()
                .setOrigin(origin)
                .setConversationInfo(conversationInfo)
                .build();
    }

    private static Entities.ConversationInfo conversationInfo(Conversation conversation, Entities.MessageConversationInfo messageConversationInfo) {
        Entities.ConversationInfo.Builder conversationInfoBuilder = Entities.ConversationInfo.newBuilder();

        if (conversation.getAdId() != null) {
            conversationInfoBuilder.setAdId(conversation.getAdId());
        }
        if (conversation.getSellerId() != null) {
            conversationInfoBuilder.setSellerMail(conversation.getSellerId());
        }
        if (conversation.getBuyerId() != null) {
            conversationInfoBuilder.setBuyerMail(conversation.getBuyerId());
        }
        if (conversation.getState() != null) {
            conversationInfoBuilder.setConversationState(conversation.getState().name());
        }
        if (conversation.getCreatedAt() != null) {
            conversationInfoBuilder.setConversationCreatedAt(conversation.getCreatedAt().toString(DATE_FORMAT));
        }
        if (conversation.getLastModifiedAt() != null) {
            conversationInfoBuilder.setConversationLastModifiedDate(conversation.getLastModifiedAt().toString(DATE_FORMAT));
        }
        if (conversation.getMessages() != null) {
            conversationInfoBuilder.setNumOfMessageInConversation(String.valueOf(conversation.getMessages().size()));
        }

        if (conversation.getCustomValues() != null) {
            String categoryId = conversation.getCustomValues().get(CATEGORY_ID);
            if (categoryId != null) {
                conversationInfoBuilder.setCategoryId(categoryId);
            }
            String ip = conversation.getCustomValues().get(IP);
            if (ip != null) {
                conversationInfoBuilder.setIp(ip);
            }
            String userAgent = conversation.getCustomValues().get(USER_AGENT);
            if (userAgent != null) {
                conversationInfoBuilder.setUserAgent(userAgent);
            }
        }

        conversationInfoBuilder.setMessageConversationInfo(messageConversationInfo);
        return conversationInfoBuilder.build();
    }

    private static Entities.MessageConversationInfo messageConversationInfo(Conversation conversation, Message message) {
        Entities.MessageConversationInfo.Builder messageConvInfoBuilder = Entities.MessageConversationInfo.newBuilder();

        messageConvInfoBuilder.setConversationId(conversation.getId());
        if (message != null) {
            messageConvInfoBuilder.setMessageId(message.getId());
            if (message.getReceivedAt() != null) {
                messageConvInfoBuilder.setMessageReceivedAt(message.getReceivedAt().toString(DATE_FORMAT));
            }
            if (message.getMessageDirection() != null && message.getMessageDirection() != MessageDirection.UNKNOWN) {
                messageConvInfoBuilder.setMessageDirection(Entities.MessageDirection.valueOf(message.getMessageDirection().name()));
            }
            if (message.getState() != null) {
                messageConvInfoBuilder.setMessageState(message.getState().name());
            }
        }

        if (conversation.getCustomValues() != null) {
            String replyChannel = conversation.getCustomValues().get(REPLY_CHANNEL_HEADER);
            if (replyChannel != null) {
                messageConvInfoBuilder.setReplyChannel(replyChannel);
            }
        }

        return messageConvInfoBuilder.build();
    }
}
