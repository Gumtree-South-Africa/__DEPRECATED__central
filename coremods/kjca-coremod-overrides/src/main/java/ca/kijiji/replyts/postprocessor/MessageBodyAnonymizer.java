package ca.kijiji.replyts.postprocessor;

import ca.kijiji.replyts.AddresserUtil;
import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private final MailCloakingService mailCloakingService;

    @Autowired
    public MessageBodyAnonymizer(MailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
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
            String content = part.getContent();
            MailAddress mailAddress;
            switch (context.getMessageDirection()) {
                case BUYER_TO_SELLER:
                    mailAddress = mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation);
                    content = StringUtils.replace(content, conversation.getBuyerId(), mailAddress.getAddress());
                    LOG.debug("Replaced buyer's real email with anonymous");
                    break;
                case SELLER_TO_BUYER:
                    mailAddress = mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation);
                    content = StringUtils.replace(content, conversation.getSellerId(), mailAddress.getAddress());
                    LOG.debug("Replaced seller's real email with anonymous");
                    break;
                default:
                    return;
            }
            part.overrideContent(content);
        }
    }

    @Override
    public int getOrder() {
        return 300;
    }
}
