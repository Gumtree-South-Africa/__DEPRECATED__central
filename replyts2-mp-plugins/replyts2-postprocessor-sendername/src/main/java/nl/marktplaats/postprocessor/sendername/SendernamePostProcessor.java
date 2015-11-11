package nl.marktplaats.postprocessor.sendername;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;

public class SendernamePostProcessor implements PostProcessor {

    public static final String FROM = "From";
    private static final Logger LOG = LoggerFactory.getLogger(SendernamePostProcessor.class);
    private final String[] platformDomains;
    private final SendernamePostProcessorConfig sendernamePostProcessorConfig;


    @Autowired
    public SendernamePostProcessor(@Value("${mailcloaking.domains}") String[] platformDomains,
                                   SendernamePostProcessorConfig sendernamePostProcessorConfig) {

        this.platformDomains = platformDomains;

        this.sendernamePostProcessorConfig = sendernamePostProcessorConfig;
    }

    private String getHeaderName(MessageDirection messageDirection) {
        switch (messageDirection) {
            case BUYER_TO_SELLER:
                return sendernamePostProcessorConfig.getBuyerConversationHeader();
            case SELLER_TO_BUYER:
                return sendernamePostProcessorConfig.getSellerConversationHeader();
            default:
                throw new IllegalStateException("Cannot Handle Message Direction " + messageDirection);
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

            String currentName = message.getHeaders().get(getHeaderName(message.getMessageDirection()));

            if (StringUtils.isBlank(currentName)) {
                currentName = (sendernamePostProcessorConfig.isFallbackToConversationId()) ? String.valueOf(conversation.getId()) : "";
            }

            String formattedName = String.format(pattern, currentName);

            try {
                MutableMail outboundMail = messageProcessingContext.getOutgoingMail();

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
}