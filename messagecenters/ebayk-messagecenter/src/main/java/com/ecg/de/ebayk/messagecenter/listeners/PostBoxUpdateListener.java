package com.ecg.de.ebayk.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxInitializer;
import com.ecg.de.ebayk.messagecenter.pushmessage.AdImageLookup;
import com.ecg.de.ebayk.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.de.ebayk.messagecenter.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 17:33
 *
 * @author maldana@ebay.de
 */
public class PostBoxUpdateListener implements MessageProcessedListener {

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed") ;

    private static final String API_HOST = System.getProperty("kmobilepush.host", "kapi.mobile.rz");
    private static final Integer API_PORT = Integer.parseInt(System.getProperty("kmobilepush.port", "80"));

    private static final String CUSTOM_VALUE_AD_API_USERID = "ad-api-user-id";

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private final UserNotificationRules userNotificationRules;
    private final PostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdImageLookup adImageLookup;


    @Autowired
    public PostBoxUpdateListener(PostBoxInitializer postBoxInitializer) {
        this.postBoxInitializer = postBoxInitializer;
        this.adImageLookup = new AdImageLookup(API_HOST, API_PORT);
        this.pushService = new PushService(API_HOST, API_PORT);
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {

        if (conversationIsFromOpenImmo(conversation)) {
            // we don't annonymize but send out to cleartext To-address (see replyts2-openimmo-deanonymizer plugin)
            // therefore no messaging in message-box possible -> so don't display it
            return;
        }

        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        Timer.Context timerContext = PROCESSING_TIMER.time();

        try {

            if (conversation.getSellerId() == null || conversation.getBuyerId() == null) {
                LOG.info(
                        String.format(
                                "No seller or buyer email available for conversation #%s and conv-state %s and message #%s",
                                conversation.getId(),
                                conversation.getState(),
                                message.getId()));
                return;
            }

            updateMessageCenter(conversation.getSellerId(), conversation, message, userNotificationRules.sellerShouldBeNotified(message));
            updateMessageCenter(conversation.getBuyerId(), conversation, message, userNotificationRules.buyerShouldBeNotified(message));


            PROCESSING_SUCCESS.inc();

        } catch (Exception e) {
            PROCESSING_FAILED.inc();
            throw new RuntimeException("Error with post-box synching " + e.getMessage(), e);
        } finally {
            timerContext.stop();
        }
    }

    private boolean conversationIsFromOpenImmo(Conversation conv) {
        if (conv.getCustomValues().get(CUSTOM_VALUE_AD_API_USERID) == null) {
            return false;
        }
        return Range.closedOpen(20000, 200000).contains(Integer.parseInt(conv.getCustomValues().get(CUSTOM_VALUE_AD_API_USERID)));
    }



    private void updateMessageCenter(String email, Conversation conversation, Message message, boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(
                email,
                conversation,
                newReplyArrived,
                new PushMessageOnUnreadConversationCallback(
                        pushService,
                        adImageLookup,
                        conversation,
                        message
                ));
    }


}
