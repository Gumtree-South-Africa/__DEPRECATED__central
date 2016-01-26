package com.ecg.replyts.app.preprocessorchain.preprocessors;


import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.util.CommonsLang.not;


@Component
class ExistingConversationLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ExistingConversationLoader.class);

    private final MailCloakingService mailCloakingService;

    @Autowired
    ExistingConversationLoader(MultiTennantMailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    public void loadExistingConversation(MessageProcessingContext context) {
        Optional<CloakedReceiverContext> receiver = mailCloakingService.resolveUser(context.getOriginalTo());
        if (not(receiver.isPresent())) {
            context.terminateProcessing(MessageState.ORPHANED, this, "Conversation for " + context.getOriginalTo() + " does not exist");
            return;
        }
        ConversationRole receiverRole = receiver.get().getRole();
        MutableConversation conversation = receiver.get().getConversation();
        context.setConversation(conversation);
        context.setMessageDirection(MessageDirection.getWithToRole(receiverRole));

        LOG.debug("Receiver {} belongs to Conversation {}. Receiver is {}", context.getOriginalTo(), receiver.get().getConversation().getId(), receiverRole);
    }
}