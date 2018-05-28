package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.mailcloaking.AnonymizedMailConverter;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewDeadConversationCommand;

/**
 * Creates a new Message Processing Context for a given mail and a predefined message id. The message id must be guaranteed to be uniuque.
 */
@Component
public class ProcessingContextFactory {

    @Value("${replyts.maxMessageProcessingTimeSeconds:0}")
    private long maxMessageProcessingTimeSeconds;

    @Value("${email.recipient.override:false}")
    private boolean overrideRecipient;

    @Autowired
    private AnonymizedMailConverter anonymizedMailConverter;

    /**
     * Creates and returns a new MessageProcessingContext, that holds the given mail and the given message id. messageId must be globally unique.
     */
    public MessageProcessingContext newContext(Mail mail, String messageId) {
        return newContext(mail, messageId, new ProcessingTimeGuard(maxMessageProcessingTimeSeconds));
    }

    /**
     * Creates and returns a new MessageProcessingContext, that holds the given mail and the given message id. messageId must be globally unique.
     */
    public MessageProcessingContext newContext(Mail mail, String messageId, ProcessingTimeGuard processingTimeGuard) {
        BiPredicate<MailAddress, MailAddress> overrideRecipientPredicate = (toRecipient, storedRecipient) -> {
            if (overrideRecipient && !anonymizedMailConverter.isCloaked(toRecipient)) {
                String toRecipientAddress = toRecipient.getAddress();
                String storedRecipientAddress = storedRecipient.getAddress();

                return StringUtils.isNotBlank(toRecipientAddress) && !toRecipientAddress.equalsIgnoreCase(storedRecipientAddress);
            }

            return false;
        };

        return new MessageProcessingContext(mail, messageId, processingTimeGuard, overrideRecipientPredicate);
    }

    /**
     * Creates a new dead conversation with a predefined conversation id and adds a new message (with the given message id) to it.
     */
    public DefaultMutableConversation deadConversationForMessageIdConversationId(String messageId, String conversationId, Optional<Mail> mail) {
        NewConversationCommand newConversation = aNewDeadConversationCommand(conversationId).build();

        List<String> textParts = Collections.emptyList();
        if (mail.isPresent()) {
            textParts = mail.get().getPlaintextParts();
        }

        AddMessageCommand addMessage = anAddMessageCommand(conversationId, messageId)
          .withMessageDirection(MessageDirection.UNKNOWN)
          .withTextParts(textParts)
          .withHeaders(findHeaders(mail))
          .build();

        DefaultMutableConversation deadConversation = DefaultMutableConversation.create(newConversation);

        deadConversation.applyCommand(addMessage);

        return deadConversation;
    }

    private Map<String, String> findHeaders(Optional<Mail> mail) {
        return mail.isPresent() ? mail.get().getUniqueHeaders() : Collections.emptyMap();
    }
}
