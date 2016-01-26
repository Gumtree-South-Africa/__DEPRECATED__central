package com.ecg.replyts.core.api.model.conversation;

import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;

/**
 * An extension of {@link Conversation} to make it mutable.
 */
public interface MutableConversation extends Conversation {

    /**
     * @return an immutable snapshot of this conversation
     */
    Conversation getImmutableConversation();

    /**
     * @param command the command to add
     * @throws IllegalArgumentException when the command is not valid
     */
    void applyCommand(ConversationCommand command);

}
