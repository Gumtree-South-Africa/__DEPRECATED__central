package com.gumtree.replyts2.plugins.reporting.config;


import com.gumtree.replyts2.plugins.reporting.Mailbot;
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
