package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Anonymizes the 'From' address.
 */
@Component
public class Anonymizer implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(Anonymizer.class);


    private final MailCloakingService mailCloakingService;

    @Autowired
    public Anonymizer(MultiTennantMailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        Conversation c = context.getConversation();
        MailAddress newTo = new MailAddress(c.getUserIdFor(context.getMessageDirection().getToRole()));
        MailAddress newFrom = mailCloakingService.createdCloakedMailAddress(context.getMessageDirection().getFromRole(), context.getConversation());

        MutableMail outgoingMail = context.getOutgoingMail();
        outgoingMail.setTo(newTo);
        outgoingMail.setFrom(newFrom);
        LOG.debug("Anonymizing Outgoing mail. Set From: {}, To: {}", newFrom, newTo);
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
