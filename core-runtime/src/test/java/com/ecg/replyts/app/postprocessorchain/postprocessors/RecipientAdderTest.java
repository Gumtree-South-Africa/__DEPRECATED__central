package com.ecg.replyts.app.postprocessorchain.postprocessors;


import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RecipientAdderTest {

    @Mock
    private MessageProcessingContext context;

    @Mock
    private MutableMail mail;

    @Mock
    private Conversation conversation;

    @Before
    public void setUp() {
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMail()).thenReturn(mail);
        when(context.getOutgoingMail()).thenReturn(mail);

        when(conversation.getUserIdFor(ConversationRole.Seller)).thenReturn("seller@domain.tld");
        when(conversation.getUserIdFor(ConversationRole.Buyer)).thenReturn("buyer@domain.tld");
    }

    @Test
    public void notOverriddenAddress() {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(mail.getDeliveredTo()).thenReturn("");
        new RecipientAdder(false).postProcess(context);
        verify(mail).setTo(new MailAddress("seller@domain.tld"));
    }

    @Test
    public void emptyOriginalTo() {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(mail.getDeliveredTo()).thenReturn("");
        new RecipientAdder(true).postProcess(context);
        verify(mail).setTo(new MailAddress("seller@domain.tld"));
    }

    @Test
    public void nullOriginalTo() {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(mail.getDeliveredTo()).thenReturn(null);
        new RecipientAdder(true).postProcess(context);
        verify(mail).setTo(new MailAddress("seller@domain.tld"));
    }

    @Test
    public void sameOriginalTo() {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(mail.getDeliveredTo()).thenReturn("seller@domain.tld");
        new RecipientAdder(true).postProcess(context);
        verify(mail).setTo(new MailAddress("seller@domain.tld"));
    }

    @Test
    public void overriddenAddress() {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(mail.getDeliveredTo()).thenReturn("overridden@domain.tld");
        new RecipientAdder(true).postProcess(context);
        verify(mail).setTo(new MailAddress("overridden@domain.tld"));
    }
}
