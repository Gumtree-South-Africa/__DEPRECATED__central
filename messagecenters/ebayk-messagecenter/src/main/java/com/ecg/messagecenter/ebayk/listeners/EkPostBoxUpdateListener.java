package com.ecg.messagecenter.ebayk.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.core.listeners.UserNotificationRules;
import com.ecg.messagecenter.ebayk.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.ebayk.pushmessage.AdImageLookup;
import com.ecg.messagecenter.ebayk.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.ebayk.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EkPostBoxUpdateListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(EkPostBoxUpdateListener.class);

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed") ;

    private static final String CUSTOM_VALUE_AD_API_USERID = "ad-api-user-id";

    private final UserNotificationRules userNotificationRules;
    private final SimplePostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdImageLookup adImageLookup;

    @Autowired
    public EkPostBoxUpdateListener(AdImageLookup adImageLookup,
                                   PushService pushService,
                                   SimplePostBoxInitializer postBoxInitializer) {
        this.postBoxInitializer = postBoxInitializer;
        this.adImageLookup = adImageLookup;
        this.pushService = pushService;
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {

        LOG.error("GEEEEERT");
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
