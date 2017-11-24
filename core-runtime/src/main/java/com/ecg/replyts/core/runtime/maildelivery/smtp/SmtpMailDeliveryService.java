package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

@Component
@Qualifier("smtpMailDeliveryService")
public class SmtpMailDeliveryService implements MailDeliveryService {
    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailDeliveryService.class);

    @Autowired
    private MailTranscoderService mailTranscoderService;

    @Autowired
    private SmtpDeliveryConfig config;

    private JavaMailSenderImpl sender;

    @PostConstruct
    private void setConfig() {
        sender = new JavaMailSenderImpl();

        sender.setHost(config.getHost());
        sender.setPort(config.getPort());

        Properties properties = new Properties();

        properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(config.getConnectTimeoutInMs()));
        properties.setProperty("mail.smtp.timeout", String.valueOf(config.getReadTimeoutInMs()));
        properties.setProperty("mail.smtp.writetimeout", String.valueOf(config.getWriteTimeoutInMs()));

        sender.setJavaMailProperties(properties);

        if (config.isLoginSpecified()) {
            sender.setUsername(config.getUsername());
            sender.setPassword(config.getPassword());

            LOG.info("Mail Delivery SMTP Configuration: {}@{}", config.getUsername(), config.getHost());
        } else {
            LOG.info("Mail Delivery SMTP Configuration: <anonymous>@{}", config.getHost());
        }
    }

    @Override
    public void deliverMail(Mail m) throws MailDeliveryException {
        try {
            MimeMessage message = mailTranscoderService.toJavaMail(m);

            sender.send(message);
        } catch (MailSendException e) {
            LOG.error("Unable to send mail message. To: {}, Delivered-To: {}, From: {}, messageId: {}", m.getTo(), m.getDeliveredTo(), m.getFrom(), m.getMessageId(), e);
            e.getFailedMessages().forEach((message, exception) -> LOG.error("Failed message: {}", message, exception));

            throw new MailDeliveryException(e);
        } catch (MessagingException | MailException e) {
            LOG.error("Unable to send mail message. To: {}, Delivered-To: {}, From: {}, messageId: {}", m.getTo(), m.getDeliveredTo(), m.getFrom(), m.getMessageId(), e);

            throw new MailDeliveryException(e);
        }
    }
}
