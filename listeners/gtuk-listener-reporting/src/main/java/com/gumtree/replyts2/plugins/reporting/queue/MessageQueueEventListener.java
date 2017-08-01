package com.gumtree.replyts2.plugins.reporting.queue;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Listener responsible for providing a snapshot of held messages in queues for processing by CS. The queue table is
 * added to for messages which get 'HELD' and removed from when they are 'timed out', approved or declined by an agent
 */
public class MessageQueueEventListener implements MessageProcessedListener {
    private MessageQueueManager messageQueueManager;
    private Clock clock;
    private static final Logger LOG = LoggerFactory.getLogger(MessageQueueEventListener.class);

    public MessageQueueEventListener(MessageQueueManager messageQueueManager, Clock clock) {
        this.messageQueueManager = messageQueueManager;
        this.clock = clock;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (!getCategoryId(conversation).isPresent()) {
            return;
        }

        if (held(message)) {
            LOG.debug("Held message to be inserted into db queue: %s", message.getId());
            messageQueueManager.enQueueMessage(message.getId(), getCategoryId(conversation).get(), clock.now());
            LOG.debug("Held message queued: %s", message.getId());
        } else if (message.getHumanResultState().isAcceptableOutcome()) {
            LOG.debug("Held message actioned and will be removed from db queue: %s", message.getId());
            boolean success = messageQueueManager.deQueueMessage(message.getId());
            if (success) {
                LOG.debug("Held message removed from db queue: %s", message.getId());
            }
        }
    }

    private Optional<Long> getCategoryId(Conversation conversation) {
        String categoryId = conversation.getCustomValues().get("categoryid");
        try {
            return Optional.of(Long.parseLong(categoryId));
        } catch (NumberFormatException nfe) {
            LOG.warn("No category Id found when attempting to add to message queue! {}", nfe.getMessage());
            return Optional.empty();
        }
    }

    private boolean held(Message message) {
        return FilterResultState.HELD.equals(message.getFilterResultState())
                && ModerationResultState.UNCHECKED.equals(message.getHumanResultState());
    }
}
