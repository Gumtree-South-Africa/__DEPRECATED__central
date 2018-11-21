package com.ecg.messagecenter.kjca.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.comaas.kjca.coremod.shared.AddresserUtil;
import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.core.listeners.UserNotificationRules;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.capi.AdInfoLookup;
import com.ecg.messagecenter.kjca.capi.CommonApiConfig;
import com.ecg.messagecenter.kjca.capi.HttpClientConfig;
import com.ecg.messagecenter.kjca.capi.UserInfoLookup;
import com.ecg.messagecenter.kjca.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.kjca.pushmessage.ActiveMQPushServiceImpl;
import com.ecg.messagecenter.kjca.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.kjca.pushmessage.PushService;
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
import org.springframework.stereotype.Component;

@Component
public class KjcaPostBoxUpdateListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(KjcaPostBoxUpdateListener.class);

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed");
    private static final Counter PROCESSING_SKIPPED_PRO_AD = TimingReports.newCounter("message-box.postBoxUpdateListener.skipped.pro");

    private final UserNotificationRules userNotificationRules;
    private final SimplePostBoxRepository postBoxRepository;
    private final SimplePostBoxInitializer postBoxInitializer;
    private final PushService sendPushService;
    private final AdInfoLookup adInfoLookup;
    private final UserInfoLookup userInfoLookup;
    private final TextAnonymizer textAnonymizer;
    private final UnreadCountCacher unreadCountCacher;

    @Autowired
    public KjcaPostBoxUpdateListener(SimplePostBoxInitializer postBoxInitializer,
                                     SimplePostBoxRepository postBoxRepository,
                                     @Value("${push-mobile.enabled:true}") boolean pushEnabled,
                                     @Value("${capi.scheme:http}") String capiScheme,
                                     @Value("${capi.host:www.dev.kjdev.ca}") String capiHost,
                                     @Value("${capi.port:8081}") int capiPort,
                                     @Value("${capi.username:box}") String capiUsername,
                                     @Value("${capi.password:box}") String capiPassword,
                                     @Value("${capi.connectionTimeout:1500}") int connectionTimeout,
                                     @Value("${capi.connectionManagerTimeout:1500}") int connectionManagerTimeout,
                                     @Value("${capi.socketTimeout:2500}") int socketTimeout,
                                     @Value("${capi.maxConnectionsPerHost:40}") int maxConnectionsPerHost,
                                     @Value("${capi.retryCount:1}") int retryCount,
                                     @Qualifier("sendPushService") PushService sendPushService,
                                     TextAnonymizer textAnonymizer,
                                     UnreadCountCacher unreadCountCacher) {
        this.postBoxInitializer = postBoxInitializer;
        this.postBoxRepository = postBoxRepository;
        this.textAnonymizer = textAnonymizer;
        this.unreadCountCacher = unreadCountCacher;

        final HttpClientConfig httpClientConfig = new HttpClientConfig(connectionTimeout, connectionManagerTimeout, socketTimeout, maxConnectionsPerHost, retryCount);
        final CommonApiConfig commonApiConfig = new CommonApiConfig(capiHost, capiPort, capiScheme, capiUsername, capiPassword);
        this.adInfoLookup = new AdInfoLookup(httpClientConfig, commonApiConfig);
        this.userInfoLookup = new UserInfoLookup(httpClientConfig, commonApiConfig);

        if (pushEnabled) {
            this.sendPushService = sendPushService;
        } else {
            this.sendPushService = null;
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
            updateMessageCenter(conversation.getSellerId(), conversation, message, userNotificationRules.sellerShouldBeNotified(message.getState(), message.getMessageDirection()));
            updateMessageCenter(conversation.getBuyerId(), conversation, message, userNotificationRules.buyerShouldBeNotified(message.getState(), message.getMessageDirection()));

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
                        postBoxRepository,
                        sendPushService,
                        textAnonymizer,
                        adInfoLookup,
                        userInfoLookup,
                        conversation,
                        message
                ),
                unreadCountCacher);
    }
}
