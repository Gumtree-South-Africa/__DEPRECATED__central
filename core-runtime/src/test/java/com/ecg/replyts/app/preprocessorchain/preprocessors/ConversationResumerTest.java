package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConversationResumerTest {
    @Mock
    private ConversationRepository repo;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Mail mail;

    @Mock
    private MutableConversation conv;

    @Captor
    private ArgumentCaptor<AddCustomValueCommand> addCustomValueCommandCapture;

    private ConversationResumer resumer;
    private ConversationIndexKey key = new ConversationIndexKey("buyer@host.com", "seller@host.com", "123");

    @Before
    public void setUp() {
        when(context.getMail()).thenReturn(mail);
        when(context.getConversation()).thenReturn(conv);

        when(conv.getCustomValues()).thenReturn(Collections.<String, String>emptyMap());
        when(conv.getId()).thenReturn("convID");
        
        resumer = new ConversationResumer(repo, true);
    }

    @Test
    public void attachesToConversationIfFound() {
        prepareMailFromBuyerToSeller();
        prepareExistingConversation();

        assertTrue(resumer.resumeExistingConversation(context));

        verify(context).setConversation(conv);
        verify(context).setMessageDirection(MessageDirection.BUYER_TO_SELLER);
    }

    @Test
    public void attachesToConversationIfFoundForSellerToBuyer() {
        prepareMailFromSellerToBuyer();
        prepareExistingConversation();

        assertTrue(resumer.resumeExistingConversation(context));

        verify(context).setConversation(conv);
        verify(context).setMessageDirection(MessageDirection.SELLER_TO_BUYER);
    }

    @Test
    public void doesNotResumeConversationIfFeatureDisabled() {
        resumer = new ConversationResumer(repo, false);
        prepareMailFromBuyerToSeller();
        prepareExistingConversation();

        assertFalse(resumer.resumeExistingConversation(context));
        verify(context, never()).setConversation(conv);
    }

    @Test
    public void doesNotAttachAnythingIfNoConversationFound() {
        prepareMailFromBuyerToSeller();
        prepareNonExistingConversation();

        assertFalse(resumer.resumeExistingConversation(context));

        verify(context, never()).setConversation(any(MutableConversation.class));
        verify(context, never()).setMessageDirection(any(MessageDirection.class));
    }

    @Test
    public void extendsNewCustomValues() {
        prepareMailFromBuyerToSeller();
        prepareExistingConversation();

        when(mail.getCustomHeaders()).thenReturn(ImmutableMap.of("foo", "bar", "scot", "car"));

        resumer.resumeExistingConversation(context);

        verify(context, times(2)).addCommand(addCustomValueCommandCapture.capture());
        List<AddCustomValueCommand> values = addCustomValueCommandCapture.getAllValues();
        assertEquals("foo", values.get(0).getKey());
        assertEquals("bar", values.get(0).getValue());
        assertEquals("scot", values.get(1).getKey());
        assertEquals("car", values.get(1).getValue());
    }

    private void prepareMailFromBuyerToSeller() {
        when(mail.getFrom()).thenReturn("buyer@host.com");
        when(mail.getDeliveredTo()).thenReturn("seller@host.com");
        when(mail.getAdId()).thenReturn("123");
    }

    private void prepareMailFromSellerToBuyer() {
        when(mail.getFrom()).thenReturn("seller@host.com");
        when(mail.getDeliveredTo()).thenReturn("buyer@host.com");
        when(mail.getAdId()).thenReturn("123");
    }

    private void prepareExistingConversation() {
        when(repo.findExistingConversationFor(any())).thenReturn(Optional.empty());
        when(repo.findExistingConversationFor(key)).thenReturn(Optional.of(conv));
    }

    private void prepareNonExistingConversation() {
        when(repo.findExistingConversationFor(any())).thenReturn(Optional.empty());
    }


}
