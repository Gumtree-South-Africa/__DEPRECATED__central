package com.ecg.de.mobile.replyts.deanonymize;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailDoNotAnonymizeHandlerTest {

    public static final String X_DO_NOT_ANONYMIZE = "X-DO-NOT-ANONYMIZE";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext msgContext;
    @Mock
    private Mail mail;

    @Before
    public void setUp() throws Exception {
        when(msgContext.getMail()).thenReturn(Optional.of(mail));
    }

    @Test
    public void shallNotAnonymize() {

        when(mail.getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("fooBar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(mail.getFrom()).thenReturn(originalFrom.getAddress());

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail()).setFrom(originalFrom);
    }

    @Test
    public void shallAnonymizeNoHeader() {

        when(mail.getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn(null);

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(mail.getFrom()).thenReturn(originalFrom.getAddress());

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail(), never()).setFrom(originalFrom);
    }

    @Test
    public void shallAnonymizeWrongPassword() {

        when(mail.getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("bar bar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(mail.getFrom()).thenReturn(originalFrom.getAddress());

        when(mail.getUniqueHeader("Reply-To")).thenReturn("inReplyTo@example.com");

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail(), never()).setFrom(originalFrom);
        verify(msgContext.getOutgoingMail(), never()).addHeader("Reply-To", "inReplyTo@example.com");
    }

    @Test
    public void preserveReplyToIfNotToAnonymize() {

        when(mail.getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("fooBar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(mail.getFrom()).thenReturn(originalFrom.getAddress());
        when(mail.getUniqueHeader("Reply-To")).thenReturn("inReplyTo@example.com");

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail()).setFrom(originalFrom);
        verify(msgContext.getOutgoingMail()).addHeader("Reply-To", "inReplyTo@example.com");
    }

    @Test
    public void preserveReplyToIfNotToAnonymizeWithPersonal() {

        when(mail.getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("fooBar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(mail.getFrom()).thenReturn(originalFrom.getAddress());
        when(mail.getUniqueHeader("Reply-To")).thenReturn("ReplyTo <inReplyTo@example.com>");

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail()).setFrom(originalFrom);
        verify(msgContext.getOutgoingMail()).addHeader("Reply-To", "ReplyTo <inReplyTo@example.com>");
    }


}
