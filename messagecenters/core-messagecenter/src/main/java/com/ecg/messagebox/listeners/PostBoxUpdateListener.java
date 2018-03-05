package com.ecg.messagebox.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagebox.util.messages.MessagesResponseFactory;
import com.ecg.messagecenter.listeners.UserNotificationRules;
import com.ecg.replyts.app.postprocessorchain.ContentOverridingPostProcessor;
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

import java.util.ArrayList;
import java.util.List;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private static final String SKIP_MESSAGE_CENTER = "skip-message-center";

    private final Timer processingTimer = newTimer("message-box.postBoxUpdateListener.timer");
    private final Counter processingSuccessCounter = newCounter("message-box.postBoxUpdateListener.success");
    private final Counter processingFailedCounter = newCounter("message-box.postBoxUpdateListener.failed");
    private final Counter missingUserIdsCounter = newCounter("message-box.postBoxUpdateListener.missingUserIdsCounter");

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

        Optional<String> buyerUserIdOpt = userIdentifierService.getBuyerUserId(conv);
        Optional<String> sellerUserIdOpt = userIdentifierService.getSellerUserId(conv);

        if (!buyerUserIdOpt.isPresent() || !sellerUserIdOpt.isPresent()) {
            LOGGER.warn("No buyer or seller id available for conversation {}, conversation state {} and message {}, [buyer/seller]: {}/{}",
                    conv.getId(), conv.getState(), msg.getId(), buyerUserIdOpt.isPresent(), sellerUserIdOpt.isPresent());
            missingUserIdsCounter.inc();
            return;
        }

        try (Timer.Context ignored = processingTimer.time()) {
            final String buyerUserId = buyerUserIdOpt.get();
            final String sellerUserId = sellerUserIdOpt.get();
            final String msgSenderUserId = msg.getMessageDirection() == BUYER_TO_SELLER ? buyerUserId : sellerUserId;
            final String msgReceiverUserId = msg.getMessageDirection() == BUYER_TO_SELLER ? sellerUserId : buyerUserId;

            if (!isDirectionBlocked(msgSenderUserId, msgReceiverUserId) && msg.getState() != IGNORED ) {
                String cleanMsg = messagesResponseFactory.getCleanedMessage(conv, msg);

                for (ContentOverridingPostProcessor contentOverridingPostProcessor : contentOverridingPostProcessors) {
                    cleanMsg = contentOverridingPostProcessor.overrideContent(cleanMsg);
                }

                // update buyer and seller projections
                updateUserProjection(conv, msg, msgSenderUserId, buyerUserId,
                        userNotificationRules.buyerShouldBeNotified(msg), cleanMsg);
                updateUserProjection(conv, msg, msgSenderUserId, sellerUserId,
                        userNotificationRules.sellerShouldBeNotified(msg), cleanMsg);

                messageAddedEventProcessor.publishMessageAddedEvent(conv, msg, cleanMsg,
                        postBoxService.getUnreadCounts(msgReceiverUserId));
            } else {
                LOGGER.debug(format("Direction from the %s to %s is blocked for the message %s", msgSenderUserId, msgReceiverUserId, msg.getId()));
            }

            processingSuccessCounter.inc();
        } catch (Exception e) {
            processingFailedCounter.inc();
            throw new RuntimeException(format("Error updating user projections for conversation %s, conversation state %s and message %s: %s",
                    conv.getId(), conv.getState(), msg.getId(), e.getMessage()), e);
        }
    }

    private void updateUserProjection(Conversation conv, Message msg, String msgSenderUserId, String projectionOwnerUserId,
                                      boolean isNewReply, String cleanMsg) {
        boolean isOwnMessage = msgSenderUserId.equals(projectionOwnerUserId);
        if ((msg.getState() == SENT && conv.getState() != ConversationState.CLOSED) || isOwnMessage) {
            postBoxService.processNewMessage(projectionOwnerUserId, conv, msg, isNewReply, cleanMsg);
        }
    }

    private boolean isDirectionBlocked(String from, String to) {
        return blockUserRepository.areUsersBlocked(from, to);
    }
}