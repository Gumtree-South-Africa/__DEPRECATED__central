package com.ecg.de.mobile.replyts.deanonymize;

import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * User: beckart
 */
public class MailDoNotAnonymizeHandlerTest {

    public static final String X_DO_NOT_ANONYMIZE = "X-DO-NOT-ANONYMIZE";

    private MessageProcessingContext msgContext;


    @Before
    public void setUp() throws Exception {
        msgContext = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
    }


    @Test
    public void shallNotAnonymize() {

        when(msgContext.getMail().getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("fooBar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(msgContext.getOriginalFrom()).thenReturn(originalFrom);

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail()).setFrom(originalFrom);
    }

    @Test
    public void shallAnonymizeNoHeader() {

        when(msgContext.getMail().getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn(null);

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(msgContext.getOriginalFrom()).thenReturn(originalFrom);

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail(), never()).setFrom(originalFrom);
    }

    @Test
    public void shallAnonymizeWrongPassword() {

        when(msgContext.getMail().getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("bar bar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(msgContext.getOriginalFrom()).thenReturn(originalFrom);

        when(msgContext.getMail().getUniqueHeader("Reply-To")).thenReturn("inReplyTo@example.com");



        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail(), never()).setFrom(originalFrom);
        verify(msgContext.getOutgoingMail(), never()).addHeader("Reply-To", "inReplyTo@example.com");
    }

    @Test
    public void preserveReplyToIfNotToAnonymize() {

        when(msgContext.getMail().getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("fooBar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(msgContext.getOriginalFrom()).thenReturn(originalFrom);
        when(msgContext.getMail().getUniqueHeader("Reply-To")).thenReturn("inReplyTo@example.com");

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail()).setFrom(originalFrom);
        verify(msgContext.getOutgoingMail()).addHeader("Reply-To", "inReplyTo@example.com");
    }

    @Test
    public void preserveReplyToIfNotToAnonymizeWithPersonal() {

        when(msgContext.getMail().getUniqueHeader(X_DO_NOT_ANONYMIZE)).thenReturn("fooBar");

        MailAddress originalFrom = new MailAddress("Benjamin <foobar>");
        when(msgContext.getOriginalFrom()).thenReturn(originalFrom);
        when(msgContext.getMail().getUniqueHeader("Reply-To")).thenReturn("ReplyTo <inReplyTo@example.com>");

        new MailDoNotAnonymizeHandler(msgContext, "foobar").handle();

        verify(msgContext.getOutgoingMail()).setFrom(originalFrom);
        verify(msgContext.getOutgoingMail()).addHeader("Reply-To", "ReplyTo <inReplyTo@example.com>");
    }


}
