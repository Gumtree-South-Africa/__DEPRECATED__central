package com.ecg.de.mobile.replyts.deanonymize;

import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;

/**
 * User: beckart
 */
class MailDoNotAnonymizeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MailDoNotAnonymizeHandler.class);

    public static final String X_DO_NOT_ANONYMIZE = "X-DO-NOT-ANONYMIZE";

    private final MessageProcessingContext messageProcessingContext;

    private final String password;

    MailDoNotAnonymizeHandler(MessageProcessingContext messageProcessingContext, final String password) {
        this.messageProcessingContext = messageProcessingContext;
        this.password = password;
    }

    private boolean doNotAnonymize() {

        Optional<String> doNotAnonymizeHeader = Optional.fromNullable(messageProcessingContext.getMail().getUniqueHeader(X_DO_NOT_ANONYMIZE));

        return doNotAnonymizeHeader.isPresent() && doNotAnonymizeHeader.get().trim().toLowerCase().equals(password.toLowerCase());

    }

    private String formatMailAddress(String name, String address) throws UnsupportedEncodingException {
        return new InternetAddress(address, name).toString();
    }

    public void handle() {
        try {

            if(doNotAnonymize()) {

                MailAddress fromMailAddress = preserveOriginalFrom();


                preserveReplyTo();


                LOG.trace("De-anonymizing mail sender {}", fromMailAddress.getAddress());

            }

        } catch (Exception e) {
            LOG.error("Error while de-anonymizing!", e);
        }
    }

    private MailAddress preserveOriginalFrom() throws UnsupportedEncodingException {
        MailAddress fromMailAddress = messageProcessingContext.getOriginalFrom();

        Optional<String> aliasName = Optional.fromNullable(messageProcessingContext.getMail().getFromName());

        MailAddress mailAddress;

        if(aliasName.isPresent() && StringUtils.hasText(aliasName.get())) {
           mailAddress = new MailAddress(formatMailAddress(aliasName.get(), fromMailAddress.getAddress()));
        } else {
            mailAddress = fromMailAddress;
        }

        messageProcessingContext.getOutgoingMail().setFrom(mailAddress);
        return fromMailAddress;
    }

    private void preserveReplyTo() {
        String replyTo = messageProcessingContext.getMail().getUniqueHeader("Reply-To");

        if(StringUtils.hasText(replyTo)) {
            messageProcessingContext.getOutgoingMail().addHeader("Reply-To", replyTo);
        }
    }


}
