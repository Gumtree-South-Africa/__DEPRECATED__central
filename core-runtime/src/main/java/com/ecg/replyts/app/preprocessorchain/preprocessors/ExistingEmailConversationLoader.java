package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Loads an existing conversation for the case where the receiver is an email
 */
@Component
public class ExistingEmailConversationLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ExistingEmailConversationLoader.class);
    private static final Counter ORPHANED_COUNTER = TimingReports.newCounter("message-orphaned-counter");

    private final MailCloakingService mailCloakingService;

    @Autowired
    ExistingEmailConversationLoader(MultiTennantMailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    void loadExistingConversation(MessageProcessingContext context) {
        Mail mail = context.getMail().get();

        Optional<CloakedReceiverContext> receiver = mailCloakingService.resolveUser(new MailAddress(mail.getDeliveredTo()));
        if (!receiver.isPresent()) {
            context.terminateProcessing(MessageState.ORPHANED, this, "Conversation for " + mail.getDeliveredTo() + " does not exist");
            ORPHANED_COUNTER.inc();
            return;
        }
        ConversationRole receiverRole = receiver.get().getRole();
        MutableConversation conversation = receiver.get().getConversation();
        context.setConversation(conversation);
        context.setMessageDirection(MessageDirection.getWithToRole(receiverRole));

        MDCConstants.setContextFields(context);

        LOG.trace("Receiver {} belongs to Conversation {}. Receiver is {}", mail.getDeliveredTo(), receiver.get().getConversation().getId(), receiverRole);
    }
}