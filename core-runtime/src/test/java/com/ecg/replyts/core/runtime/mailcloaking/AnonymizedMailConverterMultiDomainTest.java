package com.ecg.replyts.core.runtime.mailcloaking;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnonymizedMailConverterMultiDomainTest {
    private static final String[] DOMAINS = {"ebay.com", "kijiji.it"};

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Conversation conversation;

    private AnonymizedMailConverter anonymizedMailConverter = new AnonymizedMailConverter("Buyer", "Seller", DOMAINS, false);

    @Before
    public void setUp() throws Exception {
        when(context.getConversation()).thenReturn(conversation);

        when(conversation.getSecretFor(any(ConversationRole.class))).thenReturn("any");
    }

    @Test
    public void uncloakValidAddressRegardlessOfSeparator() {
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer.1234@ebay.com")));
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer-1234@ebay.com")));
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("BuyerX1234@ebay.com")));
        assertEquals("234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer1234@ebay.com")));
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer.1234@kijiji.it")));
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer-1234@kijiji.it")));
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("BuyerX1234@kijiji.it")));
        assertEquals("234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer1234@kijiji.it")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failWhenNoSeparatorAndMessageId() {
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer@ebay.com")));
    }

    @Test
    public void testAnonymizeBuyerDomain() throws Exception {
        // set header value
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("buyer_domain", "kijiji.it"));
        assertThat(anonymizedMailConverter.fromSecretToMail(conversation, ConversationRole.Buyer).getAddress(), equalTo("Buyer-any@kijiji.it"));

    }

    @Test
    public void testAnonymizeSellerDomain() throws Exception {
        // set header value
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("seller_domain", "kijiji.it"));
        assertThat(anonymizedMailConverter.fromSecretToMail(conversation, ConversationRole.Seller).getAddress(), equalTo("Seller-any@kijiji.it"));
    }

    @Test
    public void testAnonymizeSellerDomainFallback() throws Exception {
        // set header value
        when(conversation.getCustomValues()).thenReturn(Collections.EMPTY_MAP);
        assertThat(anonymizedMailConverter.fromSecretToMail(conversation, ConversationRole.Seller).getAddress(), equalTo("Seller-any@ebay.com"));
    }

    @Test
    public void validCloakedEmailAddressesAreUncloakable() {
        assertTrue(anonymizedMailConverter.isCloaked(new MailAddress("Buyer.1234@ebay.com")));
        assertTrue(anonymizedMailConverter.isCloaked(new MailAddress("Seller.5678@kijiji.it")));
    }

    @Test
    public void invalidRoleNameIsNotUncloakable() {
        assertFalse(anonymizedMailConverter.isCloaked(new MailAddress("Something.1234@kijiji.it")));
    }

    @Test
    public void invalidDomainIsNotUncloakable() {
        assertFalse(anonymizedMailConverter.isCloaked(new MailAddress("Buyer.1234@ecg.com")));
    }

    @Test
    public void uncloakValidAddress() {
        assertEquals("1234", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer.1234@kijiji.it")));
        assertEquals("4321", anonymizedMailConverter.fromMailToSecret(new MailAddress("Buyer.4321@ebay.com")));
    }
}
