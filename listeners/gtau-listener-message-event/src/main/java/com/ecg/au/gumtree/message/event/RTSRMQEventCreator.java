package com.ecg.au.gumtree.message.event;

import com.ebay.ecg.australia.events.entity.Entities;
import com.ebay.ecg.australia.events.event.message.MessageEvents;
import com.ebay.ecg.australia.events.origin.OriginDefinition;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by fmiri on 23/03/2017.
 *
 * @author Ima Miri <fmiri@ebay.com>
 */
public class RTSRMQEventCreator {

    private static Logger LOG = LoggerFactory.getLogger(RTSRMQEventCreator.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String REPLY_CHANNEL_HEADER = "X-Reply-Channel";
    private static final String USER_AGENT = "User-Agent";
    private static final String CATEGORY_ID = "categoryid";
    private static final String IP = "ip";

    @Autowired
    private RabbitMQEventHandlerClient eventHandlerClient;

    public void messageEventEntry(final Conversation conversation, final Message message) {

        try {
            final MessageEvents.MessageCreatedEvent messageRequestCreatedEvent = createMessageEvent(conversation, message);
            eventHandlerClient.fire(messageRequestCreatedEvent);
            LOG.info("Sent ConversationCreatedEvent conversationId: {}, messageId: {}, adId: {}, sellerMail: {}, buyerMail: {}",
                    messageRequestCreatedEvent.getConversationInfo().getMessageConversationInfo().getConversationId(),
                    messageRequestCreatedEvent.getConversationInfo().getMessageConversationInfo().getMessageId(),
                    messageRequestCreatedEvent.getConversationInfo().getAdId(),
                    messageRequestCreatedEvent.getConversationInfo().getSellerMail(),
                    messageRequestCreatedEvent.getConversationInfo().getBuyerMail());
        } catch (Exception e) {
            LOG.error("Error sending ConversationCreatedEvent to RabbitMQ", e);
        }

    }

    private MessageEvents.MessageCreatedEvent createMessageEvent(final Conversation conversation, final Message message) {
        try {
            final MessageEvents.MessageCreatedEvent messageRequestCreatedEvent = createMessageRequestCreatedEvent(conversation, message);

            if (messageRequestCreatedEvent.getConversationInfo() == null) {
                LOG.warn("Failed to insert json event");
            }
            return messageRequestCreatedEvent;
        } catch (Throwable ex) {
            LOG.error("Could not log the event", ex);
        }

        return null;
    }

    /**
     * Create the ConversationCreatedEvent which will be sent to RabbitMQ.
     *
     * @param conversation the Conversation
     * @param message the Message
     *
     * @return ConversationCreatedEvent the event to send to RabbitMQ
     */
    protected MessageEvents.MessageCreatedEvent createMessageRequestCreatedEvent(final Conversation conversation, final Message message) {

        final Entities.MessageConversationInfo messageConversationInfo = Entities.MessageConversationInfo.newBuilder()
                .setConversationId(conversation.getId())
                .setMessageDirection(Entities.MessageDirection.valueOf(message.getMessageDirection().name()))
                .setMessageId(message.getId())
                .setMessageState(message.getState().name())
                .setMessageReceivedAt(message.getReceivedAt().toString(DATE_FORMAT))
                .setReplyChannel(conversation.getCustomValues().get(REPLY_CHANNEL_HEADER))
                .build();

        final Entities.ConversationInfo conversationInfo = Entities.ConversationInfo.newBuilder()
                .setMessageConversationInfo(messageConversationInfo)
                .setConversationState(conversation.getState().name())
                .setAdId(conversation.getAdId())
                .setSellerMail(conversation.getSellerSecret())
                .setBuyerMail(conversation.getBuyerId())
                .setNumOfMessageInConversation(String.valueOf(conversation.getMessages().size()))
                .setConversationCreatedAt(conversation.getCreatedAt().toString(DATE_FORMAT))
                .setConversationLastModifiedDate(conversation.getLastModifiedAt().toString(DATE_FORMAT))
                .setCategoryId(conversation.getCustomValues().get(CATEGORY_ID))
                .setIp(conversation.getCustomValues().get(IP))
                .setUserAgent(conversation.getCustomValues().get(USER_AGENT))
                .build();

        final OriginDefinition.Origin origin = OriginDefinition.Origin.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .build();

        return MessageEvents.MessageCreatedEvent.newBuilder()
                .setOrigin(origin)
                .setConversationInfo(conversationInfo)
                .build();
    }

}