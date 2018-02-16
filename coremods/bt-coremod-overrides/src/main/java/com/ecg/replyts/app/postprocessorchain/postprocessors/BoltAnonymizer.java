package com.ecg.replyts.app.postprocessorchain.postprocessors;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.MimeUtility;

import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

import static java.lang.String.format;

public class BoltAnonymizer extends Anonymizer {
    private static final Logger LOG = LoggerFactory.getLogger(Anonymizer.class);

    @Value("${mail.from.default.display}")
    private String defaultFromDisplay;

    public BoltAnonymizer(MultiTennantMailCloakingService mailCloakingService) {
        super(mailCloakingService);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        String fromTemplate = "%s <%s>";

        Conversation c = context.getConversation();

        MailAddress newTo = new MailAddress(c.getUserIdFor(context.getMessageDirection().getToRole()));
        MailAddress newFrom = mailCloakingService.createdCloakedMailAddress(context.getMessageDirection().getFromRole(), context.getConversation());
        
        // BOLT-20980 (From the incoming messages from BOLT)
        String buyerName = context.getMail().getCustomHeaders().get("buyer-name") != null
          ? context.getMail().getCustomHeaders().get("buyer-name") : null;

        // BOLT-20980 (From the reply message)
        if (buyerName == null) {
            buyerName = context.getConversation().getCustomValues().get("buyer-name") != null
              ? context.getConversation().getCustomValues().get("buyer-name") : null;
        }

        // BOLT-20980 (From the incoming messages from BOLT)
        String sellerName = context.getMail().getCustomHeaders().get("seller-name") != null
          ? context.getMail().getCustomHeaders().get("seller-name") : null;

        // BOLT-20980 (From the reply message)
        if (sellerName == null) {
            sellerName = context.getConversation().getCustomValues().get("seller-name") != null
              ? context.getConversation().getCustomValues().get("seller-name") : null;
        }

        String fromUserName = context.getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? buyerName : sellerName;

        LOG.debug("From user name is {}", fromUserName);

        if (StringUtils.hasText(fromUserName)) {
            String[] components = defaultFromDisplay.split("\\s");

            String displayName = format("%s %s %s", fromUserName, components[1], components[2]);

            // BOLT-20953 (https://issues.apache.org/jira/browse/MIME4J-239)
            try {
                LOG.debug("Display name before encoding is {}", displayName);

                displayName = MimeUtility.encodeText(displayName,"UTF-8","B");

                // This is for user name that contains no special characters, but has @
                displayName = displayName.contains("@") ? format("\"%s\"", displayName) : displayName;

                LOG.debug("Display name after encoding is {}", displayName);
            } catch (UnsupportedEncodingException e) {
                // Do nothing
            }

            newFrom = new MailAddress(String.format(fromTemplate, displayName, newFrom.getAddress()));
        } else {
            LOG.debug("Using the default From Display {}", defaultFromDisplay);

            newFrom = new MailAddress(String.format(fromTemplate, defaultFromDisplay, newFrom.getAddress()));
        }
        
        LOG.debug("Modified cloaked Mail Address is {}", newFrom.getAddress());
        
        MutableMail outgoingMail = context.getOutgoingMail();

        outgoingMail.setTo(newTo);
        outgoingMail.setFrom(newFrom);

        LOG.debug("Anonymizing Outgoing mail. Set From: {}, To: {}", newFrom, newTo);
    }
}
