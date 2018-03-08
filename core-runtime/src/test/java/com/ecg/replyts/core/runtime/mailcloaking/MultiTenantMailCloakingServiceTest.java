package com.ecg.replyts.core.runtime.mailcloaking;

import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantMailCloakingServiceTest {
    @Mock
    private AnonymizedMailConverter anonymizedMailConverter;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MutableConversation conversation;

    @Mock
    private MessageProcessingContext context;

    @InjectMocks
    private MultiTenantMailCloakingService mailCloakingService;

    @Before
    public void setUp() {
        when(anonymizedMailConverter.fromSecretToMail(conversation, ConversationRole.Buyer)).thenReturn(new MailAddress("new@host.com"));
        when(context.getConversation()).thenReturn(conversation);
    }

    @Test
    public void createNewCloakedMailAddress() {
        when(conversation.getSecretFor(ConversationRole.Buyer)).thenReturn("1");
        assertEquals("new@host.com", mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation).getAddress());
    }

    @Test
    public void uncloaksValidNewMailAddress() {
        when(anonymizedMailConverter.isCloaked(any(MailAddress.class))).thenReturn(true);
        when(anonymizedMailConverter.fromMailToSecret(any(MailAddress.class))).thenReturn("foo");
        when(conversationRepository.getBySecret("foo")).thenReturn(conversation);
        when(conversation.getId()).thenReturn("123@456");
        when(conversation.getSecretFor(ConversationRole.Buyer)).thenReturn("foo");
        assertEquals(new CloakedReceiverContext(conversation, ConversationRole.Buyer), mailCloakingService.resolveUser(new MailAddress("foo@bar")).get());
    }

    @Test
    public void failsToUncloakNewMailAddressForUnknownSecret() {
        when(anonymizedMailConverter.isCloaked(any(MailAddress.class))).thenReturn(true);
        when(anonymizedMailConverter.fromMailToSecret(any(MailAddress.class))).thenReturn("foo");
        assertFalse(mailCloakingService.resolveUser(new MailAddress("foo@bar")).isPresent());
    }


    @Test
    public void emptyIfUserCantBeresolve() {
        when(anonymizedMailConverter.isCloaked(any(MailAddress.class))).thenReturn(true);
        when(anonymizedMailConverter.fromMailToSecret(any(MailAddress.class))).thenReturn("foo");
        when(conversationRepository.getBySecret("foo")).thenReturn(null);

        assertFalse(mailCloakingService.resolveUser(new MailAddress("foo@bar")).isPresent());
    }
}