package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.api.model.mail.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Service capable of transcoding a ReplyTS mail back into "regular" Mail objects that can be used by services that
 * utilize
 *
 * @author huttar
 */
public class MailTranscoderService {

    private Session sess = Session.getInstance(new Properties());

    private static final Logger LOGGER = LoggerFactory.getLogger(MailTranscoderService.class);

    /**
     * Takes a ReplyTS Mail and "converts" it into a normal java-mail message.
     *
     * @param mail the mail that shall be converted
     * @return converted java-mail message
     * @throws MessagingException if the java mail message instance could not read the mail format it got
     */
    public MimeMessage toJavaMail(Mail mail) throws MessagingException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayInputStream bin;
        try {
            mail.writeTo(bout);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Preparing mail for sending:\n{}", new String(bout.toByteArray()));
            }
            bin = new ByteArrayInputStream(bout.toByteArray());
            return new MimeMessage(sess, bin);
        } catch (IOException ex) {
            LOGGER.error("Could not convert message", ex);
            return null;
        }
    }
}
