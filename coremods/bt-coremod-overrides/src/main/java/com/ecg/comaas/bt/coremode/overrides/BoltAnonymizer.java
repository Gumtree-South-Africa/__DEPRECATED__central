package com.ecg.comaas.bt.coremode.overrides;

import com.ecg.replyts.app.postprocessorchain.postprocessors.Anonymizer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTenantMailCloakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class BoltAnonymizer extends Anonymizer {
    private static final Logger LOG = LoggerFactory.getLogger(Anonymizer.class);

    @Value("${mail.from.default.display}")
    private String defaultFromDisplay;

    public BoltAnonymizer(MultiTenantMailCloakingService mailCloakingService) {
        super(mailCloakingService);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        String fromTemplate = "%s <%s>";

        Conversation c = context.getConversation();

        MailAddress newTo = new MailAddress(c.getUserIdFor(context.getMessageDirection().getToRole()));
        MailAddress newFrom = getMailCloakingService().createdCloakedMailAddress(context.getMessageDirection().getFromRole(), context.getConversation());

        String buyerName = getName("buyer-name", context);
        String sellerName = getName("seller-name", context);

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

    private String getName(String key, MessageProcessingContext context) {
        // BOLT-20980 From the incoming messages from BOLT, name is taken from customHeaders, from the reply message name is taken from customValues

        Map<String, String> customHeaders = context.getMail().get().getCustomHeaders();
        Map<String, String> customValues = context.getConversation().getCustomValues();

        return Optional.ofNullable(customHeaders.get(key)).orElse(customValues.get(key));
    }
}
