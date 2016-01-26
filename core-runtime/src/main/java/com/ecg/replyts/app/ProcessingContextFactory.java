package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewDeadConversationCommand;

/**
 * Creates a new Message Processing Context for a given mail and a predefined message id. The message id must be guaranteed to be uniuque.
 */
class ProcessingContextFactory {

    private final long maxMessageProcessingTimeSeconds;

    ProcessingContextFactory(long maxMessageProcessingTimeSeconds) {
        this.maxMessageProcessingTimeSeconds = maxMessageProcessingTimeSeconds;
    }

    /**
     * Creates and returns a new MessageProcessingContext, that holds the given mail and the given message id. messageId must be globally unique.
     */
    public MessageProcessingContext newContext(Mail mail, String messageId) {
        return new MessageProcessingContext(mail, messageId, new ProcessingTimeGuard(maxMessageProcessingTimeSeconds));
    }

    /**
     * Creates a new dead conversation with a predefined conversation id and adds a new message (with the given message id) to it.
     */
    public DefaultMutableConversation deadConversationForMessageIdConversationId(String messageId, String conversationId, Optional<Mail> mail) {
        NewConversationCommand newConversation = aNewDeadConversationCommand(conversationId).build();
        AddMessageCommand addMessage = anAddMessageCommand(conversationId, messageId)
                .withMessageDirection(MessageDirection.UNKNOWN)
                .withPlainTextBody(findText(mail))
                .withHeaders(findHeaders(mail))
                .build();

        DefaultMutableConversation deadConversation = DefaultMutableConversation.create(newConversation);
        deadConversation.applyCommand(addMessage);
        return deadConversation;
    }

    private String findText(Optional<Mail> mail) {
        if (!mail.isPresent()) {
            return "";
        }
        List<String> plaintextParts = mail.get().getPlaintextParts();
        if (!plaintextParts.isEmpty()) {
            return plaintextParts.get(0);
        }
        return "";
    }

    private Map<String, String> findHeaders(Optional<Mail> mail) {
        return mail.isPresent() ? mail.get().getUniqueHeaders() : Collections.<String, String>emptyMap();
    }

}
