package com.ecg.messagecenter.core.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagecenter.core.persistence.simple.CassandraSimplePostBoxRepository;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationClosedAndDeletedForUserEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
public class PostBoxV1CleanupListener implements ConversationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxV1CleanupListener.class);

    private final Timer cleanupForUserTimer = newTimer("message-center.postBoxCleanupForUserListener.timer");
    private final Counter failedCleanupForUserCounter = TimingReports.newCounter("message-center.postBoxCleanupForUserListener.failed");

    private final SimplePostBoxRepository postBoxRepository;

    @Autowired
    public PostBoxV1CleanupListener(SimplePostBoxRepository postBoxRepository) {
        this.postBoxRepository = postBoxRepository;
    }

    @Override
    public void eventsTriggered(Conversation conversation, List<ConversationEvent> conversationEvents) {
        for (ConversationEvent event : conversationEvents) {
            if (event instanceof ConversationClosedAndDeletedForUserEvent) {
                deleteConversationForUser(conversation, ((ConversationClosedAndDeletedForUserEvent) event).getUserEmail());
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private void deleteConversationForUser(Conversation conversation, String userEmail) {
        Preconditions.checkArgument(userEmail != null && !userEmail.equals(""), "userId should have a value (convId: '%s')", conversation.getId());

        try (Timer.Context ignored = cleanupForUserTimer.time()) {
            postBoxRepository.deleteConversations(PostBoxId.fromEmail(userEmail), Lists.newArrayList(conversation.getId()));
            LOGGER.info("Deleted conversation with id '{}' from postbox of user with email '{}'", conversation.getId(), userEmail);
        } catch (Exception e) {
            failedCleanupForUserCounter.inc();
            LOGGER.error("Failed to delete conversation with id '{}' from postbox of user with email '{}'", conversation.getId(), userEmail, e);
        }
    }
}
