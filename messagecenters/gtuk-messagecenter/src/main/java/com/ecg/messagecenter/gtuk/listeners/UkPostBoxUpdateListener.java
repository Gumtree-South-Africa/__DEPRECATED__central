package com.ecg.messagecenter.gtuk.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.core.listeners.UserNotificationRules;
import com.ecg.messagecenter.gtuk.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.gtuk.pushmessage.AdInfoLookup;
import com.ecg.messagecenter.gtuk.pushmessage.KmobilePushService;
import com.ecg.messagecenter.gtuk.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.gtuk.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.conversation.ConversationState.DEAD_ON_ARRIVAL;

@Component
public class UkPostBoxUpdateListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(UkPostBoxUpdateListener.class);

    private static final SimplePostBoxInitializer.PostBoxWriteCallback NO_OP = (email1, unreadCount, markedAsUnread) -> {
    };
    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed");
    private static final Counter MESSAGE_HELD_COUNTER = TimingReports.newCounter("message-box.postBoxUpdateListener.message-held");
    private static final Counter MESSAGE_BLOCKED_BY_CS_COUNTER = TimingReports.newCounter("message-box.postBoxUpdateListener.message-blocked-by-cs");
    private static final Counter MESSAGE_ALLOWED_BY_CS_COUNTER = TimingReports.newCounter("message-box.postBoxUpdateListener.message-allowed-by-cs");

    private final UserNotificationRules userNotificationRules;
    private final SimplePostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;

    @Autowired
    public UkPostBoxUpdateListener(SimplePostBoxInitializer postBoxInitializer,
                                   @Value("${push-mobile.enabled:false}") boolean pushEnabled,
                                   @Value("${push-mobile.host:example.com}") String pushHost,
                                   @Value("${push-mobile.port:80}") Integer pushPort,
                                   @Value("${api.image.lookup.enabled:false}") Boolean apiEnabled,
                                   @Value("${api.host:example.com}") String apiHost,
                                   @Value("${api.port:80}") Integer apiPort) {
        this.postBoxInitializer = postBoxInitializer;
        this.adInfoLookup = new AdInfoLookup(apiHost, apiPort);
        if (pushEnabled) {
            this.pushService = new KmobilePushService(pushHost, pushPort);
        } else {
            this.pushService = null;
        }
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        checkAndCountMessageState(message);
        if (conversation.getState() != DEAD_ON_ARRIVAL) {
            if (conversation.getSellerId() != null && conversation.getBuyerId() != null) {
                processMessage(conversation, message);
            } else {
                LOG.info("No seller or buyer email available for conversation #{} and conv-state {} and message #{}",
                        conversation.getId(), conversation.getState(), message.getId());
            }
        }
    }

    private void processMessage(Conversation conversation, Message message) {
        try (Timer.Context ignore = PROCESSING_TIMER.time()) {
            updateMessageCenter(conversation.getSellerId(), conversation, message,
                    userNotificationRules.sellerShouldBeNotified(message));
            updateMessageCenter(conversation.getBuyerId(), conversation, message,
                    userNotificationRules.buyerShouldBeNotified(message));
            PROCESSING_SUCCESS.inc();
        } catch (Exception e) {
            PROCESSING_FAILED.inc();
            throw new RuntimeException("Error with post-box syncing " + e.getMessage(), e);
        }
    }

    private void checkAndCountMessageState(Message message) {
        if (FilterResultState.HELD == message.getFilterResultState()) {
            MESSAGE_HELD_COUNTER.inc();
        }

        if (ModerationResultState.GOOD == message.getHumanResultState()) {
            MESSAGE_ALLOWED_BY_CS_COUNTER.inc();
        } else if (ModerationResultState.BAD == message.getHumanResultState()) {
            MESSAGE_BLOCKED_BY_CS_COUNTER.inc();
        }
    }

    private void updateMessageCenter(String email, Conversation conversation, Message message, boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(
                email,
                conversation,
                newReplyArrived,
                getPostBoxWriteCallback(conversation, message)
        );
    }

    private SimplePostBoxInitializer.PostBoxWriteCallback getPostBoxWriteCallback(Conversation conversation, Message message) {
        return (pushService == null)
                ? NO_OP
                : new PushMessageOnUnreadConversationCallback(pushService, adInfoLookup, conversation, message);
    }
}