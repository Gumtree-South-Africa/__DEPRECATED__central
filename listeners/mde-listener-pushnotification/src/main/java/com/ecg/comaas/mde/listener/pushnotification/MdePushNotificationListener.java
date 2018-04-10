package com.ecg.comaas.mde.listener.pushnotification;

import com.ecg.comaas.mde.listener.pushnotification.cassandra.CassandraMessageRepository;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
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
    private static final String X_CUST_FROM_USERID = "X-Cust-From-Userid";
    private static final String X_CUST_TO_USERID = "X-Cust-To-Userid";

    @Autowired
    private NotificationSender sender;

    @Autowired
    private CassandraMessageRepository messageRepository;

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        LOG.trace("processing conversation {}", conversation.getId());
        final String senderId = message.getHeaders().get(X_CUST_FROM_USERID);
        final String recipientId = message.getHeaders().get(X_CUST_TO_USERID);

        if (isAnonymousId(recipientId)) {
            LOG.trace("skip push notification - anonymous recipient {}", recipientId);
            return;
        }
        if (isOwnMessage(recipientId, senderId)) {
            LOG.debug("skip push notification - recipient and sender are the same. Recipient {}, sender {}.", recipientId, senderId);
            return;
        }

        messageRepository
                .getLastMessage(recipientId, conversation.getId())
                .map(com.ecg.comaas.mde.listener.pushnotification.model.Message::getMetadata)
                .ifPresent(msgMeta -> sendPushNotification(conversation, recipientId, senderId, msgMeta.getText()));
    }

    private void sendPushNotification(Conversation conversation, String recipientId, String senderId, String text) {
        LOG.trace("Sending push notification to customer [{}]. Message sender was [{}] ...", recipientId, senderId);
        sender.send(new MdePushMessagePayload(
                conversation.getId(),
                convertAdIdToNumeric(conversation.getAdId()),
                text,
                recipientId,
                getTitle(conversation)
        ));
    }

    private boolean isOwnMessage(String recipient, String senderUserId) {
        return recipient.equals(senderUserId);
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
