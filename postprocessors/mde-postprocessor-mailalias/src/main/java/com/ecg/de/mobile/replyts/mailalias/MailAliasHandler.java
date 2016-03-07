package com.ecg.de.mobile.replyts.mailalias;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;

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

            MessageDirection direction = messageProcessingContext.getMessageDirection();

            Optional<String> aliasName = Optional.fromNullable(messageProcessingContext.getMail().getFromName());

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
