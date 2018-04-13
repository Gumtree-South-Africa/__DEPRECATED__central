package com.ecg.comaas.kjca.coremod.overrides.postprocessor;

import com.ecg.comaas.kjca.coremod.shared.AddresserUtil;
import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * Examines the body of the message and replaces all instances of seller and
 * buyer email addresses with their anonymized equivalents. Buyer's email is
 * untouched when the seller's replying and vice versa.
 *
 * Does not do anything if anonymization is disabled. See {@link Addresser}.
 */
public class MessageBodyAnonymizer implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBodyAnonymizer.class);

    private final TextAnonymizer textAnonymizer;

    @Autowired
    public MessageBodyAnonymizer(TextAnonymizer textAnonymizer) {
        this.textAnonymizer = textAnonymizer;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        if (!AddresserUtil.shouldAnonymizeConversation(context.getConversation())) {
            LOG.debug("Anonymization is off. Skipping.");
            return;
        }

        Conversation conversation = context.getConversation();
        MutableMail outgoingMail = context.getOutgoingMail();
        List<TypedContent<String>> textParts = outgoingMail.getTextParts(false); // grabs both text/plain and text/html
        for (TypedContent<String> part : textParts) {
            part.overrideContent(textAnonymizer.anonymizeText(conversation, part.getContent()));
        }
    }

    @Override
    public int getOrder() {
        return 300;
    }
}
