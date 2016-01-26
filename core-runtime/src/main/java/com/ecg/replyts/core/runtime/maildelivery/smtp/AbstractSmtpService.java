package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.api.sanitychecks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.internet.MimeMessage;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Shared Baseclass for the {@link SmtpMailDeliveryService}.
 * It provides a default configuration for SMTP access and is able to deliver {@link MimeMessage}s.
 *
 * @author huttar
 */
public abstract class AbstractSmtpService implements CheckProvider, Check {

    private SmtpPing smtpPing;
    private JavaMailSenderImpl sender;
    private SmtpDeliveryConfig config;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSmtpService.class);

    public AbstractSmtpService(SmtpPing smtpPing, SmtpDeliveryConfig config) {
        this.smtpPing = smtpPing;
        setConfig(config);
    }

    private void setConfig(SmtpDeliveryConfig config) {
        this.config = config;

        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.connectiontimeout", String.valueOf(config.getConnectTimeoutInMs()));
        mailProps.setProperty("mail.smtp.timeout", String.valueOf(config.getReadTimeoutInMs()));
        mailProps.setProperty("mail.smtp.writetimeout", String.valueOf(config.getWriteTimeoutInMs()));

        sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setJavaMailProperties(mailProps);

        if (config.isLoginSpecified()) {

            sender.setUsername(config.getUsername());
            sender.setPassword(config.getPassword());

            LOGGER.info(String.format("Mail Delivery SMTP Configuration. %s@%s", config.getUsername(),
                    config.getHost()));
        } else {
            LOGGER.info(String.format("Mail Delivery SMTP Configuration. anonymous user@%s", config.getHost()));
        }
    }

    public SmtpDeliveryConfig getConfig() {
        return config;
    }

    public JavaMailSenderImpl getSender() {
        return sender;
    }

    public void send(MimeMessage msg) {
        try {
            getSender().send(msg);
        } catch (MailException mailException) {
            LOGGER.error("Problem during mail delivery", mailException);
            throw mailException;
        }

    }

    @Override
    public Result execute() throws Exception {
        try {
            smtpPing.ping(config.getHost(), config.getPort(), 1000);
            return Result.createResult(Status.OK, Message.shortInfo("SMTP connection established successfully"));
        } catch (Exception ex) {
            return Result.createResult(Status.CRITICAL, Message.fromException(ex));
        }
    }

    @Override
    public String getName() {
        return "SMTP-" + config.getHost();
    }

    @Override
    public String getCategory() {
        return "SMTP";
    }

    @Override
    public String getSubCategory() {
        return getClass().getSimpleName();
    }

    @Override
    public List<Check> getChecks() {
        return Collections.singletonList((Check) this);
    }
}
