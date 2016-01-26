package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationStartInfoTest {

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Mail mail;
    private ConversationStartInfo info;

    @Before
    public void setUp() {
        when(context.getMail()).thenReturn(mail);

        when(mail.getFrom()).thenReturn("fromfield@host.com");
        when(mail.getDeliveredTo()).thenReturn("deliveredtofield@host.com");
        when(mail.getAdId()).thenReturn("123");
        this.info = new ConversationStartInfo(context);
    }

    @Test
    public void usesFromAsSBuyer() {
        assertEquals(new MailAddress("fromfield@host.com"), info.buyer() );
    }

    @Test
    public void replyToHasPreferenceOverFromAsBuyer() {
        when(mail.getReplyTo()).thenReturn("replytofield@host.com");
        assertEquals(new MailAddress("replytofield@host.com"), info.buyer() );
    }

    @Test
    public void usesDeliveredToFieldAsSeller() {
        assertEquals(new MailAddress("deliveredtofield@host.com"), info.seller() );
    }

    @Test
    public void readsAdIdFromMail() {
        assertEquals("123", info.adId());
    }
}
