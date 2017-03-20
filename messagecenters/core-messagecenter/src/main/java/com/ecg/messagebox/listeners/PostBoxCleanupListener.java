package com.ecg.messagebox.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
public class PostBoxCleanupListener implements ConversationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxCleanupListener.class);

    private final Timer processingTimer = newTimer("message-box.postBoxCleanupListener.timer");
    private final Counter failedCounter = TimingReports.newCounter("message-box.postBoxCleanupListener.failed");

    private final PostBoxService postBoxService;
    private final UserIdentifierService userIdentifierService;

    @Autowired
    public PostBoxCleanupListener(PostBoxService postBoxService, UserIdentifierService userIdentifierService) {
        this.postBoxService = postBoxService;
        this.userIdentifierService = userIdentifierService;
    }

    @Override
    public void eventsTriggered(Conversation conversation, List<ConversationEvent> conversationEvents) {
        for (ConversationEvent event : conversationEvents) {
            if (event instanceof ConversationDeletedEvent) {
                cleanupPostBoxForConversation(conversation);
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private void cleanupPostBoxForConversation(Conversation conversation) {
        try (Timer.Context ignored = processingTimer.time()) {
            // clean buyer and seller projection
            Optional<String> buyerId = userIdentifierService.getBuyerUserId(conversation);
            buyerId.ifPresent(id -> postBoxService.deleteConversation(id, conversation.getId(), conversation.getAdId()));

            Optional<String> sellerId = userIdentifierService.getSellerUserId(conversation);
            sellerId.ifPresent(id -> postBoxService.deleteConversation(id, conversation.getId(), conversation.getAdId()));
        } catch (Exception e) {
            failedCounter.inc();
            LOGGER.error("Failed to clean up postbox for conversation " + conversation.getId(), e);
        }
    }

}
