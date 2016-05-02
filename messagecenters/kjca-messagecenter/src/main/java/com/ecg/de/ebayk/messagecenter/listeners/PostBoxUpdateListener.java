package com.ecg.de.ebayk.messagecenter.listeners;

import ca.kijiji.replyts.AddresserUtil;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.capi.AdInfoLookup;
import com.ecg.de.ebayk.messagecenter.capi.Configuration;
import com.ecg.de.ebayk.messagecenter.capi.UserInfoLookup;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxInitializer;
import com.ecg.de.ebayk.messagecenter.pushmessage.*;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

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
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed");
    private static final Counter PROCESSING_SKIPPED_PRO_AD = TimingReports.newCounter("message-box.postBoxUpdateListener.skipped.pro");

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private final UserNotificationRules userNotificationRules;
    private final PostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final UserInfoLookup userInfoLookup;

    @Autowired
    public PostBoxUpdateListener(PostBoxInitializer postBoxInitializer,
                                 @Value("${push-mobile.enabled:true}") boolean pushEnabled,
                                 @Value("${capi.host:www.dev.kjdev.ca}") String capiHost,
                                 @Value("${capi.port:8081}") Integer capiPort,
                                 @Value("${capi.username:box}") String capiUsername,
                                 @Value("${capi.password:box}") String capiPassword,
                                 @Value("${capi.connectionTimeout:1500}") Integer connectionTimeout,
                                 @Value("${capi.connectionManagerTimeout:1500}") Integer connectionManagerTimeout,
                                 @Value("${capi.socketTimeout:2500}") Integer socketTimeout,
                                 @Value("${capi.maxConnectionsPerHost:40}") Integer maxConnectionsPerHost,
                                 @Value("${capi.retryCount:1}") Integer retryCount,
                                 @Qualifier("pushMessageJmsTemplate") JmsTemplate jmsTemplate) {
        this.postBoxInitializer = postBoxInitializer;

        final Configuration configuration = new Configuration(capiHost, capiPort, capiUsername, capiPassword, connectionTimeout, connectionManagerTimeout, socketTimeout, maxConnectionsPerHost, retryCount);
        this.adInfoLookup = new AdInfoLookup(configuration);
        this.userInfoLookup = new UserInfoLookup(configuration);

        if (pushEnabled) {
            this.pushService = new ActiveMQPushServiceImpl(jmsTemplate);
        } else {
            this.pushService = null;
        }
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {

        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        if (conversation.getSellerId() == null || conversation.getBuyerId() == null) {
            LOG.warn("No seller or buyer email available for conversation [{}] and conv-state [{}] and message [{}]",
                    conversation.getId(),
                    conversation.getState(),
                    message.getId());
            return;
        }

        if (!AddresserUtil.shouldAnonymizeConversation(conversation)) {
            PROCESSING_SKIPPED_PRO_AD.inc();
            LOG.debug("Conversation [{}] should not be anonymized, so not saving message [{}] to postbox",
                    conversation.getId(),
                    message.getId());
            return;
        }

        try (Timer.Context ignored = PROCESSING_TIMER.time()) {

            updateMessageCenter(conversation.getSellerId(), conversation, message, userNotificationRules.sellerShouldBeNotified(message));
            updateMessageCenter(conversation.getBuyerId(), conversation, message, userNotificationRules.buyerShouldBeNotified(message));

            PROCESSING_SUCCESS.inc();

        } catch (Exception e) {
            PROCESSING_FAILED.inc();
            throw new RuntimeException("Error with post-box syncing " + e.getMessage(), e);
        }
    }


    private void updateMessageCenter(String email, Conversation conversation, Message message, boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(
                email,
                conversation,
                newReplyArrived,
                new PushMessageOnUnreadConversationCallback(
                        pushService,
                        adInfoLookup,
                        userInfoLookup,
                        conversation,
                        message
                ));
    }
}
