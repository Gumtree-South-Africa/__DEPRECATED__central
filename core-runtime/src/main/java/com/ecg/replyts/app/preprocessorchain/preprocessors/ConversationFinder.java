package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;

@Component("conversationFinder")
public class ConversationFinder implements PreProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationFinder.class);

    private final NewConversationCreator newConversationCreator;
    private final ExistingConversationLoader existingConversationLoader;
    private final ConversationResumer conversationResumer;
    private final String[] platformDomains;

    @Autowired
    public ConversationFinder(
            NewConversationCreator newConversationCreator,
            ExistingConversationLoader existingConversationLoader,
            @Value("${mailcloaking.domains}") String[] platformDomains,
            ConversationResumer conversationResumer) {
        this.newConversationCreator = newConversationCreator;
        this.existingConversationLoader = existingConversationLoader;
        this.conversationResumer = conversationResumer;
        this.platformDomains = platformDomains.clone();
    }

    @Override
    public void preProcess(MessageProcessingContext context) {
        LOG.debug("Finding conversation on message {}", context.getMessageId());

        if (Strings.isNullOrEmpty(context.getMail().getDeliveredTo())) {
            throw new IllegalArgumentException("Could not read 'DeliveredTo' recipient from Mail.");
        }
        MailAddress from = context.getOriginalFrom();
        MailAddress to = context.getOriginalTo();

        boolean senderIsFromPlatformDomain = from.isFromDomain(platformDomains);
        if (senderIsFromPlatformDomain) {
            LOG.warn("Sender {} is from Platform's domain. Discarding Message.", from.getAddress());
            context.terminateProcessing(MessageState.IGNORED, this, "Sender is Cloaked " + from.getAddress());
            LOG.debug("Could not assign conversation for message {}, ended in {}", context.getMessageId(), context.getTermination().getEndState());
            return;
        }
        boolean isReply = to.isFromDomain(platformDomains);
        if (isReply) {
            LOG.debug("Load existing Conversation for {}", to.getAddress());
            existingConversationLoader.loadExistingConversation(context);
            if (context.isTerminated()) {
                LOG.debug("Message is orphaned.");
                return;
            }
        } else {
            boolean wasResumed = conversationResumer.resumeExistingConversation(context);
            if (!wasResumed) {
                LOG.debug("Create new Conversation");
                newConversationCreator.setupNewConversation(context);
            }
        }

        AddMessageCommand addMessageCommand =
                anAddMessageCommand(context.getConversation().getId(), context.getMessageId()).
                        withMessageDirection(context.getMessageDirection()).
                        withSenderMessageIdHeader(context.getMail().getUniqueHeader(Mail.MESSAGE_ID_HEADER)).
                        withInResponseToMessageId(context.getInResponseToMessageId()).
                        withHeaders(context.getMail().getUniqueHeaders()).
                        withTextParts(context.getMail().getPlaintextParts()).
                        withAttachmentFilenames(context.getMail().getAttachmentNames()).
                        build();

        context.addCommand(addMessageCommand);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
