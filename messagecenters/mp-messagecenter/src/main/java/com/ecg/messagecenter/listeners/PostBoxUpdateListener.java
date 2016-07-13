package com.ecg.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.NewMessageListener;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.pushmessage.AdImageLookup;
import com.ecg.messagecenter.pushmessage.PushMessageOnNewReply;
import com.ecg.messagecenter.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

public class PostBoxUpdateListener implements MessageProcessedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed");

    private static final String CUSTOM_VALUE_SKIP_MC = "skip-message-center";

    private final PostBoxService postBoxDelegatorService;
    private final UserNotificationRules userNotificationRules;
    private final boolean pushEnabled;
    private final PushService pushService;
    private final AdImageLookup adImageLookup;
    private final UserIdentifierService userIdentifierService;

    @Autowired
    public PostBoxUpdateListener(@Qualifier("postBoxDelegatorService") PostBoxService postBoxDelegatorService,
                                 UserIdentifierService userIdentifierService,
                                 @Value("${mobilepush.enabled:false}") boolean pushEnabled,
                                 @Value("${mobilepush.host:localhost}") String pushApiHost,
                                 @Value("${mobilepush.port:80}") int pushApiPort) {

        this.postBoxDelegatorService = postBoxDelegatorService;
        this.pushEnabled = pushEnabled;
        this.adImageLookup = new AdImageLookup(pushApiHost, pushApiPort);
        this.pushService = new PushService(pushApiHost, pushApiPort);
        this.userNotificationRules = new UserNotificationRules();
        this.userIdentifierService = userIdentifierService;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (skipMessageCentreForMessage(conversation)) {
            return;
        }

        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        if (conversation.getSellerId() == null || conversation.getBuyerId() == null) {
            LOGGER.info(
                    String.format(
                            "No seller or buyer email available for conversation %s and conv-state %s and message %s",
                            conversation.getId(),
                            conversation.getState(),
                            message.getId()));
            return;
        }

        try (Timer.Context ignored = PROCESSING_TIMER.time()) {

            Optional<String> buyerIdOptional = updateMessageCenterForBuyerIfFound(conversation, message);
            Optional<String> sellerIdOptional = updateMessageCenterForSellerIfFound(conversation, message);

            if (buyerIdOptional.isPresent() || sellerIdOptional.isPresent()) {
                PROCESSING_SUCCESS.inc();
            }
        } catch (Exception e) {
            PROCESSING_FAILED.inc();
            LOGGER.error(
                    String.format(
                            "Error with post-box synching for conversation %s and conv-state %s and message %s",
                            conversation.getId(),
                            conversation.getState(),
                            message.getId()), e
            );
        }
    }

    private Optional<String> updateMessageCenterForSellerIfFound(Conversation conversation, Message message) {
        Optional<String> sellerIdOptional = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Seller);
        if (sellerIdOptional.isPresent()) {
            updateMessageCenter(sellerIdOptional.get(), ConversationRole.Seller, conversation, message, userNotificationRules.sellerShouldBeNotified(message));
        }
        return sellerIdOptional;
    }

    private Optional<String> updateMessageCenterForBuyerIfFound(Conversation conversation, Message message) {
        Optional<String> buyerIdOptional = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Buyer);
        if (buyerIdOptional.isPresent()) {
            updateMessageCenter(buyerIdOptional.get(), ConversationRole.Buyer, conversation, message, userNotificationRules.buyerShouldBeNotified(message));
        }
        return buyerIdOptional;
    }

    private boolean skipMessageCentreForMessage(Conversation conv) {
        return conv.getCustomValues().get(CUSTOM_VALUE_SKIP_MC) != null
                && conv.getCustomValues().get(CUSTOM_VALUE_SKIP_MC).equalsIgnoreCase("true");
    }

    private void updateMessageCenter(String userId, ConversationRole role, Conversation conversation, Message message, boolean newReplyArrived) {
        Optional<NewMessageListener> callbackOptional;
        if (newReplyArrived) {
            callbackOptional = createPushMessageCallback(conversation, message);
        } else {
            callbackOptional = Optional.empty();
        }

        postBoxDelegatorService.processNewMessage(userId, conversation, message, role, newReplyArrived, callbackOptional);
    }

    private Optional<NewMessageListener> createPushMessageCallback(Conversation conversation, Message message) {
        if (pushEnabled) {
            return Optional.of(new PushMessageOnNewReply(
                    pushService,
                    adImageLookup,
                    conversation,
                    message
            ));
        } else {
            return Optional.empty();
        }
    }
}
