package com.ecg.comaas.mde.postprocessor.mailalias;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailAliasPostProcessorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext msgContext;
    @Mock
    private Mail mail;

    @Before
    public void setUp() {
        when(msgContext.getMail()).thenReturn(Optional.of(mail));
    }

    @Test
    public void setAliasIfExists() {
        when(mail.getFromName()).thenReturn("Buyer Name");
        when(msgContext.getOutgoingMail().getFrom()).thenReturn("buyer@mobile.de");

        new MailAliasPostProcessor().postProcess(msgContext);
        ArgumentCaptor<MailAddress> argument = ArgumentCaptor.forClass(MailAddress.class);

        verify(msgContext.getOutgoingMail()).setFrom(argument.capture());
        assertEquals("Buyer Name <buyer@mobile.de>", argument.getValue().getAddress());
    }


    @Test
    public void aliasIsMissing() {
        when(mail.getFromName()).thenReturn(null);
        new MailAliasPostProcessor().postProcess(msgContext);
        verify(msgContext.getOutgoingMail(), never()).setFrom(any(MailAddress.class));
    }

    @Test
    public void suppressException() {
        when(msgContext.getOutgoingMail()).thenThrow(new RuntimeException());

        try {
            new MailAliasPostProcessor().postProcess(msgContext);
        } catch (Exception e) {
            fail();
        }
    }
}
