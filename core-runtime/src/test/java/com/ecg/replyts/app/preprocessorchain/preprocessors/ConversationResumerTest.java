package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierServiceFactory;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
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
        when(context.getSender()).thenReturn(new MailAddress("buyer@host.com"));
        when(context.getRecipient()).thenReturn(new MailAddress("seller@host.com"));
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(conv.getCustomValues()).thenReturn(Collections.emptyMap());
        when(conv.getId()).thenReturn("convID");

        resumer = new IdBasedConversationResumer();

        ReflectionTestUtils.setField(resumer, "userIdentifierService", new UserIdentifierServiceFactory().createUserIdentifierService());
    }

    @After
    public void after()
    {
        MDC.clear();
    }

    @Test
    public void attachesToConversationIfFound() {
        prepareMailFromBuyerToSeller();
        prepareExistingConversation();

        assertThat(resumer.resumeExistingConversation(repo, context)).isTrue();
        assertThat(MDC.get(CONVERSATION_ID)).isEqualTo("convID");
        assertThat(MDC.get(MAIL_FROM)).isEqualTo("buyer@host.com");
        assertThat(MDC.get(MAIL_TO)).isEqualTo("seller@host.com");
        assertThat(MDC.get(MAIL_DIRECTION)).isEqualTo("BUYER_TO_SELLER");

        verify(context).setConversation(conv);
        verify(context).setMessageDirection(MessageDirection.BUYER_TO_SELLER);
    }

    @Test
    public void attachesToConversationIfFoundForSellerToBuyer() {
        prepareMailFromSellerToBuyer();
        prepareExistingConversation();

        assertThat(resumer.resumeExistingConversation(repo, context)).isTrue();

        verify(context).setConversation(conv);
        verify(context).setMessageDirection(MessageDirection.SELLER_TO_BUYER);
    }

    @Test
    public void doesNotAttachAnythingIfNoConversationFound() {
        prepareMailFromBuyerToSeller();
        prepareNonExistingConversation();

        assertThat(resumer.resumeExistingConversation(repo, context)).isFalse();

        verify(context, never()).setConversation(any(MutableConversation.class));
        verify(context, never()).setMessageDirection(any(MessageDirection.class));
    }

    @Test
    public void extendsNewCustomValues() {
        prepareMailFromBuyerToSeller();
        prepareExistingConversation();

        when(mail.getCustomHeaders()).thenReturn(ImmutableMap.of("foo", "bar", "scot", "car"));

        resumer.resumeExistingConversation(repo, context);

        verify(context, times(2)).addCommand(addCustomValueCommandCapture.capture());
        List<AddCustomValueCommand> values = addCustomValueCommandCapture.getAllValues();
        assertThat(values.get(0).getKey()).isEqualTo("foo");
        assertThat(values.get(0).getValue()).isEqualTo("bar");
        assertThat(values.get(1).getKey()).isEqualTo("scot");
        assertThat(values.get(1).getValue()).isEqualTo("car");
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
