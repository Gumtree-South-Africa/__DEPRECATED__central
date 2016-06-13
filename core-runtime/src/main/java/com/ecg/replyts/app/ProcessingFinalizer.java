package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In charge of persisting all changes the MessageProcessingContext did to a
 * conversation/message. Normally, the persistFinalState is invoked at the end
 * of message processing. If the processing context could not be set up
 * correctly (because the mail cannot be attached to a conversation) the
 * persistUnassignableMail method is invoked.
 */
@Component
class ProcessingFinalizer {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingFinalizer.class);
    private static final Counter TOO_MANY_MESSAGES_IN_CONVERSATION = TimingReports.newCounter("conversation-too-many-messages");
    private static final Histogram CONVERSATION_MESSAGE_COUNT = TimingReports.newHistogram("conversation-message-count");
    private static final int MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION = 500;

    private final MutableConversationRepository conversationRepository;
    private final MailRepository mailRepository;
    private final SearchIndexer searchIndexer;
    private final ExcessiveConversationSizeConstraint conversationSizeConstraint;
    private final ConversationEventListeners conversationEventListeners;
    private final boolean skipMailStorage;
    private final MailPublisher mailPublisher;

    @Autowired
    ProcessingFinalizer(MutableConversationRepository conversationRepository, MailRepository mailRepository,
                        SearchIndexer searchIndexer, ConversationEventListeners conversationEventListeners,
                        MailPublisher mailPublisher, boolean skipMailStorage) {
        this(conversationRepository, mailRepository, searchIndexer,
                new ExcessiveConversationSizeConstraint(MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION),
                conversationEventListeners, mailPublisher, skipMailStorage);
    }

    ProcessingFinalizer(MutableConversationRepository conversationRepository, MailRepository mailRepository,
                        SearchIndexer searchIndexer, ExcessiveConversationSizeConstraint constraint,
                        ConversationEventListeners conversationEventListeners, MailPublisher mailPublisher,
                        boolean skipMailStorage) {
        this.conversationRepository = conversationRepository;
        this.mailRepository = mailRepository;
        this.searchIndexer = searchIndexer;
        conversationSizeConstraint = constraint;
        this.conversationEventListeners = conversationEventListeners;
        this.skipMailStorage = skipMailStorage;
        this.mailPublisher = mailPublisher;
    }


    void persistAndIndex(DefaultMutableConversation conversation, String messageId, byte[] incomingMailContent,
                         Optional<byte[]> outgoingMailContent, Termination termination) {
        checkNotNull(termination);
        checkNotNull(conversation);

        if (conversationSizeConstraint.tooManyMessagesIn(conversation)) {
            LOG.warn("Too many messages in conversation {}. Don't persist update on conversation!", conversation);
            TOO_MANY_MESSAGES_IN_CONVERSATION.inc();
            return;
        }

        conversation.applyCommand(new MessageTerminatedCommand(
                conversation.getId(),
                messageId,
                termination.getIssuer(),
                termination.getReason(),
                termination.getEndState()));

        conversation.commit(conversationRepository, conversationEventListeners);
        processEmail(messageId, incomingMailContent, outgoingMailContent);

        reportConversationSizeInMetrics(conversation);

        try {
            searchIndexer.updateSearchAsync(ImmutableList.<Conversation>of(conversation));
        } catch (RuntimeException e) {
            LOG.error("Search update failed for conversation", e);
        }

    }

    private void processEmail(String messageId, byte[] incomingMailContent, Optional<byte[]> outgoingMailContent) {
        if (skipMailStorage) {
            LOG.trace("Skipping persistence step, you can configure it with persistence.skip property");
        } else {
            mailRepository.persistMail(messageId, incomingMailContent, outgoingMailContent);
        }
        if (mailPublisher != null) {
            mailPublisher.publishMail(messageId, incomingMailContent, outgoingMailContent);
        }
    }

    private void reportConversationSizeInMetrics(DefaultMutableConversation conversation) {
        CONVERSATION_MESSAGE_COUNT.update(conversation.getMessages().size());
    }

}
