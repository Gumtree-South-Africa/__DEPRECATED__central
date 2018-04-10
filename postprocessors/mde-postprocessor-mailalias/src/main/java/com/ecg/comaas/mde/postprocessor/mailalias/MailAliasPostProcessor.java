package com.ecg.comaas.mde.postprocessor.mailalias;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

class MailAliasPostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MailAliasPostProcessor.class);

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
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
