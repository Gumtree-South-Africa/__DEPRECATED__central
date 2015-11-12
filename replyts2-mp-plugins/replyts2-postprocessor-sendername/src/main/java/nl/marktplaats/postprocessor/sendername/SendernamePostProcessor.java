package nl.marktplaats.postprocessor.sendername;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

public class SendernamePostProcessor implements PostProcessor {

    public static final String FROM = "from";
    private static final Logger LOG = LoggerFactory.getLogger(SendernamePostProcessor.class);
    private final String[] platformDomains;
    private final SendernamePostProcessorConfig sendernamePostProcessorConfig;


    @Autowired
    public SendernamePostProcessor(@Value("${mailcloaking.domains}") String[] platformDomains,
                                   SendernamePostProcessorConfig sendernamePostProcessorConfig) {

        this.platformDomains = platformDomains;

        this.sendernamePostProcessorConfig = sendernamePostProcessorConfig;
    }

    private boolean canHandle(Message m) {
        return getPattern(m.getMessageDirection()) != null;
    }

    private String decodeRfc2047(String headerValue, String messageId) {
        if (headerValue == null) {
            return null;
        }

        try {
            // Decode the name when RFC2047 encoding is used.
            return MimeUtility.decodeText(headerValue);

        } catch (UnsupportedEncodingException uee) {
            // Use as is, no conversion.
            LOG.debug(String.format(
                    "Header '%s' for message %d has unsupported character encoding, using it raw (%s)",
                    headerValue, messageId, uee.getMessage()));
            return headerValue;
        }
    }

    private String formatName(MessageDirection md, String name) {
        String pattern = getPattern(md);
        return pattern == null ? name : String.format(pattern, name);
    }

    private String getHeaderName(MessageDirection md) {
        switch (md) {
            case BUYER_TO_SELLER:
                // This mail is from buyer, e.g. initiator of conversation
                return FROM;
            case SELLER_TO_BUYER:
                // This mail is from seller, e.g. respondent of conversation
                return "to";
            default:
                throw new IllegalStateException("Unknown message direction " + md);
        }
    }

    @Override
    public int getOrder() {
        // WARNING: order value needs to be higher then that of Anonymizer.getOrder().
        return 300;
    }

    private String getPattern(MessageDirection messageDirection) {
        switch (messageDirection) {
            case BUYER_TO_SELLER:
                return sendernamePostProcessorConfig.getBuyerNamePattern();
            case SELLER_TO_BUYER:
                return sendernamePostProcessorConfig.getSellerNamePattern();
            default:
                throw new IllegalStateException("Cannot Handle Message Direction " + messageDirection);
        }
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();
        MessageDirection messageDirection = message.getMessageDirection();
        String pattern = getPattern(messageDirection);
        if (pattern != null) {
            Conversation conversation = messageProcessingContext.getConversation();
            if (conversation == null) {
                LOG.warn("Could not find message\'s conversation. Skipping Sending Preperator");
                return;
            }

            MutableMail outboundMail = messageProcessingContext.getOutgoingMail();

            String currentName = trimToNull(outboundMail.getUniqueHeader(getHeaderName(message.getMessageDirection())));
            String decodedCurrentName = trimToNull(decodeRfc2047(currentName, message.getId()));

            String formattedName = String.format(pattern, decodedCurrentName);

            try {
                String originalFrom = outboundMail.getFrom();
                String newFrom = (new InternetAddress(originalFrom, formattedName)).toString();
                outboundMail.removeHeader(FROM);
                outboundMail.addHeader(FROM, newFrom);
            } catch (UnsupportedEncodingException e) {
                LOG.error("Could not process message with id " + message.getId(), e);
                throw new RuntimeException(e);
            } catch (StackOverflowError e) {
                LOG.error("Could not process message with id " + message.getId(), e);
                throw e;
            }

        } else {
            LOG.debug("no patten defined for direction {}", messageDirection);
        }
    }

    private String trimToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }


}