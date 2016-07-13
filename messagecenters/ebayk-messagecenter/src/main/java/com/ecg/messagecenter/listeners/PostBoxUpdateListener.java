package com.ecg.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxInitializer;
import com.ecg.messagecenter.pushmessage.AdImageLookup;
import com.ecg.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

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

    private static String API_HOST;
    private static final Integer API_PORT = Integer.parseInt(System.getProperty("kapi.port", "80"));
    private static String MOBILEPUSH_HOST;
    private static final Integer MOBILEPUSH_PORT = Integer.parseInt(System.getProperty("kmobilepush.port", "60021"));

    private static final String CUSTOM_VALUE_AD_API_USERID = "ad-api-user-id";

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private final UserNotificationRules userNotificationRules;
    private final PostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdImageLookup adImageLookup;


    @Autowired
    public PostBoxUpdateListener(PostBoxInitializer postBoxInitializer,
                                 @Value("${kapi.host:}") String kapiSpringProp,
                                 @Value("${kmobilepush.host:}") String kmobileSpringProp) {
        String kapiSysProp = System.getProperty("kapi.host");

        if (isNotEmpty(kapiSysProp)) {
            API_HOST = kapiSysProp;
        } else if (isNotEmpty(kapiSpringProp)) {
            API_HOST = kapiSpringProp;
        } else {
            API_HOST = "kapi.mobile.rz";
        }

        LOG.info("Using API_HOST: " + API_HOST);

        String kmobileSysProp = System.getProperty("kmobilepush.host");

        if (isNotEmpty(kmobileSysProp)) {
            MOBILEPUSH_HOST = kmobileSysProp;
        } else if (isNotEmpty(kmobileSpringProp)) {
            MOBILEPUSH_HOST = kmobileSpringProp;
        } else {
            MOBILEPUSH_HOST = "kmobilepush.mobile.rz";
        }

        LOG.info("Using MOBILEPUSH_HOST: " + MOBILEPUSH_HOST);

        this.postBoxInitializer = postBoxInitializer;
        this.adImageLookup = new AdImageLookup(API_HOST, API_PORT);
        this.pushService = new PushService(MOBILEPUSH_HOST, MOBILEPUSH_PORT);
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
