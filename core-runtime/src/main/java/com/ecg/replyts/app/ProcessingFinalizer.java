package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
    public static final String CASSANDRA_PERSISTENCE_STRATEGY = "cassandra";
    public static String KAFKA_KEY_FIELD_SEPARATOR = "/";

    @Autowired
    private MutableConversationRepository conversationRepository;

    @Autowired
    private SearchIndexer searchIndexer;

    @Autowired(required = false)
    private MailRepository mailRepository;

    @Autowired(required = false)
    private AttachmentRepository attachmentRepository;

    @Autowired(required = false)
    private Document2KafkaSink document2KafkaSink;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Value("#{'${indexing.2kafka.enabled:false}' == '${region:ams1}' }")
    private boolean enableIndexing2Kafka;

    @Autowired(required = false)
    private MailPublisher mailPublisher;

    @Value("${persistence.strategy:unknown}")
    private String persistenceStrategy;

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

        if(document2KafkaSink!=null) {
            document2KafkaSink.pushToKafka(conversation, messageId);
        }

        if (!enableIndexing2Kafka) {
            try {
                searchIndexer.updateSearchAsync(ImmutableList.of(conversation));
            } catch (RuntimeException e) {
                LOG.error("Search update failed for conversation", e);
            }
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
    private void checkConfiguration() {
        if (enableIndexing2Kafka) {
            checkState( document2KafkaSink != null, "Must configure kafka document sink ()if you want to enable reindex to kafka!");
            LOG.info("New indexing strategy is enabled");
        } else if (document2KafkaSink == null) {
            LOG.info("Legacy indexing strategy is enabled");
        } else if (document2KafkaSink != null) {
            LOG.info("Both legacy and new indexing strategies are enabled");
        }
    }

}
