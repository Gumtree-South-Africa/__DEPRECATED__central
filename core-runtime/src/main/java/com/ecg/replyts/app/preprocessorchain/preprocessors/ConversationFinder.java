package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ListIterator;
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;

@Component("conversationFinder")
public class ConversationFinder implements PreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationFinder.class);
    private static final Logger TERMINATED_MESSAGE_LOG = LoggerFactory.getLogger("TerminatedMessageLogger");
    private static final Counter TERMINATED_MESSAGE_COUNTER = TimingReports.newCounter("message-terminated-and-discarded");

    private final NewConversationCreator newConversationCreator;
    private final ExistingEmailConversationLoader existingEmailConversationLoader;
    private final ConversationRepository conversationRepository;
    private final ConversationResumer conversationResumer;
    private final String[] platformDomains;

    @Autowired
    public ConversationFinder(
            NewConversationCreator newConversationCreator,
            ExistingEmailConversationLoader existingEmailConversationLoader,
            @Value("${mailcloaking.domains}") String[] platformDomains,
            ConversationRepository conversationRepository,
            ConversationResumer conversationResumer) {
        this.newConversationCreator = newConversationCreator;
        this.existingEmailConversationLoader = existingEmailConversationLoader;
        this.conversationRepository = conversationRepository;
        this.conversationResumer = conversationResumer;
        this.platformDomains = platformDomains.clone();
    }

    @Override
    public void preProcess(MessageProcessingContext context) {
        if (!context.getMail().isPresent()) {
            return;
        }

        LOG.trace("Finding conversation on message {}", context.getMessageId());

        Mail mail = context.getMail().get();
        if (Strings.isNullOrEmpty(mail.getDeliveredTo())) {
            throw new IllegalArgumentException("Could not read 'DeliveredTo' recipient from Mail.");
        }
        MailAddress from = new MailAddress(mail.getFrom());
        MailAddress to = new MailAddress(mail.getDeliveredTo());

        boolean senderIsFromPlatformDomain = from.isFromDomain(platformDomains);
        if (senderIsFromPlatformDomain) {
            // Crap branch, self replies or fake mail addresses on the platform domain
            LOG.warn("Sender {} is from Platform's domain. Discarding Message.", from.getAddress());
            context.terminateProcessing(MessageState.IGNORED, this, "Sender is Cloaked " + from.getAddress());
            LOG.debug("Could not assign conversation for message {}, ended in {}", context.getMessageId(), context.getTermination().getEndState());
            return;
        }
        boolean isReply = to.isFromDomain(platformDomains);
        if (isReply) {
            // Buyer or seller reply to existing conversation
            LOG.trace("Load existing Conversation for {}", to.getAddress());
            existingEmailConversationLoader.loadExistingConversation(context);
            if (context.isTerminated()) {
                final String messageId = context.hasConversation() ? context.getMessage().getId() : "unknown";
                TERMINATED_MESSAGE_LOG.warn("Message {} belongs to terminated context. Termination state: {}, reason: {}",
                        messageId, context.getTermination().getEndState(), context.getTermination().getReason());
                TERMINATED_MESSAGE_COUNTER.inc();
                return;
            }
        } else {
            // Initial message, might be done several times so it needs to resume if the conversation was already created
            boolean wasResumed = conversationResumer.resumeExistingConversation(conversationRepository, context);
            if (!wasResumed) {
                LOG.trace("Create new Conversation");
                // First message creates conversation
                newConversationCreator.setupNewConversation(context);
            }
        }

        Conversation conversation = context.getConversation();
        AddMessageCommand addMessageCommand =
                anAddMessageCommand(conversation.getId(), context.getMessageId()).
                        withMessageDirection(context.getMessageDirection()).
                        withSenderMessageIdHeader(mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER)).
                        withInResponseToMessageId(getInResponseToMessageId(conversation, mail)).
                        withHeaders(mail.getUniqueHeaders()).
                        withTextParts(mail.getPlaintextParts()).
                        withAttachmentFilenames(mail.getAttachmentNames()).
                        build();

        context.addCommand(addMessageCommand);
    }

    private String getInResponseToMessageId(Conversation conversation, Mail mail) {
        Optional<String> lastReferencesMessageId = mail.getLastReferencedMessageId();
        if (!lastReferencesMessageId.isPresent()) {
            return null;
        }
        String lastRef = lastReferencesMessageId.get();
        // Iterate in reverse for that odd case that messages have duplicate Message-ID's.
        ListIterator<Message> listIterator = conversation.getMessages().listIterator(conversation.getMessages().size());
        while (listIterator.hasPrevious()) {
            String messageId = listIterator.previous().getId();
            if (messageId.equals(lastRef)) {
                return messageId;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}