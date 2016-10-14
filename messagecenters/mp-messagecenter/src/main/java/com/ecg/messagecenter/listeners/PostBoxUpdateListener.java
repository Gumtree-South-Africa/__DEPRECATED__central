package com.ecg.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagebox.identifier.UserIdentifierService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.IGNORED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
public class PostBoxUpdateListener implements MessageProcessedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private static final String SKIP_MESSAGE_CENTER = "skip-message-center";

    private final Timer processingTimer = newTimer("message-box.postBoxUpdateListener.timer");
    private final Counter processingSuccessCounter = newCounter("message-box.postBoxUpdateListener.success");
    private final Counter processingFailedCounter = newCounter("message-box.postBoxUpdateListener.failed");
    private final Counter missingUserIdsCounter = newCounter("message-box.postBoxUpdateListener.missingUserIdsCounter");

    private final PostBoxService postBoxService;
    private final UserIdentifierService userIdentifierService;
    private final UserNotificationRules userNotificationRules;

    @Autowired
    public PostBoxUpdateListener(PostBoxService postBoxService,
                                 UserIdentifierService userIdentifierService) {
        this.postBoxService = postBoxService;
        this.userIdentifierService = userIdentifierService;
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conv, Message msg) {
        if ("true".equalsIgnoreCase(conv.getCustomValues().get(SKIP_MESSAGE_CENTER))
                || conv.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        Optional<String> buyerUserIdOpt = userIdentifierService.getUserIdentificationOfConversation(conv, ConversationRole.Buyer);
        Optional<String> sellerUserIdOpt = userIdentifierService.getUserIdentificationOfConversation(conv, ConversationRole.Seller);

        if (!buyerUserIdOpt.isPresent() || !sellerUserIdOpt.isPresent()) {
            LOGGER.error(String.format("No buyer or seller id available for conversation %s, conversation state %s and message %s",
                    conv.getId(), conv.getState(), msg.getId()));
            missingUserIdsCounter.inc();
            return;
        }

        try (Timer.Context ignored = processingTimer.time()) {
            String buyerUserId = buyerUserIdOpt.get();
            String sellerUserId = sellerUserIdOpt.get();
            String msgSenderUserId = msg.getMessageDirection() == BUYER_TO_SELLER ? buyerUserId : sellerUserId;

            // update buyer and seller projections
            updateUserProjection(conv, msg, msgSenderUserId, buyerUserId,
                    userNotificationRules.buyerShouldBeNotified(msg));
            updateUserProjection(conv, msg, msgSenderUserId, sellerUserId,
                    userNotificationRules.sellerShouldBeNotified(msg));

            processingSuccessCounter.inc();
        } catch (Exception e) {
            processingFailedCounter.inc();
            throw new RuntimeException(String.format("Error updating user projections for conversation %s, conversation state %s and message %s: %s",
                    conv.getId(), conv.getState(), msg.getId(), e.getMessage()), e);
        }
    }

    private void updateUserProjection(Conversation conv, Message msg, String msgSenderUserId, String projectionOwnerUserId,
                                      boolean isNewReply) {
        boolean isOwnMessage = msgSenderUserId.equals(projectionOwnerUserId);
        if (msg.getState() != IGNORED && ((msg.getState() == SENT && conv.getState() != ConversationState.CLOSED) || isOwnMessage)) {
            postBoxService.processNewMessage(projectionOwnerUserId, conv, msg, isNewReply);
        }
    }
}