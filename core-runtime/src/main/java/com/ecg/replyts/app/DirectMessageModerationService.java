package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.MessageModeratedCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.indexer.DocumentSink;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.MessageEventPublisher;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.joda.time.DateTime.now;

/**
 * Handles Moderation results from CS agents, forwarded via REST endpoint. If the result state is GOOD or TIMED_OUT, the
 * message will be sent; if it is BAD, it will end up BLOCKED.
 */
@Service
public class DirectMessageModerationService implements ModerationService {
    // Choose a quite long time, post processing shouldn't be the processing problem
    private static final long MAX_MESSAGE_PROCESSING_TIME_SECONDS = 300L;

    private static final Logger LOG = LoggerFactory.getLogger(DirectMessageModerationService.class);

    @Autowired
    private MutableConversationRepository conversationRepository;

    @Autowired
    private DocumentSink documentSink;

    @Autowired(required = false)
    private List<MessageProcessedListener> listeners = emptyList();

    @Autowired
    private HeldMailRepository heldMailRepository;

    @Autowired
    private ProcessingFlow flow;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired
    private ProcessingContextFactory processingContextFactory;

    @Autowired(required = false)
    private AttachmentRepository attachmentRepository;

    @Autowired
    private MessageEventPublisher messageEventPublisher;

    @Value("${replyts.tenant.short}")
    private String shortTenantName;

    @Override
    public void changeMessageState(MutableConversation conversation, String messageId, ModerationAction moderationAction) throws MessageNotFoundException {
        Preconditions.checkArgument(moderationAction.getModerationResultState().isAcceptableOutcome(), "Moderation State " + moderationAction.getModerationResultState() + " is not an acceptable moderation outcome");
        LOG.debug("Attempting to changing state {} for conversation {}  message {} ", moderationAction, conversation.getId(), messageId);
        conversation.applyCommand(new MessageModeratedCommand(conversation.getId(), messageId, now(), moderationAction));

        MessageProcessingContext processingContext = null;
        if (moderationAction.getModerationResultState().allowsSending()) {
            byte[] inboundMailData = heldMailRepository.read(messageId);
            Mail mail;
            try {
                mail = Mails.readMail(inboundMailData);
            } catch (ParsingException e) {
                // Assume we can parse the mail, as it was parsed before it was inserted into persistence in first place. If
                // the mail is unparseable now, this is a weird case, that we cannot handle correctly.
                throw new RuntimeException(e);
            }
            processingContext = createMessageProcessingContext(conversation, mail, messageId);
            flow.inputForPostProcessor(processingContext);

            if (attachmentRepository != null && processingContext.getAttachments().size() > 0) {
                attachmentRepository.storeAttachments(messageId, processingContext.getAttachments());
            }
        }

        heldMailRepository.remove(messageId);

        documentSink.sink(conversation);

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        Message message = conversation.getMessageById(messageId);

        if (processingContext != null) {
            messageEventPublisher.publish(conversation, message, processingContext.getTransport(), processingContext.getOriginTenant());
        } else {
            messageEventPublisher.publish(conversation, message, MessageTransport.MAIL, shortTenantName);
        }

        for (MessageProcessedListener l : listeners) {
            l.messageProcessed(conversation, message);
        }
    }

    private MessageProcessingContext createMessageProcessingContext(MutableConversation conversation, Mail mail, String messageId) {
        MessageProcessingContext context = processingContextFactory.newContext(mail, messageId, new ProcessingTimeGuard(MAX_MESSAGE_PROCESSING_TIME_SECONDS));

        context.setConversation(conversation);
        context.setMessageDirection(conversation.getMessageById(messageId).getMessageDirection());

        // only email messages can be moderated
        context.setTransport(MessageTransport.MAIL);
        context.setOriginTenant(shortTenantName);
        return context;
    }
}