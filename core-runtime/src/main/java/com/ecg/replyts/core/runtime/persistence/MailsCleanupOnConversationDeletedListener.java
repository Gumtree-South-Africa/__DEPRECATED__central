package com.ecg.replyts.core.runtime.persistence;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression("${persistence.skip.mail.storage:false}")
public class MailsCleanupOnConversationDeletedListener implements ConversationEventListener {
    private final MailRepository mailRepository;

    private static final Logger LOG = LoggerFactory.getLogger(MailsCleanupOnConversationDeletedListener.class);
    private static final Counter CLEANUP_FAILED = TimingReports.newCounter("cleanupConversation-failed");
    private static final Timer CLEANUP_CONVERSATION_MAILS_TIMER = TimingReports.newTimer("cleanupConversation-mails");

    @Autowired
    public MailsCleanupOnConversationDeletedListener(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    @Override
    public void eventsTriggered(Conversation conversation, List<ConversationEvent> conversationEvent) {
        for (ConversationEvent event : conversationEvent) {
            if (event instanceof ConversationDeletedEvent) {
                cleanupMailsOfConversation(conversation);
            }
        }
    }

    @Override
    public int getOrder() {
        // A really low value; we should try to clean up the database as soon as possible.
        return -200;
    }

    private void cleanupMailsOfConversation(Conversation conversation) {
        try (Timer.Context ignored = CLEANUP_CONVERSATION_MAILS_TIMER.time()) {
            for (Message m : conversation.getMessages()) {
                mailRepository.deleteMail(m.getId());
            }
        } catch (Exception e) {
            CLEANUP_FAILED.inc();
            LOG.error("Failed to delete conversation " + conversation.getId(), e);
        }
    }
}
