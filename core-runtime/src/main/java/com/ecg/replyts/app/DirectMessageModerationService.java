package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.MessageModeratedCommand;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.joda.time.DateTime.now;

/**
 * Service that handles Moderation results from CS agents (forwarded to ReplyTS via Webservice).
 * If the ModerationResultState GOOD, the message will be sent, if it is BAD, it will end up BLOCKED.
 */
public class DirectMessageModerationService implements ModerationService {

    // Max processing time, choose a quite long time, post processing shouldn't be the processing problem
    private static final long MAX_MESSAGE_PROCESSING_TIME_SECONDS = 300L;

    private final MutableConversationRepository conversationRepository;
    private final SearchIndexer searchIndexer;
    private final List<MessageProcessedListener> listeners;
    private final MailRepository mailRepository;
    private final ProcessingFlow flow;
    private final Mails mails = new Mails();
    private final ConversationEventListeners conversationEventListeners;


    DirectMessageModerationService(MutableConversationRepository conversationRepository, ProcessingFlow flow, MailRepository mailRepository, SearchIndexer searchIndexer, List<MessageProcessedListener> listeners, ConversationEventListeners conversationEventListeners) {
        this.conversationRepository = conversationRepository;
        this.flow = flow;
        this.mailRepository = mailRepository;
        this.searchIndexer = searchIndexer;
        this.listeners = listeners;
        this.conversationEventListeners = conversationEventListeners;
    }

    @Override
    public void changeMessageState(MutableConversation conversation, String messageId, ModerationAction moderationAction) {
        Preconditions.checkArgument(moderationAction.getModerationResultState().isAcceptableOutcome(), "Moderation State " + moderationAction.getModerationResultState() + " is not an acceptable moderation outcome");
        MessageModeratedCommand command = new MessageModeratedCommand(conversation.getId(), messageId, now(), moderationAction);
        conversation.applyCommand(command);

        if (moderationAction.getModerationResultState().allowsSending()) {
            byte[] inboundMailData = mailRepository.readInboundMail(messageId);
            MessageProcessingContext processingContext = putIntoFlow(conversation, inboundMailData, messageId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                processingContext.getOutgoingMail().writeTo(outputStream);
                mailRepository.persistMail(messageId, inboundMailData, Optional.of(outputStream.toByteArray()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        searchIndexer.updateSearchAsync(ImmutableList.<Conversation>of(conversation));

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        Message message = conversation.getMessageById(messageId);

        for (MessageProcessedListener l : listeners) {
            l.messageProcessed(conversation, message);
        }
    }

    private MessageProcessingContext putIntoFlow(MutableConversation conversation, byte[] mail, String messageId) {
        try {
            ProcessingTimeGuard processingTimeGuard = new ProcessingTimeGuard(MAX_MESSAGE_PROCESSING_TIME_SECONDS);
            MessageProcessingContext context = new MessageProcessingContext(mails.readMail(mail), messageId, processingTimeGuard);
            context.setConversation(conversation);
            context.setMessageDirection(conversation.getMessageById(messageId).getMessageDirection());

            flow.inputForPostProcessor(context);
            return context;
        } catch (ParsingException e) {
            // assume we can parse the mail, as it was parsed before it was inserted into persistence in first place.
            // If the mail is unparsable now, this is a weird case, that we cannot handle correctly.
            throw new RuntimeException(e);
        }
    }
}
