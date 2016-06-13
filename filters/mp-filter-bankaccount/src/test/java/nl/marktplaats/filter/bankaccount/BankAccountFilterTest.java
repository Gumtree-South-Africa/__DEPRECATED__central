package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BankAccountFilterTest {

    private static final String PLAIN_TEXT_CONTENT_FRAUDULENT = "Message with a fraudulent bank account: 123456. Good luck!";
    private static final String AD_ID = "m369147";
    private static final String fromUserId = "123";
    private static final String toUserId = "456";

    @Mock private BankAccountFinder bankAccountFinder;
    @Mock private DescriptionBuilder descriptionBuilder;
    @Mock private Conversation conversation;
    @Mock private MessageProcessingContext messageProcessingContext;

    private BankAccountFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        filter = new BankAccountFilter(bankAccountFinder, descriptionBuilder);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
    }

    private static Message createMockMessage(String index, MessageDirection direction, String subject, String plainTextPart) {
        Message message = mock(Message.class, "message_" + index);
        when(message.getId()).thenReturn("msg" + index);
        when(message.getMessageDirection()).thenReturn(direction);
        when(message.getTextParts()).thenReturn(Arrays.asList(plainTextPart));
        when(message.getHeaders()).thenReturn(new HashMap() {{ put("Subject", subject); }} );
        return message;
    }

    @Test
    public void matchExactly() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "123456", "");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("123456"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", asList("123456"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void doNotFailOnNullSubject() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, null, "123456");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("123456"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", asList("123456"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchExactlyIbanKnownCountry() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "NL84INGB0002930139", "");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("NL84INGB0002930139", "NL84INGB0002930139", 100);
        BankAccountMatch bankAccountMatch1 = new BankAccountMatch("NL84INGB0002930139", "2930139", 0);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("NL84INGB0002930139"), AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(bankAccountFinder.containsSingleBankAccountNumber("NL84INGB0002930139", asList("NL84INGB0002930139"), AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|NL84INGB0002930139|");
        when(descriptionBuilder.build(conversation, bankAccountMatch1, message, 1)).thenReturn("|NL84INGB0002930139|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("NL84INGB0002930139", "NL84INGB0002930139", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("NL84INGB0002930139", "2930139", 0, FilterResultState.OK));
    }

    @Test
    public void matchExactlyIbanOfUnknownCountry() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "NO1225673786578", "");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("NO1225673786578", "NO1225673786578", 100);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("NO1225673786578"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("NO1225673786578", asList("NO1225673786578"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|NO1225673786578|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("NO1225673786578", "NO1225673786578", 100, FilterResultState.OK));
    }

    @Test
    public void matchWithSeparatorsIbanKnownCountry() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "NL84 INGB000 2930 139", "");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("NL84INGB0002930139", "NL84 INGB000 2930 139", 100);
        BankAccountMatch bankAccountMatch1 = new BankAccountMatch("NL84INGB0002930139", "2930 139", 0);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("NL84 INGB000 2930 139"), AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(bankAccountFinder.containsSingleBankAccountNumber("NL84INGB0002930139", asList("NL84 INGB000 2930 139"), AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|NL84INGB0002930139|");
        when(descriptionBuilder.build(conversation, bankAccountMatch1, message, 1)).thenReturn("|NL84INGB0002930139|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("NL84INGB0002930139", "NL84 INGB000 2930 139", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("NL84INGB0002930139", "2930 139", 0, FilterResultState.OK));
    }

    @Test
    public void matchWithSeparatorsIbanOfUnknownCountry() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "NO 12 25 67 37 86 57 8", "");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("NO1225673786578", "NO 12 25 67 37 86 57 8", 100);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("NO 12 25 67 37 86 57 8"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("NO1225673786578", asList("NO 12 25 67 37 86 57 8"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|NO1225673786578|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("NO1225673786578", "NO 12 25 67 37 86 57 8", 100, FilterResultState.OK));
    }

    @Test
    public void matchWithSeparators() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "12 34 56", "1 23 45 6");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "12 34 56", 0);
        BankAccountMatch bankAccountMatch1 = new BankAccountMatch("123456", "1 23 45 6", 0);
        List<String> textParts = asList("1 23 45 6", "12 34 56");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");
        when(descriptionBuilder.build(conversation, bankAccountMatch1, message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "12 34 56", 0, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("123456", "1 23 45 6", 0, FilterResultState.OK));
    }

    @Test
    public void matchExactlySubjectOnly() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "Re: please use 123456", "Nothing to see here.");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        List<String> textParts = asList("Nothing to see here.", "Re: please use 123456");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchExactlyPlainTextMailOnly() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "subject", PLAIN_TEXT_CONTENT_FRAUDULENT);
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        List<String> textParts = asList(PLAIN_TEXT_CONTENT_FRAUDULENT, "subject");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchEscapedHtmlMailOnly() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "subject", "Message with a fraudulent bank account: 1&#50;3&#x34;56</b>. Good luck!");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        List<String> textParts = asList("Message with a fraudulent bank account: 1&#50;3&#x34;56</b>. Good luck!", "subject");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchMultipleBankAccounts() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "123456", "NL84INGB0002930139");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        BankAccountMatch bankAccountMatch1 = new BankAccountMatch("NL84INGB0002930139", "NL84INGB0002930139", 100);
        BankAccountMatch bankAccountMatch2 = new BankAccountMatch("NL84INGB0002930139", "2930139", 10);
        List<String> textParts = asList("NL84INGB0002930139", "123456");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1, bankAccountMatch2));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("NL84INGB0002930139", textParts, AD_ID)).thenReturn(asList(bankAccountMatch1, bankAccountMatch2));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");
        when(descriptionBuilder.build(conversation, bankAccountMatch1.withZeroScore(), message, 1)).thenReturn("|NL84INGB0002930139|");
        when(descriptionBuilder.build(conversation, bankAccountMatch2.withZeroScore(), message, 1)).thenReturn("|NL84INGB0002930139|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(3));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("NL84INGB0002930139", "NL84INGB0002930139", 0, FilterResultState.OK));
        // Score for next one should maybe be 50.
        assertThat(filterFeedbacks.get(2), matchesAccount("NL84INGB0002930139", "2930139", 0, FilterResultState.OK));
    }

    @Test
    public void onlyLowCertaintyMatchesGiveHoldScore() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, "subject", "Maak aub geld over aan rekening 12 34 56 94 3, of naar rekening 12.34.56.94.3.");
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "12 34 56", 50);
        BankAccountMatch bankAccountMatch1 = new BankAccountMatch("123456", "12.34.56", 50);
        List<String> textParts = asList("Maak aub geld over aan rekening 12 34 56 94 3, of naar rekening 12.34.56.94.3.", "subject");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");
        when(descriptionBuilder.build(conversation, bankAccountMatch1.withZeroScore(), message, 1)).thenReturn("|123456|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertTrue(filterFeedbacks.size() > 1);
        int totalScore = 0;
        for (FilterFeedback filterFeedback : filterFeedbacks) {
            totalScore += filterFeedback.getScore();
        }
        assertThat(totalScore, is(50));
    }

    @Test
    public void descriptionInFilterReasonContainsDataForOlderMessageWithFoundBankAccountNumber() throws Exception {
        List<Message> messages = new ArrayList<>(6);
        messages.add(createMockMessage("0", MessageDirection.BUYER_TO_SELLER, "", ""));
        messages.add(createMockMessage("1", MessageDirection.SELLER_TO_BUYER, "", "123456"));
        messages.add(createMockMessage("2", MessageDirection.BUYER_TO_SELLER, "", ""));
        messages.add(createMockMessage("3", MessageDirection.SELLER_TO_BUYER, "", ""));
        messages.add(createMockMessage("4", MessageDirection.BUYER_TO_SELLER, "", ""));
        Message message = createMockMessage("5", MessageDirection.SELLER_TO_BUYER, "subject", "where is my product?\n> give me more money! 123 456");
        messages.add(message);

        Map<String, String> customValues = new HashMap<>();
        customValues.put("from-userid", fromUserId);
        customValues.put("to-userid", toUserId);
        when(conversation.getCustomValues()).thenReturn(customValues);
        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getAdId()).thenReturn(AD_ID);
        when(conversation.getMessages()).thenReturn(messages);
        when(messageProcessingContext.getMessage()).thenReturn(messages.get(5));
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123 456", 50);
        List<String> textParts = asList("where is my product?\n> give me more money! 123 456", "subject");
        when(bankAccountFinder.findBankAccountNumberMatches(textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", textParts, AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("fraudster@mail.com|100|123456|fraudster-anon@mail.marktplaats.nl|f.r.audster@mail.com||victim@mail.com|"+AD_ID+"|4|"+fromUserId+"|"+toUserId);

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertThat(filterFeedbacks.size(), is(1));
        FilterFeedback filterFeedback = filterFeedbacks.get(0);
        assertThat(filterFeedback.getDescription(), is(
            "fraudster@mail.com|100|123456|fraudster-anon@mail.marktplaats.nl|f.r.audster@mail.com||victim@mail.com|"+AD_ID+"|4|"+fromUserId+"|"+toUserId));
    }

    @Test
    public void doNotFailOnNullAdId() {
        when(conversation.getAdId()).thenReturn(null);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, null, "123456");
        when(messageProcessingContext.getMessage()).thenReturn(message);

        this.filter.filter(messageProcessingContext);
    }

    @Test
    public void findAdIdMatch() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, null, "the ad is: 757706428");
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        when(messageProcessingContext.getMessage()).thenReturn(message);
        BankAccountMatch bankAccountMatch = new BankAccountMatch("757706428", "757706428", 20);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("the ad is: 757706428"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("757706428", asList("the ad is: 757706428"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|757706428|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("757706428", "757706428", 20, FilterResultState.OK));
    }

    @Test
    public void removeScoreOnAdIdMatchWhenHigherMatchIsPresent() {
        when(conversation.getAdId()).thenReturn(AD_ID);
        Message message = createMockMessage("1", MessageDirection.BUYER_TO_SELLER, null, "pay me via 123456, the ad is: 757706428");
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        when(messageProcessingContext.getMessage()).thenReturn(message);
        BankAccountMatch bankAccountMatch = new BankAccountMatch("123456", "123456", 100);
        BankAccountMatch bankAccountMatch1 = new BankAccountMatch("757706428", "757706428", 20);
        when(bankAccountFinder.findBankAccountNumberMatches(asList("pay me via 123456, the ad is: 757706428"), AD_ID)).thenReturn(asList(bankAccountMatch, bankAccountMatch1));
        when(bankAccountFinder.containsSingleBankAccountNumber("123456", asList("pay me via 123456, the ad is: 757706428"), AD_ID)).thenReturn(asList(bankAccountMatch));
        when(bankAccountFinder.containsSingleBankAccountNumber("757706428", asList("pay me via 123456, the ad is: 757706428"), AD_ID)).thenReturn(asList(bankAccountMatch1));
        when(descriptionBuilder.build(conversation, bankAccountMatch, message, 1)).thenReturn("|123456|");
        when(descriptionBuilder.build(conversation, bankAccountMatch1.withZeroScore(), message, 1)).thenReturn("|757706428|");

        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("757706428", "757706428", 0, FilterResultState.OK));
    }

    private static class BankAccountFilterMatcher extends TypeSafeMatcher<FilterFeedback> {
        private String expectedBankAccountNumber;
        private String expectedHint;
        private int expectedScore;
        private FilterResultState expectedResultState;

        public BankAccountFilterMatcher(String expectedBankAccountNumber, String expectedHint, int expectedScore, FilterResultState expectedResultState) {
            super(FilterFeedback.class);
            this.expectedBankAccountNumber = expectedBankAccountNumber;
            this.expectedHint = expectedHint;
            this.expectedScore = expectedScore;
            this.expectedResultState = expectedResultState;
        }

        @Override
        public boolean matchesSafely(FilterFeedback item) {
            return item.getDescription().contains("|" + expectedBankAccountNumber + "|") &&
                    item.getUiHint().equals(expectedHint) &&
                    (item.getScore() == expectedScore) &&
                    (item.getResultState() == expectedResultState);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("a reason with account %s, hint %s, score %d, eval %s", expectedBankAccountNumber, expectedHint, expectedScore, expectedResultState));
        }
    }

    private BankAccountFilterMatcher matchesAccount(String expectedBankAccountNumber, String expectedHint, int expectedScore, FilterResultState expectedResultState) {
        return new BankAccountFilterMatcher(expectedBankAccountNumber, expectedHint, expectedScore, expectedResultState);
    }
}