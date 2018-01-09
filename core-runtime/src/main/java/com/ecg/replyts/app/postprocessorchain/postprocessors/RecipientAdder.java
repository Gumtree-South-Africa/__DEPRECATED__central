package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Set 'To' address.
 * Allows overriding a recipient's address if the address differs from the stored address and {@code email.replyto.override} is set {@code true}.
 * - Recipient's address can be changed using 'Reply-To' mail header to be able to change email address and not to use only the address stored
 * the conversation
 */
@Component
public class RecipientAdder implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientAdder.class);

    private final boolean replyToOverride;

    @Autowired
    public RecipientAdder(@Value("${email.replyto.override:false}") boolean replyToOverride) {
        this.replyToOverride = replyToOverride;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        Conversation conversation = context.getConversation();
        MutableMail mail = context.getOutgoingMail();
        String storedRecipient = conversation.getUserIdFor(context.getMessageDirection().getToRole());
        String replyToRecipient = context.getMail().getReplyTo();

        // if tenant support overriding of stored email addresses and `Reply-To` header is not blank
        if (replyToOverride && StringUtils.isNotBlank(replyToRecipient) && !replyToRecipient.equals(storedRecipient)) {
            mail.setTo(new MailAddress(replyToRecipient));
            LOG.info("Recipient of Outgoing mail was overridden: {}", replyToRecipient);
        } else {
            mail.setTo(new MailAddress(storedRecipient));
            LOG.debug("Recipient of Outgoing mail: {}", storedRecipient);
        }
    }

    @Override
    public int getOrder() {
        return 210;
    }
}
