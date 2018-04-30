package com.ecg.messagecenter.bt.listeners;

import com.ecg.messagecenter.bt.persistence.PostBoxInitializer;
import com.ecg.messagecenter.bt.util.MessageTextHandler;
import com.ecg.messagecenter.core.listeners.UserNotificationRules;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.bt.pushmessage.AdImageLookup;
import com.ecg.messagecenter.bt.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.bt.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;

import static java.lang.String.format;

@Component
public class PostBoxUpdateListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed") ;

    @Autowired
    private PostBoxInitializer postBoxInitializer;

    @Autowired
    private PushService pushService;

    @Autowired
    private AdImageLookup adImageLookup;

    private UserNotificationRules userNotificationRules = new UserNotificationRules();

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }
        
        if (conversation.getState() == ConversationState.CLOSED) {
            return;
        }

        if (MessageTextHandler.isXml(message.getPlainTextBody())) {
            return;
        }

        try (Timer.Context ignore = PROCESSING_TIMER.time()) {
            if (conversation.getSellerId() == null || conversation.getBuyerId() == null) {
                LOG.info(String.format("No seller or buyer email available for conversation #%s and conv-state %s and message #%s",
                  conversation.getId(), conversation.getState(), message.getId()));

                return;
            }

            if (message.getState() == MessageState.SENT || message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
            	updateMessageCenter(conversation.getBuyerId(), conversation, message, userNotificationRules.buyerShouldBeNotified(message));
            }
            
            if (message.getState() == MessageState.SENT || message.getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
            	updateMessageCenter(conversation.getSellerId(), conversation, message, userNotificationRules.sellerShouldBeNotified(message));
            }

            PROCESSING_SUCCESS.inc();
        } catch (Exception e) {
            PROCESSING_FAILED.inc();

            throw new RuntimeException(format("Error with post-box synching %s", e.getMessage()), e);
        }
    }

    private void updateMessageCenter(String email, Conversation conversation, Message message, boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(email, conversation, newReplyArrived,
          new PushMessageOnUnreadConversationCallback(pushService, adImageLookup, conversation, message));
    }
}