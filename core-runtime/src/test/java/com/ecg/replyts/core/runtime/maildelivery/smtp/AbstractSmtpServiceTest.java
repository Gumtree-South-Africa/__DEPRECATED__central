/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.runtime.maildelivery.smtp;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author alindhorst
 */
public class AbstractSmtpServiceTest {

    private AbstractSmtpService instance;

    @Before
    public void setup() {
        SmtpDeliveryConfig config = new SmtpDeliveryConfig();
        config.setHost("localhost");
        SmtpPing ping = new SmtpPing();
        instance = new AbstractSmtpService(ping, config) {
        };
    }

    @Test
    public void configValuesAreUsed() {
        JavaMailSenderImpl sender = instance.getSender();
        assertThat(sender.getHost(), is("localhost"));
        assertThat(sender.getPort(), is(25));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"), is("0"));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.timeout"), is("0"));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.writetimeout"), is("0"));
    }

    @Test
    public void timeoutValuesAreUsed() throws Exception {
        SmtpDeliveryConfig config = new SmtpDeliveryConfig();
        config.setConnectTimeoutInMs(5000);
        config.setReadTimeoutInMs(6000);
        config.setWriteTimeoutInMs(7000);
        SmtpPing ping = new SmtpPing();
        instance = new AbstractSmtpService(ping, config) {
        };

        JavaMailSenderImpl sender = instance.getSender();

        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"), is("5000"));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.timeout"), is("6000"));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.writetimeout"), is("7000"));
    }

    @Test
    public void senderGetsCorrectData() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        JavaMailSenderImpl sender = Mockito.mock(JavaMailSenderImpl.class);
        ReflectionTestUtils.setField(instance, "sender", sender);
        instance.send(message);
        verify(sender, times(1)).send(any(MimeMessage.class));
    }

    @Test(expected = MailException.class)
    public void mailErrorsThrowExceptions() throws Exception {
        MimeMessage message = mock(MimeMessage.class);
        JavaMailSenderImpl sender = Mockito.mock(JavaMailSenderImpl.class);
        ReflectionTestUtils.setField(instance, "sender", sender);
        doThrow(new MailSendException("Relay access denied")).when(sender).send(message);
        instance.send(message);
    }
}
