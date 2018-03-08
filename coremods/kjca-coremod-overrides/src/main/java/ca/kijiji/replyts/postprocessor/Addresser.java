package ca.kijiji.replyts.postprocessor;

import ca.kijiji.replyts.AddresserUtil;
import ca.kijiji.replyts.BoxHeaders;
import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
/**
 * Sets the "To" and "From" headers.
 *
 * Anonymization is enabled by default unless the X-Cust-Anonymize header is set to false.
 *
 * Also see {@link com.ecg.replyts.app.postprocessorchain.postprocessors.Anonymizer} for
 * the original implementation. Our version preserves the "name" part of the email.
 */
public class Addresser implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(Addresser.class);

    static final String HEADER_LOCALE = BoxHeaders.LOCALE.getHeaderName();
    static final String HEADER_FROM_NAME = BoxHeaders.REPLIER_NAME.getHeaderName();
    static final String HEADER_ANONYMIZE = BoxHeaders.ANONYMIZE.getHeaderName();
    static final String FROM_FR_PREFIX = "Réponse Kijiji (de ";
    static final String FROM_EN_PREFIX = "Kijiji Reply (from ";
    static final String FROM_SUFFIX = ")";
    static final String FROM_EMAIL_LOCAL = "post";
    static final String FROM_EMAIL_HOST = "kijiji.ca";

    private final MailCloakingService mailCloakingService;

    @Autowired
    public Addresser(MailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        Conversation convo = context.getConversation();
        Mail incomingMail = context.getMail().get();
        MutableMail outgoingMail = context.getOutgoingMail();
        String locale = incomingMail.getUniqueHeader(HEADER_LOCALE);
        MailAddress newTo = new MailAddress(convo.getUserIdFor(context.getMessageDirection().getToRole()));
        String fromNameFromR2S = incomingMail.getUniqueHeader(HEADER_FROM_NAME);
        String fromNameFromEmail = incomingMail.getFromName();
        String fromName = StringUtils.hasText(fromNameFromR2S) ? fromNameFromR2S : fromNameFromEmail;

        MailAddress fromAddress;
        Mailbox mailbox;
        boolean shouldAnonymize = AddresserUtil.shouldAnonymizeConversation(convo);
        if (shouldAnonymize) {
            fromAddress = mailCloakingService.createdCloakedMailAddress(context.getMessageDirection().getFromRole(), context.getConversation());
            mailbox = createAnonymizedMailbox(fromName, fromAddress);
            // No need for Reply-To when anonymizing. It's the same address as From.
        } else {
            fromAddress = new MailAddress(convo.getUserIdFor(context.getMessageDirection().getFromRole()));
            mailbox = createPlainMailbox(locale, fromName, fromAddress);
            outgoingMail.addHeader(Mail.REPLY_TO, fromAddress.getAddress()); // doesn't include name for backwards-compatibility
        }

        String encodedFromAddress = AddressFormatter.DEFAULT.encode(mailbox);

        outgoingMail.addHeader("From", encodedFromAddress);
        outgoingMail.setTo(newTo);

        if (LOG.isDebugEnabled()) {
            LOG.debug("{}Anonymizing outgoing mail. Set Reply-To: {}, To: {}, From: {}",
                    !shouldAnonymize ? "NOT " : "",
                    fromAddress,
                    newTo,
                    encodedFromAddress
            );
        }
    }

    private Mailbox createPlainMailbox(String locale, String fromName, MailAddress fromAddress) {
        Mailbox mailbox;
        String senderName = StringUtils.hasText(fromName) ? fromName : fromAddress.getAddress();

        // Not using a resource bundle here, since it's the *only* place (right now)
        // where we're using different strings based on the language.
        if ("fr_CA".equals(locale)) {
            mailbox = new Mailbox(FROM_FR_PREFIX + senderName + FROM_SUFFIX, FROM_EMAIL_LOCAL, FROM_EMAIL_HOST);
        } else {
            mailbox = new Mailbox(FROM_EN_PREFIX + senderName + FROM_SUFFIX, FROM_EMAIL_LOCAL, FROM_EMAIL_HOST);
        }
        return mailbox;
    }

    private Mailbox createAnonymizedMailbox(String fromName, MailAddress fromAddress) {
        String address = fromAddress.getAddress();
        int lastAtChar = address.lastIndexOf('@');
        String[] atSplit = address.split("@");
        if (lastAtChar == -1 || atSplit.length > 2) {
            throw new RuntimeException("Could not parse address: " + fromName + " " + address);
        }

        return new Mailbox(fromName, address.substring(0, lastAtChar), address.substring(lastAtChar + 1));
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
