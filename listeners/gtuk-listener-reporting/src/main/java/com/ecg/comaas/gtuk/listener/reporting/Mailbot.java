package com.ecg.comaas.gtuk.listener.reporting;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

import javax.mail.internet.MimeMessage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TEST CLASS
public class Mailbot {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final JavaMailSender mailSender = createMailSender();

    public void sendMail() {
        final Runnable mailer = new Runnable() {
            @Override
            public void run() {
                mailSender.send(new MimeMessagePreparator() {
                    @Override
                    public void prepare(MimeMessage mimeMessage) throws Exception {
                        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                        message.setFrom("aeaster@gumtree.com");
                        message.setTo("andrew.easter@gmail.com");
                        message.setText("Hello World!");
                        message.setSubject("Hello World!");
                        message.getMimeMessage().addHeader("X-ADID", "12345");
                        message.getMimeMessage().addHeader("X-Platformid", "gumtreeuk2");
                        message.getMimeMessage().addHeader("X-Cust-Categoryid", "1033");
                        message.getMimeMessage().addHeader("X-Cust-Buyerip", "192.168.0.1");
                    }
                });
            }
        };

        scheduler.scheduleAtFixedRate(mailer, 5, 5, TimeUnit.SECONDS);
    }

    private JavaMailSender createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("replyts2vagrant.local");
        return mailSender;
    }
}