package com.ecg.messagebox.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagecenter.core.listeners.UserNotificationRules;
import com.ecg.replyts.app.ContentOverridingPostProcessorService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.IGNORED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static java.lang.String.format;

/* This listener is order-dependent, it should start before MdePushNotificationListener */
@Component
@Order(value = 500)
public class PostBoxUpdateListener implements MessageProcessedListener {

    static final String SKIP_MESSAGE_CENTER = "skip-message-center";
    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxUpdateListener.class);
    private final Timer processingTimer = newTimer("message-box.postBoxUpdateListener.core.timer");
    private final Counter processingSuccessCounter = newCounter("message-box.postBoxUpdateListener.core.success");
    private final Counter processingFailedCounter = newCounter("message-box.postBoxUpdateListener.core.failed");
    private final Counter missingBothIdsCounter = newCounter("message-box.postBoxUpdateListener.core.missingBothIds");
    private final Counter missingBuyerIdCounter = newCounter("message-box.postBoxUpdateListener.core.missingBuyerId");

    private final PostBoxService postBoxService;
    private final UserIdentifierService userIdentifierService;
    private final UserNotificationRules userNotificationRules;
    private final MessageAddedEventProcessor messageAddedEventProcessor;
    private final BlockUserRepository blockUserRepository;
    private final ContentOverridingPostProcessorService contentOverridingPostProcessorService;

    @Autowired
    public PostBoxUpdateListener(PostBoxService postBoxService,
                                 UserIdentifierService userIdentifierService,
                                 MessageAddedEventProcessor messageAddedEventProcessor,
                                 BlockUserRepository blockUserRepository,
                                 ContentOverridingPostProcessorService contentOverridingPostProcessorService) {
        this.postBoxService = postBoxService;
        this.userIdentifierService = userIdentifierService;
        this.userNotificationRules = new UserNotificationRules();
        this.messageAddedEventProcessor = messageAddedEventProcessor;
        this.blockUserRepository = blockUserRepository;
        this.contentOverridingPostProcessorService = contentOverridingPostProcessorService;
    }

    @Override
    public void messageProcessed(Conversation conv, Message msg) {
        if ("true".equalsIgnoreCase(conv.getCustomValues().get(SKIP_MESSAGE_CENTER))
                || conv.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        if (msg.getState() == IGNORED) {
            LOGGER.debug("Message is IGNORED and not stored in MessageBox: MessageId {}, ConversationId {}", msg.getId(), conv.getId());
            return;
        }

        Optional<String> buyerUserIdOpt = userIdentifierService.getBuyerUserId(conv);
        Optional<String> sellerUserIdOpt = userIdentifierService.getSellerUserId(conv);

        if (!buyerUserIdOpt.isPresent() || !sellerUserIdOpt.isPresent()) {
            LOGGER.info("No buyer or seller id available for conversation {}, conversation state {} and message {}, [buyer/seller]: {}/{}",
                    conv.getId(), conv.getState(), msg.getId(), buyerUserIdOpt.isPresent(), sellerUserIdOpt.isPresent());

            if (!buyerUserIdOpt.isPresent()) {
                if (!sellerUserIdOpt.isPresent()) {
                    missingBothIdsCounter.inc();
                } else {
                    missingBuyerIdCounter.inc();
                }
            }
            return;
        }

        try (Timer.Context ignored = processingTimer.time()) {
            String cleanMsg = contentOverridingPostProcessorService.getCleanedMessage(conv, msg);

            String senderId = msg.getMessageDirection() == BUYER_TO_SELLER ? buyerUserIdOpt.get() : sellerUserIdOpt.get();
            String receiverId = msg.getMessageDirection() == BUYER_TO_SELLER ? sellerUserIdOpt.get() : buyerUserIdOpt.get();

            if (messageShouldBeVisibleToReceiver(conv, msg, senderId, receiverId)) {
                postBoxService.processNewMessage(receiverId, conv, msg, true, cleanMsg);
            } else {
                LOGGER.debug("Direction from the {} to {} is blocked for the message {}", senderId, receiverId, msg.getId());
            }

            // sender always sees his own message
            postBoxService.processNewMessage(senderId, conv, msg, false, cleanMsg);

            messageAddedEventProcessor.publishMessageAddedEvent(conv, msg, cleanMsg, postBoxService.getUnreadCounts(receiverId));

            processingSuccessCounter.inc();
        } catch (Exception e) {
            processingFailedCounter.inc();
            throw new RuntimeException(format("Error updating user projections for conversation %s, conversation state %s and message %s: %s",
                    conv.getId(), conv.getState(), msg.getId(), e.getMessage()), e);
        }
    }

    public boolean messageShouldBeVisibleToReceiver(Conversation conv, Message msg, String senderId, String receiverId) {
        return isConversationActive(conv, msg) &&
                !blockUserRepository.hasBlocked(receiverId, senderId);
    }

    // note: it is arguably unintuitive that the classification if this message (SENT) determines the 'activeness' of conversation, but note that
    // this is terminology that's apparently used more broadly in our domain.
    private boolean isConversationActive(Conversation conv, Message msg) {
        return msg.getState() == SENT && conv.getState() != ConversationState.CLOSED;
    }
}