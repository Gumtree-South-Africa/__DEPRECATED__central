package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Abstract PostBox initializer which is typically called from a tenant-specific MessageProcessedListener whenever
 * a new Conversation passes through the COMaaS pipeline.
 *
 * @param <T> the specific type of AbstractConversationType this initializer works with (different tenants have their own implementations - which we will ideally consolidate soon)
 */
public abstract class AbstractSimplePostBoxInitializer<T extends AbstractConversationThread> {
    @Autowired
    protected SimplePostBoxRepository postBoxRepository;

    @Value("${replyts.maxPreviewMessageCharacters:250}")
    protected int maxChars;

    @Value("${replyts.maxConversationAgeDays:180}")
    protected int maxAgeDays;

    public void moveConversationToPostBox(
            String email,
            Conversation conversation,
            boolean newReplyArrived,
            PostBoxWriteCallback postBoxWriteCallback) {
        // We don't want to continue spamming PostBox for user if it was set as ignored before

        if (conversation.isClosedBy(ConversationRole.getRole(email, conversation))) {
            return;
        }

        // Additional filter defined by the tenant implementation (e.g. conversation-level blocking)

        if (filter(email, conversation)) {
            return;
        }

        // Don't display empty conversations and don't add empty messages to existing conversations

        Optional<String> previewLastMessage = extractPreviewLastMessage(conversation, email);
        Optional<? extends AbstractConversationThread> existingThread = postBoxRepository.threadById(email, conversation.getId());

        if (!previewLastMessage.isPresent() || shouldReuseExistingThread(existingThread, previewLastMessage.get())) {
            return;
        }

        // Update the conversation

        Message lastMessage = Iterables.getLast(conversation.getMessages());

        T conversationThread = newConversationThread(email, conversation, newReplyArrived, lastMessage);

        long newUnreadCount = postBoxRepository.upsertThread(email, conversationThread, newReplyArrived);

        postBoxWriteCallback.success(email, newUnreadCount, newReplyArrived);
    }

    private boolean shouldReuseExistingThread(Optional<? extends AbstractConversationThread> existingThread, @Nonnull String newMessage) {
        return existingThread.flatMap(AbstractConversationThread::getPreviewLastMessage)
          .map(last -> last.equals(newMessage)).orElse(false);
    }

    protected abstract boolean filter(String email, Conversation conversation);

    protected abstract Optional<String> extractPreviewLastMessage(Conversation conversation, String email);

    protected abstract T newConversationThread(String email, Conversation conversation, boolean newReplyArrived, Message lastMessage);

    public interface PostBoxWriteCallback {
        void success(String email, Long unreadCount, boolean markedAsUnread);
    }
}
