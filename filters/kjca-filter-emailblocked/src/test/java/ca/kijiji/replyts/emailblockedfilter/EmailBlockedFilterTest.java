package ca.kijiji.replyts.emailblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailBlockedFilterTest {

    private static final String BUYER_EMAIL = "buyer@kijiji.ca";
    private static final String BUYER_SECRET = "b.secret@rts.kijiji.ca";
    private static final String SELLER_EMAIL = "seller@kijiji.ca";
    private static final String SELLER_SECRET = "s.secret@rts.kijiji.ca";
    private static final int SCORE = 100;

    private EmailBlockedFilter objectUnderTest;

    private MessageProcessingContext messageContext;

    @Mock
    private TnsApiClient tnsApiClientMock;

    @Mock
    private Mail mailMock;

    @Before
    public void setUp() {
        objectUnderTest = new EmailBlockedFilter(SCORE, tnsApiClientMock);
    }

    @Test
    public void whenNoContext_shouldReturnEmptyFeedback() {
        List<FilterFeedback> actualFeedback = objectUnderTest.filter(null);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenMessageDirectionUnknown_shouldReturnEmptyFeedback() {
        packMessageContext(aMessage(), aConversation(), MessageDirection.UNKNOWN);

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContext);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenNoBuyerEmail_shouldReturnEmptyFeedback() {
        packMessageContext(aMessage(), aConversation("", SELLER_EMAIL));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContext);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenNoSellerEmail_shouldReturnEmptyFeedback() {
        packMessageContext(aMessage(), aConversation(BUYER_EMAIL, ""));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContext);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenTnsApiReturnsFalseForBuyer_shouldReturnFeedbackOnlyForSeller() {
        when(tnsApiClientMock.getJsonAsMap(contains(BUYER_EMAIL))).thenReturn(Collections.singletonMap(EmailBlockedFilter.IS_BLOCKED_KEY, Boolean.FALSE));
        when(tnsApiClientMock.getJsonAsMap(contains(SELLER_EMAIL))).thenReturn(Collections.singletonMap(EmailBlockedFilter.IS_BLOCKED_KEY, Boolean.TRUE));
        packMessageContext(aMessage(), aConversation(BUYER_EMAIL, SELLER_EMAIL));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContext);

        assertThat(actualFeedback).containsExactly(
                new FilterFeedback("seller email is blocked", "Seller email is blocked", SCORE, FilterResultState.DROPPED)
        );
    }

    @Test
    public void whenTnsApiReturnsFalseForSeller_shouldReturnFeedbackOnlyForBuyer() {
        when(tnsApiClientMock.getJsonAsMap(contains(BUYER_EMAIL))).thenReturn(Collections.singletonMap(EmailBlockedFilter.IS_BLOCKED_KEY, Boolean.TRUE));
        when(tnsApiClientMock.getJsonAsMap(contains(SELLER_EMAIL))).thenReturn(Collections.singletonMap(EmailBlockedFilter.IS_BLOCKED_KEY, Boolean.FALSE));
        packMessageContext(aMessage(), aConversation(BUYER_EMAIL, SELLER_EMAIL));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContext);

        assertThat(actualFeedback).containsExactly(
                new FilterFeedback("buyer email is blocked", "Buyer email is blocked", SCORE, FilterResultState.DROPPED)
        );
    }

    @Test
    public void whenBothTnsApiResultsTrue_shouldReturnProperFeedback() {
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(Collections.singletonMap(EmailBlockedFilter.IS_BLOCKED_KEY, Boolean.TRUE));
        packMessageContext(aMessage(), aConversation(BUYER_EMAIL, SELLER_EMAIL));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContext);

        assertThat(actualFeedback).containsExactly(
                new FilterFeedback("buyer email is blocked", "Buyer email is blocked", SCORE, FilterResultState.DROPPED),
                new FilterFeedback("seller email is blocked", "Seller email is blocked", SCORE, FilterResultState.DROPPED)
        );
    }

    private static AddMessageCommand aMessage() {
        return AddMessageCommandBuilder
                .anAddMessageCommand("cid", "mid")
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(DateTime.now())
                .withHeaders(ImmutableMap.of())
                .withTextParts(Collections.singletonList(""))
                .build();
    }

    private static DefaultMutableConversation aConversation() {
        return aConversation(BUYER_EMAIL, SELLER_EMAIL);
    }

    private static DefaultMutableConversation aConversation(String buyerEmail, String sellerEmail) {
        NewConversationCommandBuilder conversationBuilder = NewConversationCommandBuilder
                .aNewConversationCommand("cid")
                .withState(ConversationState.ACTIVE)
                .withBuyer(buyerEmail, BUYER_SECRET)
                .withSeller(sellerEmail, SELLER_SECRET);
        return DefaultMutableConversation.create(conversationBuilder.build());
    }

    private void packMessageContext(AddMessageCommand addMessageCommand, DefaultMutableConversation conversation) {
        packMessageContext(addMessageCommand, conversation, MessageDirection.BUYER_TO_SELLER);
    }

    private void packMessageContext(AddMessageCommand addMessageCommand, DefaultMutableConversation conversation, MessageDirection messageDirection) {
        conversation.applyCommand(addMessageCommand);
        messageContext = new MessageProcessingContext(mailMock, "msgId", new ProcessingTimeGuard(0));
        messageContext.setConversation(conversation);
        messageContext.setMessageDirection(messageDirection);
    }
}
