package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.springframework.mail.MailException;

import javax.mail.MessagingException;

/**
 * {@link MailDeliveryService} implementation that sends a mail via SMTP protocol
 *
 * @author huttar
 */
public class SmtpMailDeliveryService extends AbstractSmtpService implements MailDeliveryService {

    private MailTranscoderService mailTranscoderService;

    public SmtpMailDeliveryService(MailTranscoderService mailTranscoderService, SmtpPing smtpPing, SmtpDeliveryConfig config) {
        super(smtpPing, config);
        this.mailTranscoderService = mailTranscoderService;
    }

    @Override
    public void deliverMail(Mail m) throws MailDeliveryException {
        try {
            send(mailTranscoderService.toJavaMail(m));
        } catch (MessagingException | MailException ex) {
            throw new MailDeliveryException(ex);
        }
    }

}
