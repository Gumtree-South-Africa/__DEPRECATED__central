package com.ecg.de.mobile.replyts.mailalias;

import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

/**
 * User: beckart
 */
class MailAliasHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MailAliasHandler.class);

    private final MessageProcessingContext messageProcessingContext;


    MailAliasHandler(MessageProcessingContext messageProcessingContext) {
        this.messageProcessingContext = messageProcessingContext;
    }


    public void handle() {
        try {
            Optional<String> aliasName = Optional.ofNullable(messageProcessingContext.getMail().get().getFromName());

            if (aliasName.isPresent()) {
                String senderMail = messageProcessingContext.getOutgoingMail().getFrom();
                messageProcessingContext.getOutgoingMail().setFrom(new MailAddress(formatMailAddress(aliasName.get(), senderMail)));
            }
        } catch (Exception e) {
            LOG.error("Error while setting sender alias name.", e);
        }
    }


    private String formatMailAddress(String name, String address) throws UnsupportedEncodingException {
        return new InternetAddress(address, name).toString();
    }

}
