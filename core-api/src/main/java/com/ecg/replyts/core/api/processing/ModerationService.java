package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;

/**
 * Operations for moderating conversations and messages.
 */
public interface ModerationService {

    /**
     * Update the state of a message and process it accordingly.
     *
     * @param conversation     conversation that contains the message (not empty)
     * @param messageId        id of the message to process (not empty)
     * @param moderationAction contains moderation action params (editor, state)
     */
    void changeMessageState(MutableConversation conversation, String messageId, ModerationAction moderationAction) throws MessageNotFoundException;

}
