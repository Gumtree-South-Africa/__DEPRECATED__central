package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.mail.Mail.IN_REPLY_TO_HEADER;
import static com.ecg.replyts.core.api.model.mail.Mail.MESSAGE_ID_HEADER;
import static com.ecg.replyts.core.api.model.mail.Mail.REFERENCES_HEADER;
import static java.lang.String.format;

@Component("messageIdPreparator")
public class MessageIdPreparator implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessageIdPreparator.class);

    private final MessageIdGenerator messageIdGenerator;

    @Autowired
    public MessageIdPreparator(MessageIdGenerator messageIdGenerator) {
        this.messageIdGenerator = messageIdGenerator;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();
        LOG.debug("Setting Message-ID/References/In-Reply-To headers for message {}", context.getMessageId());
        try {
            outgoingMail.removeHeader(MESSAGE_ID_HEADER);
            outgoingMail.removeHeader(REFERENCES_HEADER);
            outgoingMail.removeHeader(IN_REPLY_TO_HEADER);

            String messageId = messageIdGenerator.encryptedMessageId(context.getMessageId());
            outgoingMail.addHeader(MESSAGE_ID_HEADER, messageId);

            String inResponseToMessageId = context.getMessage().getInResponseToMessageId();
            if (inResponseToMessageId != null) {
                Message referenceMessage = context.getConversation().getMessageById(inResponseToMessageId);
                if (referenceMessage != null && referenceMessage.getSenderMessageIdHeader() != null) {
                    /*
                    Note: we only add the latest reference and not all references.
                    This is not 100% compliant. However, according to RFC 1036 it is
                    ".. permissible to not include the entire previous "References"
                    line if it is too long."
                    So for simplicity lets stick for now with only the latest reference.
                     */
                    outgoingMail.addHeader(REFERENCES_HEADER, referenceMessage.getSenderMessageIdHeader());
                    outgoingMail.addHeader(IN_REPLY_TO_HEADER, referenceMessage.getSenderMessageIdHeader());
                }
            }
        } catch (Exception ex) {
            String message = format("Could not replace Message-ID/References/In-Reply-To headers in message %s", context.getMessageId());
            throw new RuntimeException(message, ex);
        }
    }


    @Override
    public int getOrder() {
        return 100;
    }
}
