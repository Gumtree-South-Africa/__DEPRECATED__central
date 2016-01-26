/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author alindhorst
 */
public class SmtpDeliveryServiceConfiguration {
    @Value("${delivery.smtp.host}")
    private String host;

    @Value("${delivery.smtp.username:}")
    private String username;

    @Value("${delivery.smtp.password:}")
    private String password;

    @Value("${delivery.smtp.port:25}")
    private int port;

    @Value("${delivery.smtp.timeout.connect.ms:10000}")
    private int connectTimeoutMs;

    @Value("${delivery.smtp.timeout.read.ms:10000}")
    private int readTimeoutMs;

    @Value("${delivery.smtp.timeout.write.ms:10000}")
    private int writeTimeoutMs;

    private SmtpPing smtpPing = new SmtpPing();

    private MailTranscoderService mailTranscoderService = new MailTranscoderService();

    @Bean
    public SmtpPing buildSmtpPing() {
        return smtpPing;
    }

    @Bean
    public MailTranscoderService buildMailTranscoderService() {
        return mailTranscoderService;
    }

    @Bean
    public MailDeliveryService buildMailDeliveryService() {
        SmtpDeliveryConfig config = new SmtpDeliveryConfig();
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        config.setPort(port);
        config.setConnectTimeoutInMs(connectTimeoutMs);
        config.setReadTimeoutInMs(readTimeoutMs);
        config.setWriteTimeoutInMs(writeTimeoutMs);

        return new SmtpMailDeliveryService(mailTranscoderService, smtpPing, config);
    }

}
