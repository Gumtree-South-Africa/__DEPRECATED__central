package com.ecg.messagebox.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagebox.util.messages.MessagesResponseFactory;
import com.ecg.messagecenter.core.listeners.UserNotificationRules;
import com.ecg.replyts.app.postprocessorchain.ContentOverridingPostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.IGNORED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static java.lang.String.format;

/* This listener is order-dependent, it should start before MdePushNotificationListener */
@Component
@Order(value = 500)
public class PostBoxUpdateListener implements MessageProcessedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private static final String SKIP_MESSAGE_CENTER = "skip-message-center";

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
    private final MessagesResponseFactory messagesResponseFactory;
    private final List<ContentOverridingPostProcessor> contentOverridingPostProcessors;

    @Autowired
    public PostBoxUpdateListener(PostBoxService postBoxService,
                                 UserIdentifierService userIdentifierService,
                                 MessageAddedEventProcessor messageAddedEventProcessor,
                                 BlockUserRepository blockUserRepository,
                                 MessagesResponseFactory messagesResponseFactory,
                                 Optional<List<ContentOverridingPostProcessor>> contentOverridingPostProcessors) {
        this.postBoxService = postBoxService;
        this.userIdentifierService = userIdentifierService;
        this.contentOverridingPostProcessors = contentOverridingPostProcessors.orElseGet(ArrayList::new);
        this.userNotificationRules = new UserNotificationRules();
        this.messageAddedEventProcessor = messageAddedEventProcessor;
        this.blockUserRepository = blockUserRepository;
        this.messagesResponseFactory = messagesResponseFactory;
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
            final String buyerUserId = buyerUserIdOpt.get();
            final String sellerUserId = sellerUserIdOpt.get();
            final String msgReceiverUserId = msg.getMessageDirection() == BUYER_TO_SELLER ? sellerUserId : buyerUserId;

            String cleanMsg = messagesResponseFactory.getCleanedMessage(conv, msg);

            for (ContentOverridingPostProcessor contentOverridingPostProcessor : contentOverridingPostProcessors) {
                cleanMsg = contentOverridingPostProcessor.overrideContent(cleanMsg);
            }

            if ((isConversationActive(conv, msg) && isNotDirectionBlocked(buyerUserId, sellerUserId)) || isMessageOwner(msg, BUYER_TO_SELLER)) {
                postBoxService.processNewMessage(buyerUserId, conv, msg, userNotificationRules.buyerShouldBeNotified(msg), cleanMsg);
            } else {
                LOGGER.debug("Direction from the {} to {} is blocked for the message {}", buyerUserId, sellerUserId, msg.getId());
            }

            if ((isConversationActive(conv, msg) && isNotDirectionBlocked(sellerUserId, buyerUserId)) || isMessageOwner(msg, SELLER_TO_BUYER)) {
                postBoxService.processNewMessage(sellerUserId, conv, msg, userNotificationRules.sellerShouldBeNotified(msg), cleanMsg);
            } else {
                LOGGER.debug("Direction from the {} to {} is blocked for the message {}", sellerUserId, buyerUserId, msg.getId());
            }

            messageAddedEventProcessor.publishMessageAddedEvent(conv, msg, cleanMsg, postBoxService.getUnreadCounts(msgReceiverUserId));

            processingSuccessCounter.inc();
        } catch (Exception e) {
            processingFailedCounter.inc();
            throw new RuntimeException(format("Error updating user projections for conversation %s, conversation state %s and message %s: %s",
                    conv.getId(), conv.getState(), msg.getId(), e.getMessage()), e);
        }
    }

    private boolean isMessageOwner(Message msg, MessageDirection direction) {
        return msg.getMessageDirection() == direction;
    }

    private boolean isConversationActive(Conversation conv, Message msg) {
        return msg.getState() == SENT && conv.getState() != ConversationState.CLOSED;
    }

    private boolean isNotDirectionBlocked(String from, String to) {
        return !blockUserRepository.isBlocked(from, to);
    }
}