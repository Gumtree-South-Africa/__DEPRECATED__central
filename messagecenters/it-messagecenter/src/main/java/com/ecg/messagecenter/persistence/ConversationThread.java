package com.ecg.messagecenter.persistence;

import org.joda.time.DateTime;

import java.util.Optional;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 15:40
 *
 * @author maldana@ebay.de
 */
public class ConversationThread extends AbstractConversationThread {


    //introduced later therefore Option to be compatible with persistent data
    private Optional<String> robot;
    private Optional<String> offerId;


    public ConversationThread(String adId, String conversationId, DateTime createdAt,
                    DateTime modifiedAt, DateTime receivedAt, boolean containsUnreadMessages,
                    Optional<String> previewLastMessage, Optional<String> buyerName,
                    Optional<String> sellerName, Optional<String> buyerId,
                    Optional<String> messageDirection, Optional<String> robot,
                    Optional<String> offerId) {
        super(adId, conversationId, createdAt, modifiedAt, receivedAt, containsUnreadMessages, previewLastMessage, buyerName, sellerName, buyerId, messageDirection);

        this.robot = robot;
        this.offerId = offerId;
    }

    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = Optional.ofNullable(message);
        if (!actualMessage.isPresent()) actualMessage = previewLastMessage;

        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(),
                        DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId,
                        messageDirection, robot, offerId);
    }

    public Optional<String> getRobot() {
        return robot;
    }

    public Optional<String> getOfferId() {
        return offerId;
    }
}
