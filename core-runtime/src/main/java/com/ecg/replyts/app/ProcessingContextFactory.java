package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.Attachment;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.mailcloaking.AnonymizedMailConverter;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

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

    private final AnonymizedMailConverter anonymizedMailConverter;

    @Autowired
    public ProcessingContextFactory(AnonymizedMailConverter anonymizedMailConverter) {
        this.anonymizedMailConverter = anonymizedMailConverter;
    }

    /**
     * Creates and returns a new MessageProcessingContext, that holds the given mail and the given message id. messageId must be globally unique.
     */
    public MessageProcessingContext newContext(@Nonnull Mail mail, String messageId) {
        return newContext(mail, messageId, newProcessingTimeGuard());
    }

    /**
     * Creates and returns a new MessageProcessingContext, that holds the given mail and the given message id. messageId must be globally unique.
     */
    public MessageProcessingContext newContext(@Nonnull Mail mail, String messageId, ProcessingTimeGuard processingTimeGuard) {
        Collection<Attachment> attachments = mail.getAttachmentNames().stream()
                .map(name -> new Attachment(name, mail.getAttachment(name).getContent()))
                .collect(Collectors.toSet());
        return new MessageProcessingContext(mail, messageId, processingTimeGuard, createOverrideRecipientOverride(),
                attachments);
    }

    public MessageProcessingContext newContext(String messageId, @Nonnull Collection<Attachment> attachments) {
        return new MessageProcessingContext(null, messageId, newProcessingTimeGuard(), createOverrideRecipientOverride(),
                attachments);
    }

    private ProcessingTimeGuard newProcessingTimeGuard() {
        return new ProcessingTimeGuard(maxMessageProcessingTimeSeconds);
    }

    private BiPredicate<MailAddress, MailAddress> createOverrideRecipientOverride() {
        return (toRecipient, storedRecipient) -> {
            if (overrideRecipient && !anonymizedMailConverter.isCloaked(toRecipient.getAddress())) {
                String toRecipientAddress = toRecipient.getAddress();
                String storedRecipientAddress = storedRecipient.getAddress();

                return StringUtils.isNotBlank(toRecipientAddress) && !toRecipientAddress.equalsIgnoreCase(storedRecipientAddress);
            }

            return false;
        };
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
