package com.ecg.de.ebayk.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxInitializer;
import com.ecg.de.ebayk.messagecenter.pushmessage.AdInfoLookup;
import com.ecg.de.ebayk.messagecenter.pushmessage.KmobilePushService;
import com.ecg.de.ebayk.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.de.ebayk.messagecenter.pushmessage.PushService;
import com.ecg.de.ebayk.messagecenter.util.MessageContentHelper;
import com.ecg.de.ebayk.messagecenter.util.MessageType;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 17:33
 *
 * @author maldana@ebay.de
 */
public class PostBoxUpdateListener implements MessageProcessedListener {

    private static final Timer PROCESSING_TIMER =
                    TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS =
                    TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED =
                    TimingReports.newCounter("message-box.postBoxUpdateListener.failed");

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private final UserNotificationRules userNotificationRules;
    private final PostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final Collection<String> disabledDomains;

    @Autowired public PostBoxUpdateListener(PostBoxInitializer postBoxInitializer,
                    @Value("${push-mobile.enabled:true}") boolean pushEnabled,
                    @Value("${push-mobile.host:}") String pushHost,
                    @Value("${push-mobile.port:80}") Integer pushPort,
                    @Value("${api.image.lookup.enabled:false}") Boolean apiEnabled,
                    @Value("${api.host:api.gumtree.com.au}") String apiHost,
                    @Value("${api.port:-1}") Integer apiPort,
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
        this.adInfoLookup = new AdInfoLookup(apiHost, apiPort, apiBasePath, adIdPrefix,
                        connectionTimeout, connectionManagerTimeout, socketTimeout,
                        maxConnectionsPerHost, maxTotalConnections, username, password);
        if (pushEnabled && !Strings.isNullOrEmpty(pushHost)) {
            disabledDomains = Arrays.asList(disabledDomainsString);
            this.pushService = new KmobilePushService(pushHost, pushPort);
        } else {
            disabledDomains = Collections.emptySet();
            this.pushService = null;
        }
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override public void messageProcessed(Conversation conversation, Message message) {

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
                LOG.info(String.format("No seller or buyer email available for conversation #%s and conv-state %s and message #%s",
                                                conversation.getId(), conversation.getState(),
                                                message.getId()));
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

    private void updateMessageCenter(String email, Conversation conversation, Message message,
                    boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(email, conversation, newReplyArrived,
                        new PushMessageOnUnreadConversationCallback(pushService, adInfoLookup,
                                        conversation, message));
    }


}
