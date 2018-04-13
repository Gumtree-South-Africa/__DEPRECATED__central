package com.ecg.comaas.gtuk.listener.reporting.config;


import com.ecg.comaas.gtuk.listener.reporting.Mailbot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailbotConfig {

    @Bean
    public Mailbot mailbot() {
        Mailbot mailbot = new Mailbot();
        mailbot.sendMail();
        return mailbot;
    }
}
