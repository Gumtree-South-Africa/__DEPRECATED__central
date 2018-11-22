package com.ecg.replyts.core.runtime.mailcloaking;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnonymizedMailConverterTest {
    private static final String[] DOMAINS = {"ebay.com"};

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Conversation conversation;

    @Mock
    private Mail mail;

    private AnonymizedMailConverter anonymizedMailConverter = new AnonymizedMailConverter("Buyer", "Seller", DOMAINS);

    @Test
    public void validCloakedEmailAddressesAreUncloakable() {
        assertTrue(anonymizedMailConverter.isCloaked(new MailAddress("Buyer.1234@ebay.com")));
        assertTrue(anonymizedMailConverter.isCloaked(new MailAddress("Seller.5678@ebay.com")));
    }

    @Test
    public void invalidRoleNameIsNotUncloakable() {
        assertFalse(anonymizedMailConverter.isCloaked(new MailAddress("Something.1234@ebay.com")));
    }

    @Test
    public void invalidDomainIsNotUncloakable() {
        assertFalse(anonymizedMailConverter.isCloaked(new MailAddress("Buyer.1234@ecg.com")));
    }

    @Test
    public void uncloakValidAddressRegardlessOfSeparator() {
        assertEquals("1234", anonymizedMailConverter.toParticipantSecret(new MailAddress("Buyer.1234@ebay.com")));
        assertEquals("1234", anonymizedMailConverter.toParticipantSecret(new MailAddress("Buyer-1234@ebay.com")));
        assertEquals("1234", anonymizedMailConverter.toParticipantSecret(new MailAddress("BuyerX1234@ebay.com")));
        assertEquals("234", anonymizedMailConverter.toParticipantSecret(new MailAddress("Buyer1234@ebay.com")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failWhenNoSeparatorAndMessageId() {
        assertEquals("1234", anonymizedMailConverter.toParticipantSecret(new MailAddress("Buyer@ebay.com")));
    }

    @Test
    public void uncloakIgnoreCase() {
        assertEquals("1234", anonymizedMailConverter.toParticipantSecret(new MailAddress("BUYER-1234@EBAY.COM")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void uncloakInvalidAddress() {
        anonymizedMailConverter.toParticipantSecret(new MailAddress("Buyer.1234@example.com"));
    }

    @Test
    public void cloakValidAddressWithSpecialChars() {
        anonymizedMailConverter = new AnonymizedMailConverter("Buyer", "Seller", DOMAINS);
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getSecretFor(ConversationRole.Buyer)).thenReturn("1234");
        when(context.getMail()).thenReturn(Optional.of(mail));
        MailAddress expected = new MailAddress("Buyer-1234@ebay.com");
        assertEquals(expected, anonymizedMailConverter.toCloakedEmailAddress(conversation, ConversationRole.Buyer));
    }

    @Test
    public void cloakValidAddress() {
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getSecretFor(ConversationRole.Buyer)).thenReturn("1234");
        MailAddress expected = new MailAddress("Buyer-1234@ebay.com");
        when(context.getMail()).thenReturn(Optional.of(mail));
        assertEquals(expected, anonymizedMailConverter.toCloakedEmailAddress(conversation, ConversationRole.Buyer));
    }
}
