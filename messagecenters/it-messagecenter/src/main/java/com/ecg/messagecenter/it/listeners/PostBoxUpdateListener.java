package com.ecg.messagecenter.it.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.listeners.UserNotificationRules;
import com.ecg.messagecenter.it.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.it.pushmessage.AdInfoLookup;
import com.ecg.messagecenter.it.pushmessage.KmobilePushService;
import com.ecg.messagecenter.it.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.it.pushmessage.PushService;
import com.ecg.messagecenter.it.util.MessageType;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Component
public class PostBoxUpdateListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed");

    private final UserNotificationRules userNotificationRules;
    private final SimplePostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final Collection<String> disabledDomains;

    @Autowired
    public PostBoxUpdateListener(SimplePostBoxInitializer postBoxInitializer,
                    @Value("${push-mobile.enabled:true}") boolean pushEnabled,
                    @Value("${push-mobile.host:}") String pushHost,
                    @Value("${push-mobile.protocol:https}") String pushProtocol,
                    @Value("${push-mobile.port:80}") Integer pushPort,
                    @Value("${api.host:localhost}") String apiHost,
                    @Value("${api.port:443}") Integer apiPort,
                    @Value("${api.protocol:https}") String apiProtocol,
                    @Value("${api.proxyHost:}") String proxyHost,
                    @Value("${api.proxyPort:}") Integer proxyPort,
                    @Value("${api.basepath:api}") String apiBasePath,
                    @Value("${api.adIdPrefix:}") String adIdPrefix,
                    @Value("${api.username:}") String username,
                    @Value("${api.password:}") String password,
                    @Value("${api.connectionTimeout:1500}") Integer connectionTimeout,
                    @Value("${api.connectionManagerTimeout:1500}") Integer connectionManagerTimeout,
                    @Value("${api.socketTimeout:2500}") Integer socketTimeout,
                    @Value("${api.maxConnectionsPerHost:40}") Integer maxConnectionsPerHost,
                    @Value("${api.maxConnectionsPerHost:40}") Integer maxTotalConnections,
                    @Value("${message-box.disabledDomains:}") String[] disabledDomainsString) {
        this.postBoxInitializer = postBoxInitializer;


        this.adInfoLookup = new AdInfoLookup(apiHost, apiPort, apiProtocol, proxyHost, proxyPort, apiBasePath,
                        adIdPrefix, connectionTimeout, connectionManagerTimeout, socketTimeout,
                        maxConnectionsPerHost, maxTotalConnections, username, password);
        if (pushEnabled && !Strings.isNullOrEmpty(pushHost)) {
            disabledDomains = Arrays.asList(disabledDomainsString);
            this.pushService = new KmobilePushService(pushHost, pushPort, pushProtocol);
        } else {
            disabledDomains = Collections.emptySet();
            this.pushService = null;
        }
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        //        if(MessageContentHelper.isXml(message.getPlainTextBody())) {
        //            return;
        //        }

        Timer.Context timerContext = PROCESSING_TIMER.time();

        try {
            if (!isPushEnabledFor(conversation)) {
                return;
            }

            if (conversation.getSellerId() == null || conversation.getBuyerId() == null) {
                LOG.info("No seller or buyer email available for conversation #{} and conv-state {} and message #{}",
                                                conversation.getId(), conversation.getState(),
                                                message.getId());
                return;
            }

            // We don't broadcast robot messages, to do so UserNotificationRules needs to change
            if (MessageType.isRobot(message)) {
                if (message.getMessageDirection().equals(MessageDirection.BUYER_TO_SELLER)) {
                    updateMessageCenter(conversation.getSellerId(), conversation, message,
                                    userNotificationRules.sellerShouldBeNotified(message));
                }
                if (message.getMessageDirection().equals(MessageDirection.SELLER_TO_BUYER)) {
                    updateMessageCenter(conversation.getBuyerId(), conversation, message,
                                    userNotificationRules.buyerShouldBeNotified(message));
                }
            } else {
                updateMessageCenter(conversation.getSellerId(), conversation, message,
                                userNotificationRules.sellerShouldBeNotified(message));
                updateMessageCenter(conversation.getBuyerId(), conversation, message,
                                userNotificationRules.buyerShouldBeNotified(message));
            }

            PROCESSING_SUCCESS.inc();
        } catch (Exception e) {
            PROCESSING_FAILED.inc();
            throw new RuntimeException("Error with post-box syncing " + e.getMessage(), e);
        } finally {
            timerContext.stop();
        }
    }

    private boolean isPushEnabledFor(Conversation conversation) {
        String buyerDomain = conversation.getCustomValues().get("buyer_domain");
        String sellerDomain = conversation.getCustomValues().get("seller_domain");

        return !(disabledDomains.contains(buyerDomain) || disabledDomains.contains(sellerDomain));
    }

    private void updateMessageCenter(String email, Conversation conversation, Message message, boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(email, conversation, newReplyArrived,
          new PushMessageOnUnreadConversationCallback(pushService, adInfoLookup, conversation, message));
    }
}