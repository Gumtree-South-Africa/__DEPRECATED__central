package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.IndexData;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In charge of persisting all changes the MessageProcessingContext did to a
 * conversation/message. Normally, the persistFinalState is invoked at the end
 * of message processing. If the processing context could not be set up
 * correctly (because the mail cannot be attached to a conversation) the
 * persistUnassignableMail method is invoked.
 */
@Component
public class ProcessingFinalizer {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingFinalizer.class);

    private static final Counter TOO_MANY_MESSAGES_IN_CONVERSATION = TimingReports.newCounter("conversation-too-many-messages");
    private static final Histogram CONVERSATION_MESSAGE_COUNT = TimingReports.newHistogram("conversation-message-count");

    protected static final int MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION = 500;
    private static final String CASSANDRA_PERSISTENCE_STRATEGY = "cassandra";
    private static String KAFKA_KEY_FIELD_SEPARATOR = "/";

    @Autowired
    private MutableConversationRepository conversationRepository;

    @Autowired(required = false)
    private MailRepository mailRepository;

    @Autowired(required = false)
    private AttachmentRepository attachmentRepository;

    @Autowired
    private SearchIndexer searchIndexer;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired(required = false)
    private MailPublisher mailPublisher;

    @Value("${persistence.strategy:unknown}")
    private String persistenceStrategy;

    @Value("${replyts.tenant}")
    private String tenant;

    @Autowired(required = false)
    @Qualifier("esSink")
    public KafkaSinkService documentSink;

    public void persistAndIndex(DefaultMutableConversation conversation, String messageId, byte[] incomingMailContent, Optional<byte[]> outgoingMailContent, Termination termination) {
        checkNotNull(termination);
        checkNotNull(conversation);

        if (!CASSANDRA_PERSISTENCE_STRATEGY.equals(persistenceStrategy) && (conversation.getMessages().size() > MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION)) {
            LOG.warn("Too many messages in conversation {}. Don't store update on conversation!", conversation);

            TOO_MANY_MESSAGES_IN_CONVERSATION.inc();

            return;
        }

        conversation.applyCommand(new MessageTerminatedCommand(conversation.getId(), messageId,
                termination.getIssuer(), termination.getReason(), termination.getEndState()));
        conversation.commit(conversationRepository, conversationEventListeners);

        processEmail(messageId, incomingMailContent, outgoingMailContent);

        CONVERSATION_MESSAGE_COUNT.update(conversation.getMessages().size());

        pushToKafka(conversation, messageId);

        try {
            searchIndexer.updateSearchAsync(ImmutableList.of(conversation));
        } catch (RuntimeException e) {
            LOG.error("Search update failed for conversation", e);
        }
    }

    private void pushToKafka(DefaultMutableConversation conversation, String messageId) {
        if (documentSink == null) {
            return;
        }

        try {
            Message message = conversation.getMessageById(messageId);
            IndexData indexData = searchIndexer.getIndexDataBuilder().toIndexData(conversation, message);
            byte[] document = indexData.getDocument().bytes().toBytes();
            String key = tenant + KAFKA_KEY_FIELD_SEPARATOR
                    + conversation.getId() + KAFKA_KEY_FIELD_SEPARATOR
                    + message.getId();
            documentSink.storeAsync(key, document);

        } catch (IOException e) {
            LOG.error("Failed to store document data in Kafka due to {}", e.getMessage(), e);
        }
    }

    private void processEmail(String messageId, byte[] incomingMailContent, Optional<byte[]> outgoingMailContent) {
        if (mailRepository != null) {
            mailRepository.persistMail(messageId, incomingMailContent, outgoingMailContent);
        }

        if (attachmentRepository != null) {
            Mail parsedMail = attachmentRepository.hasAttachments(messageId, incomingMailContent);
            if (parsedMail != null) {

                // This stores incoming mail attachments only
                attachmentRepository.storeAttachments(messageId, parsedMail);
            }
        }

        if (mailPublisher != null) {
            mailPublisher.publishMail(messageId, incomingMailContent, outgoingMailContent);
        }
    }

    @PostConstruct
    private void checkTenantIsSet() {
        if (StringUtils.isBlank(tenant) && documentSink != null) {
            throw new IllegalStateException("Cannot find replyts.tenant property. Documents for indexing cannot be saved to Kafka!");
        }
    }

}
