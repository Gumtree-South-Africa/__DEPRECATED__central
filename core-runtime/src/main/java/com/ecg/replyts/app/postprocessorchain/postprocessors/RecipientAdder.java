package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.EmailPostProcessor;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Set 'To' address.
 */
@Component
public class RecipientAdder implements EmailPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientAdder.class);

    @Override
    public void postProcess(MessageProcessingContext context) {
        MailAddress recipient = context.getRecipient();
        context.getOutgoingMail().setTo(recipient);
        LOG.debug("Recipient of Outgoing mail: {}", recipient);
    }

    @Override
    public int getOrder() {
        return 210;
    }
}
