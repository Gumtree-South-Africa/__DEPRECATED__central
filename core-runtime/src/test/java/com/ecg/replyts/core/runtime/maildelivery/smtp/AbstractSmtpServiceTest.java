/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.runtime.maildelivery.smtp;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author alindhorst
 */
@RunWith(SpringRunner.class)
public class AbstractSmtpServiceTest {

    private AbstractSmtpService instance;

    @MockBean
    private SmtpDeliveryConfig config;

    @Before
    public void setup() {
        when(config.getHost()).thenReturn("localhost");
        when(config.getPort()).thenReturn(25);

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
        when(config.getConnectTimeoutInMs()).thenReturn(5000);
        when(config.getReadTimeoutInMs()).thenReturn(6000);
        when(config.getWriteTimeoutInMs()).thenReturn(7000);
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
