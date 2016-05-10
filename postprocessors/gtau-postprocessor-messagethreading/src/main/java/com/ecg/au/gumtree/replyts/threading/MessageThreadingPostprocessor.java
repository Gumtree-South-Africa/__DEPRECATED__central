package com.ecg.au.gumtree.replyts.threading;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailparser.MessageIdHeaderEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static java.lang.String.format;

/**
 * @author mdarapour
 */
public class MessageThreadingPostprocessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MessageThreadingPostprocessor.class);

    private final MessageThreadingConfig config;
    private final MessageIdHeaderEncryption messageIdHeaderEncryption;
    private final MailRepository mailRepository;
    private final String[] platformDomains;

    @Autowired
    public MessageThreadingPostprocessor(@Value("${replyts-message-threading.plugin.order:250}") int order,
                                         @Value("${mailcloaking.domains}") String[] platformDomains,
                                         MailRepository mailRepository) {
        this(new MessageThreadingConfig(order), new MessageIdHeaderEncryption(), platformDomains, mailRepository);
    }

    MessageThreadingPostprocessor(MessageThreadingConfig config, MessageIdHeaderEncryption messageIdHeaderEncryption, String[] platformDomains, MailRepository mailRepository) {
        this.config = config;
        this.messageIdHeaderEncryption = messageIdHeaderEncryption;
        this.platformDomains = platformDomains;
        this.mailRepository = mailRepository;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();
        StringBuilder references = new StringBuilder();
        String replyTo = null;

        try {
            List<Message> messages = context.getConversation().getMessages();

            // Ignore if the conversation does not contain enough messages
            // or the outgoing mail already has a 'References' header
            if(messages == null || messages.size() < 2 || outgoingMail.containsHeader(Mail.REFERENCES_HEADER))
                return;

            // Mail References header format is 'References: <Message-ID>'
            for(Message message : messages) {
                if(message.getId().equals(decryptedMessageId(outgoingMail.getMessageId())))
                    continue;
                replyTo = encryptedMessageId(message.getId());
                references.append(replyTo);
            }

            outgoingMail.addHeader(Mail.REFERENCES_HEADER, references.toString());
            outgoingMail.addHeader(Mail.IN_REPLY_TO_HEADER, replyTo);
        } catch (Exception ex) {
            LOG.warn(format("Could not replace References/In-Reply-To headers in message %s", context.getMessageId()), ex);
        }
    }

    String encryptedMessageId(String messageId) throws Exception {
        Mail mail = mailRepository.readOutboundMailParsed(messageId);
        return mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER);
    }

    String decryptedMessageId(String messageId) {
        return messageIdHeaderEncryption.decrypt(messageId.substring(1, messageId.length() - platformDomains[0].length() - 2));
    }

    @Override
    public int getOrder() {
        return config.getOrder();
    }
}
