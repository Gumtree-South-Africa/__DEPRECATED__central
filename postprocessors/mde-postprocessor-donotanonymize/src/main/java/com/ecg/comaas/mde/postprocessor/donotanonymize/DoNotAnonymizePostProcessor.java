package com.ecg.comaas.mde.postprocessor.donotanonymize;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

class DoNotAnonymizePostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DoNotAnonymizePostProcessor.class);

    private static final String X_DO_NOT_ANONYMIZE = "X-DO-NOT-ANONYMIZE";

    private final String password;

    DoNotAnonymizePostProcessor(String password) {
        this.password = password;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        try {
            if(doNotAnonymize(context)) {
                MailAddress fromMailAddress = preserveOriginalFrom(context);
                preserveReplyTo(context);
                LOG.trace("De-anonymizing mail sender {}", fromMailAddress.getAddress());
            }
        } catch (Exception e) {
            LOG.error("Error while de-anonymizing!", e);
        }
    }

    private boolean doNotAnonymize(MessageProcessingContext context) {
        Optional<Mail> mail = context.getMail();
        if (!mail.isPresent()) {
            return false;
        }
        Optional<String> doNotAnonymizeHeader = Optional.ofNullable(mail.get().getUniqueHeader(X_DO_NOT_ANONYMIZE));
        return doNotAnonymizeHeader.isPresent() && doNotAnonymizeHeader.get().trim().toLowerCase().equals(password.toLowerCase());
    }

    private String formatMailAddress(String name, String address) throws UnsupportedEncodingException {
        return new InternetAddress(address, name).toString();
    }

    private MailAddress preserveOriginalFrom(MessageProcessingContext context) throws UnsupportedEncodingException {
        Mail mail = context.getMail().get();
        MailAddress fromMailAddress = new MailAddress(mail.getFrom());
        Optional<String> aliasName = Optional.ofNullable(mail.getFromName());
        MailAddress mailAddress;

        if(aliasName.isPresent() && StringUtils.hasText(aliasName.get())) {
            mailAddress = new MailAddress(formatMailAddress(aliasName.get(), fromMailAddress.getAddress()));
        } else {
            mailAddress = fromMailAddress;
        }

        context.getOutgoingMail().setFrom(mailAddress);
        return fromMailAddress;
    }

    private void preserveReplyTo(MessageProcessingContext context) {
        String replyTo = context.getMail().get().getUniqueHeader("Reply-To");

        if(StringUtils.hasText(replyTo)) {
            context.getOutgoingMail().addHeader("Reply-To", replyTo);
        }
    }
}
