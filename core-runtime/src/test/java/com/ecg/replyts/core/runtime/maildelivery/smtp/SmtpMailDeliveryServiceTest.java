package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SmtpMailDeliveryServiceTest.TestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
  "delivery.smtp.host = localhost",
  "delivery.smtp.timeout.connect.ms = 5000",
  "delivery.smtp.timeout.read.ms = 6000",
  "delivery.smtp.timeout.write.ms = 7000"
})
public class SmtpMailDeliveryServiceTest {
    @Autowired
    private MailDeliveryService mailDeliveryService;

    @Autowired
    private MailTranscoderService mailTranscoderService;

    private JavaMailSenderImpl sender = Mockito.mock(JavaMailSenderImpl.class);

    @Test
    public void configValuesAreUsed() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) ReflectionTestUtils.getField(mailDeliveryService, "sender");

        assertThat(sender.getHost(), is("localhost"));
        assertThat(sender.getPort(), is(25));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"), is("5000"));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.timeout"), is("6000"));
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.writetimeout"), is("7000"));
    }

    @Test
    public void senderGetsCorrectData() throws MailDeliveryException, MessagingException {
        MimeMessage message = mock(MimeMessage.class);

        ReflectionTestUtils.setField(mailDeliveryService, "sender", sender);

        when(mailTranscoderService.toJavaMail(null)).thenReturn(message);

        mailDeliveryService.deliverMail(null);

        verify(sender, times(1)).send(eq(message));
    }

    @Test(expected = MailDeliveryException.class)
    public void mailErrorsThrowExceptions() throws Exception {
        MimeMessage message = mock(MimeMessage.class);

        ReflectionTestUtils.setField(mailDeliveryService, "sender", sender);

        when(mailTranscoderService.toJavaMail(null)).thenReturn(message);
        doThrow(new MailSendException("Relay access denied")).when(sender).send(message);

        mailDeliveryService.deliverMail(null);
    }

    @Configuration
    @Import({ SmtpMailDeliveryService.class, SmtpDeliveryConfig.class })
    static class TestContext {
        @MockBean
        private MailTranscoderService mailTranscoderService;
    }
}
