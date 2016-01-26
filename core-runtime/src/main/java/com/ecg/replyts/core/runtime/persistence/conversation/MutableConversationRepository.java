package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;

import java.util.List;

/**
 * Adds ability to persist events to {@link ConversationRepository}.
 * Is used from {@link DefaultMutableConversation}.
 */
public interface MutableConversationRepository extends ConversationRepository {

    /**
     * Commit some events for a given conversation.
     *
     * @param conversationId      the conversation to commit to (not empty)
     * @param toBeCommittedEvents the events to commit (not null)
     */
    void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents);

    /**
     * Deletes a specific conversation
     */
    void deleteConversation(Conversation c);
}
