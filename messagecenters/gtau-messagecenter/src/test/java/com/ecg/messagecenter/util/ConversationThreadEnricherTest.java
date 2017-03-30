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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
  "messages.conversations.enrichment.on.read = false"
})
public class ConversationThreadEnricherTest {
    @Autowired
    private ConversationThreadEnricher enricher;

    @Autowired
    private MailCloakingService mailCloakingService;

    @Autowired
    private ConversationRepository conversationRepository;

    private String buyerId = "buyer1@seller.com";
    private String sellerId = "seller1@seller.com";
    private String buyerAnonymousEmail = "Buyer.3o9sdp1ykrnv@test-platform.com";
    private String sellerAnonymousEmail = "Seller.3vvbtmecyi5ue@test-platform.com";
    private String buyerSecret = "8qdcsuwwpss6";
    private String sellerSecret = "1zsvmrx9nmmlm";
    private String status = "ACTIVE";

    @Before
    public void setup() {
        NewConversationCommand newConversationCommand = new NewConversationCommand(
                "1", "1",
                buyerId, sellerId, buyerSecret,
                sellerSecret, DateTime.now(), ConversationState.ACTIVE,
                Maps.newHashMap()
        );

        when(conversationRepository.getById(anyString()))
                .thenReturn(DefaultMutableConversation.create(newConversationCommand));

        when(mailCloakingService.createdCloakedMailAddress(eq(ConversationRole.Seller), any(Conversation.class)))
                .thenReturn(new MailAddress(sellerAnonymousEmail));

        when(mailCloakingService.createdCloakedMailAddress(eq(ConversationRole.Buyer), any(Conversation.class)))
                .thenReturn(new MailAddress(buyerAnonymousEmail));
    }

    @Test
    public void shouldEnrichWhenFieldsNotInitialized() throws Exception {
        // given
        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Lists.newArrayList(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        //when
        ConversationThread conversationThreadEnriched = enricher.enrichOnWrite(conversationThread, Optional.empty());

        //then
        assertTrue(conversationThreadEnriched.getBuyerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getSellerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getStatus().isPresent());
    }

    @Test
    public void shouldNotEnrichWhenFieldsAlreadyInitialized() throws Exception {
        // given
        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Lists.newArrayList(),
                Optional.empty(),
                Optional.of(buyerAnonymousEmail),
                Optional.of(sellerAnonymousEmail),
                Optional.of(status));

        //when
        ConversationThread conversationThreadEnriched = enricher.enrichOnWrite(conversationThread, Optional.empty());

        //then
        assertTrue(buyerAnonymousEmail.equals(conversationThreadEnriched.getBuyerAnonymousEmail().get()));
        assertTrue(sellerAnonymousEmail.equals(conversationThreadEnriched.getSellerAnonymousEmail().get()));
        assertTrue(status.equals(conversationThreadEnriched.getStatus().get()));
    }

    @Test
    public void shouldNotCallEnrichForEnrichOnRead() {
        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Lists.newArrayList(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        // when
        ConversationThread conversationThreadEnriched = enricher.enrichOnRead(conversationThread, Optional.empty());

        // then
        assertFalse(conversationThreadEnriched.getBuyerAnonymousEmail().isPresent());
        assertFalse(conversationThreadEnriched.getSellerAnonymousEmail().isPresent());
        assertFalse(conversationThreadEnriched.getStatus().isPresent());
    }

    @Test
    public void shouldCallEnrichWhenEnrichmentPropertyIsOverridden() {
        ReflectionTestUtils.setField(enricher, "shouldEnrichOnRead", true);

        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Lists.newArrayList(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        // when
        ConversationThread conversationThreadEnriched = enricher.enrichOnRead(conversationThread, Optional.empty());

        // then
        assertTrue(conversationThreadEnriched.getBuyerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getSellerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getStatus().isPresent());
    }

    @Test
    public void repositoryLazilyReturnsNullValue() {
        ConversationThread conversationThread = new ConversationThread(
                "2",
                "abc",
                now(),
                now().minusDays(179),
                now(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Lists.newArrayList(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(""),
                Optional.empty());

        when(conversationRepository.getById(anyString())).thenReturn(null);

        // when
        ConversationThread conversationThreadEnriched = enricher.enrichOnWrite(conversationThread, Optional.empty());

        // then
        verify(conversationRepository, times(1)).getById(anyString());
        assertFalse(conversationThreadEnriched.getBuyerAnonymousEmail().isPresent());
        assertTrue(conversationThreadEnriched.getSellerAnonymousEmail().isPresent());
        assertFalse(conversationThreadEnriched.getStatus().isPresent());
    }

    @Configuration
    @Import(ConversationThreadEnricher.class)
    static class TestContext {
        @MockBean
        private MailCloakingService mailCloakingService;

        @MockBean
        private ConversationRepository conversationRepository;
    }
}
