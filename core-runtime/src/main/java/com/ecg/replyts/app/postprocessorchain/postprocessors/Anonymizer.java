package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Anonymizer implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(Anonymizer.class);

    protected MailCloakingService mailCloakingService;

    @Autowired
    public Anonymizer(MultiTennantMailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MailAddress newFrom = mailCloakingService.createdCloakedMailAddress(context.getMessageDirection().getFromRole(), context.getConversation());

        context.getOutgoingMail().setFrom(newFrom);

        LOG.debug("Anonymizing Sender of Outgoing mail: {}", newFrom);
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
