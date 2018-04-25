package com.ecg.messagecenter.gtau.util;

import com.ecg.messagecenter.gtau.persistence.ConversationThread;
import com.ecg.messagecenter.gtau.util.ConversationThreadEnricher;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTime.now;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationThreadEnricherTest {

    private ConversationThreadEnricher conversationThreadEnricher;
    private Conversation conversation;

    @Mock
    private MailCloakingService mailCloakingServiceMock;

    private String buyerAnonymousEmail = "Buyer.3o9sdp1ykrnv@test-platform.com";
    private String sellerAnonymousEmail = "Seller.3vvbtmecyi5ue@test-platform.com";

    @Before
    public void setup() {
        when(mailCloakingServiceMock.createdCloakedMailAddress(eq(ConversationRole.Seller), any(Conversation.class)))
                .thenReturn(new MailAddress(sellerAnonymousEmail));
        when(mailCloakingServiceMock.createdCloakedMailAddress(eq(ConversationRole.Buyer), any(Conversation.class)))
                .thenReturn(new MailAddress(buyerAnonymousEmail));

        conversationThreadEnricher = new ConversationThreadEnricher(mailCloakingServiceMock, false);

        NewConversationCommand newConversationCommand = new NewConversationCommand(
                "1", "1",
                "buyer1@seller.com", "seller1@seller.com", "8qdcsuwwpss6",
                "1zsvmrx9nmmlm", DateTime.now(), ConversationState.ACTIVE,
                Maps.newHashMap()
        );

        conversation = DefaultMutableConversation.create(newConversationCommand);
    }

    @Test
    public void shouldEnrichWhenFieldsNotInitialized() throws Exception {
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

        ConversationThread conversationThreadEnriched = conversationThreadEnricher.enrichOnWrite(conversationThread, conversation);

        assertThat(conversationThreadEnriched.getBuyerAnonymousEmail()).isPresent();
        assertThat(conversationThreadEnriched.getSellerAnonymousEmail()).isPresent();
        assertThat(conversationThreadEnriched.getStatus()).isPresent();
    }

    @Test
    public void shouldNotEnrichWhenFieldsAlreadyInitialized() throws Exception {
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
                Optional.of("ACTIVE"));

        ConversationThread conversationThreadEnriched = conversationThreadEnricher.enrichOnWrite(conversationThread, conversation);

        assertThat(conversationThreadEnriched.getBuyerAnonymousEmail().get()).isEqualToIgnoringCase(buyerAnonymousEmail);
        assertThat(conversationThreadEnriched.getSellerAnonymousEmail().get()).isEqualToIgnoringCase(sellerAnonymousEmail);
        assertThat(conversationThreadEnriched.getStatus().get()).isEqualToIgnoringCase("ACTIVE");
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

        ConversationThread conversationThreadEnriched = conversationThreadEnricher.enrichOnRead(conversationThread, conversation);

        assertThat(conversationThreadEnriched.getBuyerAnonymousEmail()).isNotPresent();
        assertThat(conversationThreadEnriched.getSellerAnonymousEmail()).isNotPresent();
        assertThat(conversationThreadEnriched.getStatus()).isNotPresent();
    }

    @Test
    public void shouldCallEnrichWhenEnrichmentPropertyIsOverridden() {
        conversationThreadEnricher = new ConversationThreadEnricher(mailCloakingServiceMock, true);
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

        ConversationThread conversationThreadEnriched = conversationThreadEnricher.enrichOnRead(conversationThread, conversation);

        assertThat(conversationThreadEnriched.getBuyerAnonymousEmail()).isPresent();
        assertThat(conversationThreadEnriched.getSellerAnonymousEmail()).isPresent();
        assertThat(conversationThreadEnriched.getStatus()).isPresent();
    }
}
