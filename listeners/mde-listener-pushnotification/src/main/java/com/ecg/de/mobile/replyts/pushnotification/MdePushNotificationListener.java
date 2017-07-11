package com.ecg.de.mobile.replyts.pushnotification;

import com.ecg.de.mobile.replyts.pushnotification.cassandra.CassandraMessageRepository;
import com.ecg.de.mobile.replyts.pushnotification.model.MessageMetadata;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/* This listener is order-dependent, it should start after PostBoxUpdateListener */
@Component
@Order(value = 1000)
public class MdePushNotificationListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(MdePushNotificationListener.class);

    private static final String HEADER_TITLE = "X-Conversation-Title";

    private static final String PROPERTY_SELLER_ID = "seller_customer_id";
    private static final String PROPERTY_BUYER_ID = "buyer_customer_id";

    @Autowired
    private NotificationSender sender;

    @Autowired
    private CassandraMessageRepository messageRepository;

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        String customerId = getCustomerId(conversation, message);
        if (isAnonymousId(customerId)) {
            LOG.info("Skip push notification for anonymous customer {}", customerId);
            return;
        }
        messageRepository
                .getLastMessage(customerId, conversation.getId())
                .map(com.ecg.de.mobile.replyts.pushnotification.model.Message::getMetadata)
                .map(MessageMetadata::getText)
                .ifPresent(text -> sendPushNotification(conversation, customerId, text));
    }

    private void sendPushNotification(Conversation conversation, String customerId, String text) {
        LOG.debug("Sending push notification to customer {}", customerId);
        sender.send(new MdePushMessagePayload(
                conversation.getId(),
                convertAdIdToNumeric(conversation.getAdId()),
                text,
                customerId,
                getTitle(conversation)
        ));
    }

    private String getCustomerId(Conversation conversation, com.ecg.replyts.core.api.model.conversation.Message msg) {
        if (msg.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
            return conversation.getCustomValues().get(PROPERTY_SELLER_ID);
        }
        return conversation.getCustomValues().get(PROPERTY_BUYER_ID);
    }

    /* For anonymous users system uses email, for logged in - customerId of type Long */
    private boolean isAnonymousId(String id) {
        return !StringUtils.isNumeric(id);
    }

    private String convertAdIdToNumeric(String adId) {
        return adId.replaceAll("\\D+", "");
    }

    private String getTitle(Conversation conversation) {
        return conversation.getMessages().stream()
                .filter(m -> m.getHeaders().containsKey(HEADER_TITLE))
                .findFirst()
                .map(m -> m.getHeaders().get(HEADER_TITLE))
                .orElse("");
    }
}
