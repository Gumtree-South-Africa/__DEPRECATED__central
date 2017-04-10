package com.ebay.ecg.australia.replyts.eventlistener.event;

import com.ebay.ecg.australia.events.entity.Entities;
import com.ebay.ecg.australia.events.event.message.MessageEvents;
import com.ebay.ecg.australia.events.origin.OriginDefinition;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fmiri on 24/03/2017.
 */
public class RTSRMQEventCreator {

    private static Logger LOG = LoggerFactory.getLogger(RTSRMQEventCreator.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String REPLY_CHANNEL_HEADER = "X-Reply-Channel";
    private static final String USER_AGENT = "User-Agent";
    private static final String CATEGORY_ID = "categoryid";
    private static final String IP = "ip";

    private RabbitMQEventHandlerClient eventHandlerClient;

    public void onSetup() {
        LOG.info("RTSRMQEventCreator created.");
    }

    public void messageEventEntry(final Conversation conversation, final Message message) {

        try {
            final MessageEvents.MessageCreatedEvent messageRequestCreatedEvent = createMessageEvent(conversation, message);

            if(messageRequestCreatedEvent != null) {
                eventHandlerClient.fire(messageRequestCreatedEvent);

                LOG.info("Sent MessageCreatedEvent conversationId: {}, messageId: {}, adId: {}, sellerMail: {}, buyerMail: {}",
                        messageRequestCreatedEvent.getConversationInfo().getMessageConversationInfo().getConversationId(),
                        messageRequestCreatedEvent.getConversationInfo().getMessageConversationInfo().getMessageId(),
                        messageRequestCreatedEvent.getConversationInfo().getAdId(),
                        messageRequestCreatedEvent.getConversationInfo().getSellerMail(),
                        messageRequestCreatedEvent.getConversationInfo().getBuyerMail());
            }
        } catch (Exception e) {
            LOG.error("Error sending MessageCreatedEvent to RabbitMQ", e);
        }
    }

    private MessageEvents.MessageCreatedEvent createMessageEvent(final Conversation conversation, final Message message) {
        final MessageEvents.MessageCreatedEvent messageRequestCreatedEvent = createMessageRequestCreatedEvent(conversation, message);

        if (messageRequestCreatedEvent == null) {
            LOG.warn("Failed to create MessageCreatedEvent");
        }
        return messageRequestCreatedEvent;
    }

    /**
     * Create the MessageCreatedEvent which will be sent to RabbitMQ.
     *
     * @param conversation the Conversation
     * @param message the Message
     *
     * @return MessageCreatedEvent the event to send to RabbitMQ
     */
    protected MessageEvents.MessageCreatedEvent createMessageRequestCreatedEvent(final Conversation conversation, final Message message) {
        if(conversation != null) {

            Entities.MessageDirection messageDirection = null;
            String messageId = null;
            String messageState = null;
            String conversationState = null;
            String categoryId = null;
            String ip = null;
            String userAgent = null;
            String replyChannel = null;
            String messageReceivedAt = null;
            String conversationCreatedAt = null;
            String conversationLastModifiedAt = null;
            String messageSize = null;

            Entities.MessageConversationInfo.Builder messageConvInfoBuilder = Entities.MessageConversationInfo.newBuilder();
            Entities.ConversationInfo.Builder conversationInfoBuilder = Entities.ConversationInfo.newBuilder();

            messageConvInfoBuilder.setConversationId(conversation.getId());
            if(message != null) {
                messageId = message.getId();
                messageConvInfoBuilder.setMessageId(messageId);

                if(message.getReceivedAt() != null) {
                    messageReceivedAt = message.getReceivedAt().toString(DATE_FORMAT);
                    messageConvInfoBuilder.setMessageReceivedAt(messageReceivedAt);
                }

                if(message.getMessageDirection() != null && message.getMessageDirection() != MessageDirection.UNKNOWN) {
                    messageDirection = Entities.MessageDirection.valueOf(message.getMessageDirection().name());
                    messageConvInfoBuilder.setMessageDirection(messageDirection);
                }

                if(message.getState() != null) {
                    messageState = message.getState().name();
                    messageConvInfoBuilder.setMessageState(messageState);
                }
            }

            conversationInfoBuilder.setAdId(conversation.getAdId());
            conversationInfoBuilder.setSellerMail(conversation.getSellerId());
            conversationInfoBuilder.setBuyerMail(conversation.getBuyerId());

            if(conversation.getState() != null) {
                conversationState = conversation.getState().name();
                conversationInfoBuilder.setConversationState(conversationState);
            }

            if(conversation.getCreatedAt() != null) {
                conversationCreatedAt = conversation.getCreatedAt().toString(DATE_FORMAT);
                conversationInfoBuilder.setConversationCreatedAt(conversationCreatedAt);
            }

            if(conversation.getLastModifiedAt() != null) {
                conversationLastModifiedAt = conversation.getLastModifiedAt().toString(DATE_FORMAT);
                conversationInfoBuilder.setConversationLastModifiedDate(conversationLastModifiedAt);
            }

            if(conversation.getMessages() != null) {
                messageSize = String.valueOf(conversation.getMessages().size());
                conversationInfoBuilder.setNumOfMessageInConversation(messageSize);
            }

            if(conversation.getCustomValues() != null) {
                categoryId = conversation.getCustomValues().get(CATEGORY_ID);
                if(categoryId != null)
                    conversationInfoBuilder.setCategoryId(categoryId);

                ip = conversation.getCustomValues().get(IP);
                if(ip != null)
                    conversationInfoBuilder.setIp(ip);

                userAgent = conversation.getCustomValues().get(USER_AGENT);
                if(userAgent != null)
                    conversationInfoBuilder.setUserAgent(userAgent);

                replyChannel = conversation.getCustomValues().get(REPLY_CHANNEL_HEADER);
                if(replyChannel != null)
                    messageConvInfoBuilder.setReplyChannel(replyChannel);
            }

            final Entities.MessageConversationInfo messageConversationInfo = messageConvInfoBuilder.build();
            conversationInfoBuilder.setMessageConversationInfo(messageConversationInfo);

            LOG.info("MessageConversationInfo: " + conversation.getId() + ", " + messageDirection + ", " + messageId + ", " + messageState +
                    ", " + messageReceivedAt + ", " + replyChannel);

            final Entities.ConversationInfo conversationInfo = conversationInfoBuilder.build();

            LOG.info("ConversationInfo: " + conversationState + ", " + conversation.getAdId() +
                    ", " + conversation.getSellerId() + ", " + conversation.getBuyerId() + ", " + messageSize +
                    ", " + conversationCreatedAt + ", " +  conversationLastModifiedAt +
                    ", " + categoryId + ", " + ip + ", " + userAgent);

            final OriginDefinition.Origin origin = OriginDefinition.Origin.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            return MessageEvents.MessageCreatedEvent.newBuilder()
                    .setOrigin(origin)
                    .setConversationInfo(conversationInfo)
                    .build();
        } else {
            LOG.info(" No conversation: " + conversation);
            return null;
        }
    }

    public RabbitMQEventHandlerClient getEventHandlerClient() {
        return eventHandlerClient;
    }

    public void setEventHandlerClient(RabbitMQEventHandlerClient eventHandlerClient) {
        this.eventHandlerClient = eventHandlerClient;
    }
}