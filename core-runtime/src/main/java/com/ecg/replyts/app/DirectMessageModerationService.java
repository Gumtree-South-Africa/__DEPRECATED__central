package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.MessageModeratedCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private Document2KafkaSink document2KafkaSink;

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

    @Override
    public void changeMessageState(MutableConversation conversation, String messageId, ModerationAction moderationAction) throws MessageNotFoundException {
        Preconditions.checkArgument(moderationAction.getModerationResultState().isAcceptableOutcome(), "Moderation State " + moderationAction.getModerationResultState() + " is not an acceptable moderation outcome");
        LOG.debug("Attempting to changing state {} for conversation {}  message {} ", moderationAction, conversation.getId(), messageId);
        conversation.applyCommand(new MessageModeratedCommand(conversation.getId(), messageId, now(), moderationAction));

        if (moderationAction.getModerationResultState().allowsSending()) {
            byte[] inboundMailData = heldMailRepository.read(messageId);

            MessageProcessingContext processingContext = putIntoFlow(conversation, inboundMailData, messageId);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                processingContext.getOutgoingMail().writeTo(outputStream);

                if (attachmentRepository != null) {

                    Mail parsedMail = attachmentRepository.hasAttachments(messageId, inboundMailData);
                    if (parsedMail != null) {

                        attachmentRepository.storeAttachments(messageId, parsedMail);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        heldMailRepository.remove(messageId);

        document2KafkaSink.pushToKafka(conversation);

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        Message message = conversation.getMessageById(messageId);

        for (MessageProcessedListener l : listeners) {
            l.messageProcessed(conversation, message);
        }
    }

    private MessageProcessingContext putIntoFlow(MutableConversation conversation, byte[] mail, String messageId) {
        try {
            MessageProcessingContext context = processingContextFactory.newContext(Mails.readMail(mail), messageId, new ProcessingTimeGuard(MAX_MESSAGE_PROCESSING_TIME_SECONDS));

            context.setConversation(conversation);
            context.setMessageDirection(conversation.getMessageById(messageId).getMessageDirection());

            flow.inputForPostProcessor(context);

            return context;
        } catch (ParsingException e) {
            // Assume we can parse the mail, as it was parsed before it was inserted into persistence in first place. If
            // the mail is unparseable now, this is a weird case, that we cannot handle correctly.
            throw new RuntimeException(e);
        }
    }
}