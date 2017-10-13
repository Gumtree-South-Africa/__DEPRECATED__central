package ca.kijiji.replyts;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mask the email addresses of a given conversation's participants in arbitrary text.
 */
@Component
public class TextAnonymizer {
    private final static Logger LOG = LoggerFactory.getLogger(TextAnonymizer.class);

    private final MailCloakingService cloakingDevice;

    @Autowired
    public TextAnonymizer(MailCloakingService mailCloakingService) {
        cloakingDevice = mailCloakingService;
    }

    public String anonymizeText(Conversation conversation, String content) {
        String maskedBuyerContent = maskBuyerAddress(conversation, content);
        return maskSellerAddress(conversation, maskedBuyerContent);
    }

    private String maskBuyerAddress(Conversation conversation, String content) {
        MailAddress mailAddress = cloakingDevice.createdCloakedMailAddress(ConversationRole.Buyer, conversation);
        content = StringUtils.replace(content, conversation.getBuyerId(), mailAddress.getAddress());
        LOG.trace("Replaced buyer's real email with anonymous");
        return content;
    }

    private String maskSellerAddress(Conversation conversation, String content) {
        MailAddress mailAddress = cloakingDevice.createdCloakedMailAddress(ConversationRole.Seller, conversation);
        content = StringUtils.replace(content, conversation.getSellerId(), mailAddress.getAddress());
        LOG.trace("Replaced seller's real email with anonymous");
        return content;
    }
}
