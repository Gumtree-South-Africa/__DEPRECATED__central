package com.gumtree.replyts2.anonymizer;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by reweber on 08/10/15
 */
public class HideEmailHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBodyAnonymizer.class);

    private MessageBodyAnonymizerConfig messageBodyAnonymizerConfig;
    private final MailCloakingService mailCloakingService;

    public HideEmailHandler(MessageBodyAnonymizerConfig messageBodyAnonymizerConfig, MailCloakingService mailCloakingService) {
        this.messageBodyAnonymizerConfig = messageBodyAnonymizerConfig;
        this.mailCloakingService = mailCloakingService;
    }

    public void process(MessageProcessingContext context) {
        if (shouldNotAnonymize(context.getMessage().getHeaders())) {
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

    private boolean shouldNotAnonymize(Map<String, String> headers) {
        String revealEmailHeader = messageBodyAnonymizerConfig.getRevealEmailHeader();
        String revealEmailValue = messageBodyAnonymizerConfig.getRevealEmailValue();
        if (revealEmailValue != null && headers.containsKey(revealEmailHeader) && headers.get(revealEmailHeader) != null) {
            return revealEmailValue.equals(headers.get(revealEmailHeader));
        }
        return false;
    }
}
