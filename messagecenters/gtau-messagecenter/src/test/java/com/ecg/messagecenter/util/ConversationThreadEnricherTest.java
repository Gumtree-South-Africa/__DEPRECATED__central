package com.ecg.messagecenter.util;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversationThreadEnricherTest {
    private ConversationThreadEnricher enricher;
    private MailCloakingService mailCloakingService;
    private ConversationRepository conversationRepository;
    private String buyerId = "buyer1@seller.com";
    private String sellerId = "seller1@seller.com";
    private String buyerAnonymousEmail = "Buyer.3o9sdp1ykrnv@test-platform.com";
    private String sellerAnonymousEmail = "Seller.3vvbtmecyi5ue@test-platform.com";
    private String buyerSecret = "8qdcsuwwpss6";
    private String sellerSecret = "1zsvmrx9nmmlm";
    private String status = "ACTIVE";
    private ConversationThread conversationThread = new ConversationThread(
            "2",
            "abc",
            now(),
            now().minusDays(179),
            now(),
            true,
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Lists.newArrayList(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty(),
            Optional.<String>empty());


    @Before
    public void setup() {
        mailCloakingService = mock(MailCloakingService.class);
        conversationRepository = mock(ConversationRepository.class);

        NewConversationCommand newConversationCommand = new NewConversationCommand(
                "1", "1",
                buyerId, sellerId, buyerSecret,
                sellerSecret, DateTime.now(), ConversationState.ACTIVE,
                Maps.<String, String>newHashMap()

        );

        when(conversationRepository.getById(anyString()))
                .thenReturn(DefaultMutableConversation.create(newConversationCommand));

        when(mailCloakingService.createdCloakedMailAddress(eq(ConversationRole.Seller), any(Conversation.class)))
                .thenReturn(new MailAddress(sellerAnonymousEmail));

        when(mailCloakingService.createdCloakedMailAddress(eq(ConversationRole.Buyer), any(Conversation.class)))
                .thenReturn(new MailAddress(buyerAnonymousEmail));

        enricher = new ConversationThreadEnricher(mailCloakingService, conversationRepository);
    }

    @Test
    public void shouldEnrichAnonymousEmailsAndStateWhenFieldsNotInitialized() throws Exception {

        // given
        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Lists.newArrayList(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty());

        //when
        ConversationThread conversationThreadEnriched = enricher.enrichAnonymousEmailsAndState(conversationThread, Optional.<Conversation>empty());

        //then
        assertTrue(conversationThreadEnriched.getBuyerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getSellerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getStatus().isPresent());
    }

    @Test
    public void shouldNotEnrichAnonymousEmailsAndStateWhenFieldsAlreadyInitialized() throws Exception {

        // given
        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Optional.<String>empty(),
                Lists.newArrayList(),
                Optional.<String>empty(),
                Optional.of(buyerAnonymousEmail),
                Optional.of(sellerAnonymousEmail),
                Optional.of(status));

        //when
        ConversationThread conversationThreadEnriched = enricher.enrichAnonymousEmailsAndState(conversationThread, Optional.<Conversation>empty());

        //then
        assertTrue(buyerAnonymousEmail.equals(conversationThreadEnriched.getBuyerAnonymousEmail().get()));
        assertTrue(sellerAnonymousEmail.equals(conversationThreadEnriched.getSellerAnonymousEmail().get()));
        assertTrue(status.equals(conversationThreadEnriched.getStatus().get()));
    }

    @Test
    public void shouldCallEnrichAnonymousEmailsAndStateWhenCallingPublicEnrichmentMethod() {

        // given
        ConversationThreadEnricher enricherSpy = spy(enricher);

        // when
        enricherSpy.enrich(conversationThread, Optional.<Conversation>empty());

        // then
        verify(enricherSpy).enrichAnonymousEmailsAndState(conversationThread, Optional.<Conversation>empty());
    }

}