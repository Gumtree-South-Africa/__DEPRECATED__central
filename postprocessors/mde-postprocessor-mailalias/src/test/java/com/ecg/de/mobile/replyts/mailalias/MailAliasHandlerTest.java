package com.ecg.de.mobile.replyts.mailalias;

import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * User: beckart
 */
public class MailAliasHandlerTest {

    private MessageProcessingContext msgContext;
    private Map<String, String> headers;

    @Before
    public void setUp() throws Exception {
        headers = new HashMap<String, String>();
        msgContext = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void setAliasIfExists() {

        when(msgContext.getMail().getFromName()).thenReturn("Buyer Name");
        when(msgContext.getOutgoingMail().getFrom()).thenReturn("buyer@mobile.de");

        new MailAliasHandler(msgContext).handle();

        ArgumentCaptor<MailAddress> argument = ArgumentCaptor.forClass(MailAddress.class);

        verify(msgContext.getOutgoingMail()).setFrom(argument.capture());

        assertEquals("Buyer Name <buyer@mobile.de>", argument.getValue().getAddress());
    }


    @Test
    public void aliasIsMissing() {

        when(msgContext.getMail().getFromName()).thenReturn(null);

        new MailAliasHandler(msgContext).handle();

        verify(msgContext.getOutgoingMail(), never()).setFrom(any(MailAddress.class));

    }

    @Test
    public void suppressException() {
        when(msgContext.getOutgoingMail()).thenThrow(new RuntimeException());

        try {
            new MailAliasHandler(msgContext).handle();
        } catch (Exception e) {
            fail();
        }

    }
}
