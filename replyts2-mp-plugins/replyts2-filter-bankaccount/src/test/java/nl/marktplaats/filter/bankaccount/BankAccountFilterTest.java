package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import com.google.common.net.MediaType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BankAccountFilterTest {
    private static final String PLAIN_TEXT_CONTENT_FRAUDULENT =
            "Message with a fraudulent bank account: 123456. Good luck!";
    private static final String HTML_CONTENT_FRAUDULENT =
            "Message with a fraudulent bank account: <b>123456</b>. Good luck!";
    private static final String HTML_CONTENT_FRAUDULENT_SPANS =
            "Message with a fraudulent bank account: <b>12<span></span>34<span></span>56</b>. Good luck!";

    private static final List<String> FRAUDULENT_BANK_ACCOUNTS = Arrays.asList(
            "2593139", "757706428", "123456", "NL84INGB0002930139", "NO1225673786578");

    private BankAccountFilter filter;
    private BankAccountFilterConfiguration config;
    private BankAccountFinder bankAccountFinder;

    @Mock private Message message;
    @Mock private Conversation conversation;
    @Mock private Mail mail;
    @Mock private TypedContent<String> mutablePlainContent;
    @Mock private TypedContent<String> mutableHtmlContent;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MailCloakingService mailCloakingService;
    @Mock private MailRepository mailRepository;
    @Mock private Mails mailsParser;


    private String fromUserId = "123";
    private String toUserId = "456";

    @Before
    public void setup() throws IOException, PersistenceException {
        initMocks(this);

        when(mail.getTextParts(false)).thenReturn(asTypedContentList(mutablePlainContent, mutableHtmlContent));
        when(mutablePlainContent.getMediaType()).thenReturn(MediaType.create("text", "plain"));
        when(mutablePlainContent.isMutable()).thenReturn(true);
        when(mutableHtmlContent.getMediaType()).thenReturn(MediaType.create("text", "html"));
        when(mutableHtmlContent.isMutable()).thenReturn(true);

        config = new BankAccountFilterConfiguration(FRAUDULENT_BANK_ACCOUNTS);
        bankAccountFinder = new BankAccountFinder(config);
        filter = new BankAccountFilter(bankAccountFinder, mailCloakingService, mailRepository, mailsParser);
    }

    private Conversation prepareSimpleSingleMailWithTexts(String adId, String subject, String plainTextPart, String htmlPart) {
        when(mail.getSubject()).thenReturn(subject);
        when(mutablePlainContent.getContent()).thenReturn(plainTextPart);
        when(mutableHtmlContent.getContent()).thenReturn(htmlPart);

        when(conversation.getAdId()).thenReturn(adId);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        return conversation;
    }

    private MessageProcessingContext prepareMessageProcessingContext(Conversation conversation, Message message, Mail mail) {
        MessageProcessingContext messageProcessingContext = mock(MessageProcessingContext.class);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(messageProcessingContext.getMail()).thenReturn(mail);
        when(messageProcessingContext.getMessage()).thenReturn(message);
        return messageProcessingContext;
    }

    @Test
    public void matchExactly() {
        prepareSimpleSingleMailWithTexts("m369147", "123456", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void doNotFailOnNullSubject() {
        prepareSimpleSingleMailWithTexts("m369147", null, "", "123456");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchExactlyRemovingDuplicates() {
        prepareSimpleSingleMailWithTexts("m369147", "123456", "123456", "123456");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchExactlyIbanKnownCountry() {
        prepareSimpleSingleMailWithTexts("m369147", "NL84INGB0002930139", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("NL84INGB0002930139", "NL84INGB0002930139", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("NL84INGB0002930139", "2930139", 0, FilterResultState.OK));
    }

    @Test
    public void matchExactlyIbanOfUnknownCountry() {
        prepareSimpleSingleMailWithTexts("m369147", "NO1225673786578", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("NO1225673786578", "NO1225673786578", 100, FilterResultState.OK));
    }

    @Test
    public void matchWithSeparatorsIbanKnownCountry() {
        prepareSimpleSingleMailWithTexts("m369147", "NL84 INGB000 2930 139", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("NL84INGB0002930139", "NL84 INGB000 2930 139", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("NL84INGB0002930139", "2930 139", 0, FilterResultState.OK));
    }

    @Test
    public void matchWithSeparatorsIbanOfUnknownCountry() {
        prepareSimpleSingleMailWithTexts("m369147", "NO 12 25 67 37 86 57 8", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("NO1225673786578", "NO 12 25 67 37 86 57 8", 100, FilterResultState.OK));
    }

    @Test
    public void matchWithSeparators() {
        prepareSimpleSingleMailWithTexts("m369147", "123456", "12 34 56", "1 23 45 6");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(3));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("123456", "12 34 56", 0, FilterResultState.OK));
        assertThat(filterFeedbacks.get(2), matchesAccount("123456", "1 23 45 6", 0, FilterResultState.OK));
    }

    @Test
    public void matchExactlySubjectOnly() {
        prepareSimpleSingleMailWithTexts("m369147", "Re: please use 123456", "Nothing to see here.", "<p>Nothing to see here.</p>");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchExactlyPlainTextMailOnly() {
        prepareSimpleSingleMailWithTexts("m369147", "subject", PLAIN_TEXT_CONTENT_FRAUDULENT, "<p>Nothing to see here.</p>");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchExactlyHtmlMailOnly() {
        prepareSimpleSingleMailWithTexts("m369147", "subject", "Nothing to see here.", HTML_CONTENT_FRAUDULENT);

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchSpannedHtmlMailOnly() {
        prepareSimpleSingleMailWithTexts("m369147", "subject", "Nothing to see here.", HTML_CONTENT_FRAUDULENT_SPANS);

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchEscapedHtmlMailOnly() {
        prepareSimpleSingleMailWithTexts("m369147", "subject", "Nothing to see here.", "Message with a fraudulent bank account: 1&#50;3&#x34;56</b>. Good luck!");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
    }

    @Test
    public void matchMultipleBankAccounts() {
        prepareSimpleSingleMailWithTexts("m369147", "subject", "123456", "NL84INGB0002930139");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
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
        prepareSimpleSingleMailWithTexts("m369147", "subject", "Maak aub geld over aan rekening 12 34 56 94 3, of naar rekening 12.34.56.94.3.", "Nothing to see here");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertTrue(filterFeedbacks.size() > 1);
        int totalScore = 0;
        for (FilterFeedback filterFeedback : filterFeedbacks) {
            totalScore += filterFeedback.getScore();
        }
        assertThat(totalScore, is(50));
    }

    @Test
    public void usesConfiguredHighScore() {
        config = new BankAccountFilterConfiguration(FRAUDULENT_BANK_ACCOUNTS, 80, 0, 0);
        bankAccountFinder = new BankAccountFinder(config);
        filter = new BankAccountFilter(bankAccountFinder, mailCloakingService, mailRepository, mailsParser);

        prepareSimpleSingleMailWithTexts("m369147", "subject", "123456", "NL84INGB0002930139");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertThat(filterFeedbacks.get(0).getScore(), is(80));
    }

    @Test
    public void usesConfiguredLowScore() {
        config = new BankAccountFilterConfiguration(FRAUDULENT_BANK_ACCOUNTS, 100, 30, 0);
        bankAccountFinder = new BankAccountFinder(config);
        filter = new BankAccountFilter(bankAccountFinder, mailCloakingService, mailRepository, mailsParser);

        prepareSimpleSingleMailWithTexts("m369147", "subject", "1234568675", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertThat(filterFeedbacks.get(0).getScore(), is(30));
    }

    /*
    @Test
    public void reasonContainsEvaluationFlag() {
        config.setStatus(FilterStatus.EVALUATION);

        prepareSimpleSingleMailWithTexts("m369147", "subject", "123456", "NL84INGB0002930139");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertThat(filterFeedbacks.get(0).isEvaluation(), is(true));
    }*/

    @Test
    public void descriptionContainsAllReportHeaders() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        Map<String, String> customValues = new HashMap<>();
        customValues.put("from-userid", fromUserId);
        customValues.put("to-userid", toUserId);
        when(conversation.getCustomValues()).thenReturn(customValues);

        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getId()).thenReturn("987654");

        when(mail.getFrom()).thenReturn("f.r.audster@mail.com");
        when(mail.getUniqueHeader("X-Originating-IP")).thenReturn("10.1.2.3");

        MailAddress anonymousFraudsterAddress = new MailAddress("fraudster-anon@mail.marktplaats.nl");
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).
                thenReturn(anonymousFraudsterAddress);

        prepareSimpleSingleMailWithTexts("m369147", "123456", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        FilterFeedback reason = filterFeedbacks.get(0);
        assertThat(reason.getDescription(), is(
                "fraudster@mail.com|" +
                        "100|" +
                        "123456|" +
                        "fraudster-anon@mail.marktplaats.nl|" +
                        "f.r.audster@mail.com|" +
                        "10.1.2.3|" +
                        "victim@mail.com|" +
                        "987654|" +
                        "1|"+fromUserId+"|"+toUserId));
    }

    @Test
    public void descriptionContainsSimplifiedIpAddress() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getId()).thenReturn("987654");

        when(mail.getFrom()).thenReturn("f.r.audster@mail.com");
        when(mail.getUniqueHeader("X-Originating-IP")).thenReturn("[10.1.2.3]");

        MailAddress anonymousFraudsterAddress = new MailAddress("fraudster-anon@mail.marktplaats.nl");
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).
                thenReturn(anonymousFraudsterAddress);

        prepareSimpleSingleMailWithTexts("m369147", "123456", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        FilterFeedback filterFeedback = filterFeedbacks.get(0);
        assertThat(filterFeedback.getDescription(), containsString("|10.1.2.3|"));
    }

    @Test
    public void descriptionSkipsAlternativeFraudsterMailWhenItIsSameAsPrimaryMail() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        Map<String, String> customValues = new HashMap<>();
        customValues.put("from-userid", fromUserId);
        customValues.put("to-userid", toUserId);
        when(conversation.getCustomValues()).thenReturn(customValues);

        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getId()).thenReturn("987654");

        when(mail.getFrom()).thenReturn("fraudster@mail.com");

        MailAddress anonymousFraudsterAddress = new MailAddress("fraudster-anon@mail.marktplaats.nl");
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).
                thenReturn(anonymousFraudsterAddress);

        prepareSimpleSingleMailWithTexts("m369147", "123456", "", "");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        FilterFeedback filterFeedback = filterFeedbacks.get(0);
        assertThat(filterFeedback.getDescription(), is(
                "fraudster@mail.com|" +
                        "100|" +
                        "123456|" +
                        "fraudster-anon@mail.marktplaats.nl|" +
                        "|" +
                        "|" +
                        "victim@mail.com|" +
                        "987654|" +
                        "1|"+fromUserId+"|"+toUserId));
    }

    @Test
    public void descriptionInFilterReasonContainsDataForOlderMessageWithFoundBankAccountNumber() throws Exception {
        // Create the mails
        final List<Mail> mails = new ArrayList<>(6);
        mails.add(createMockMail(0, "f.r.audster@mail.com", "innocent"));
        mails.add(createMockMail(1, "thevictim@mail.com", "innocent"));
        mails.add(createMockMail(2, "f.r.audster@mail.com", "give me money! 123.456"));
        mails.add(createMockMail(3, "thevictim@mail.com", "it is transferred to 123456\n> give me money! 123456"));
        mails.add(createMockMail(4, "f.r.audster@mail.com", "give me more money!"));
        mails.add(createMockMail(5, "thevictim@mail.com", "where is my product?\n> give me more money! 123 456"));

        // Create the messages
        List<Message> messages = new ArrayList<>(6);
        messages.add(crateMockMessage(0, MessageDirection.BUYER_TO_SELLER));
        messages.add(crateMockMessage(1, MessageDirection.SELLER_TO_BUYER));
        messages.add(crateMockMessage(2, MessageDirection.BUYER_TO_SELLER));
        messages.add(crateMockMessage(3, MessageDirection.SELLER_TO_BUYER));
        messages.add(crateMockMessage(4, MessageDirection.BUYER_TO_SELLER));
        messages.add(crateMockMessage(5, MessageDirection.SELLER_TO_BUYER));


        Map<String, String> customValues = new HashMap<>();
        customValues.put("from-userid", fromUserId);
        customValues.put("to-userid", toUserId);
        when(conversation.getCustomValues()).thenReturn(customValues);

        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getId()).thenReturn("987654");
        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getId()).thenReturn("987654");

        when(conversation.getMessages()).thenReturn(messages);

        MailAddress anonymousFraudsterAddress = new MailAddress("fraudster-anon@mail.marktplaats.nl");
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).
                thenReturn(anonymousFraudsterAddress);

        MailAddress anonymousVictimAddress = new MailAddress("victim-anon@mail.marktplaats.nl");
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).
                thenReturn(anonymousVictimAddress);

        mockDfsAndParser(mails);

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, messages.get(5), mails.get(5));
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        FilterFeedback filterFeedback = filterFeedbacks.get(0);
        assertThat(filterFeedback.getDescription(), is(
                "fraudster@mail.com|" +
                        "100|" +
                        "123456|" +
                        "fraudster-anon@mail.marktplaats.nl|" +
                        "f.r.audster@mail.com|" +
                        "|" +
                        "victim@mail.com|" +
                        "987654|" +
                        "4|"+fromUserId+"|"+toUserId));
    }

    @Test
    public void doNotFailOnNullAdId() {
        prepareSimpleSingleMailWithTexts(null, null, "", "123456");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        this.filter.filter(messageProcessingContext);
    }

    @Test
    public void findAdIdMatch() {
        prepareSimpleSingleMailWithTexts("m757706428", null, "", "the ad is: 757706428");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(1));
        assertThat(filterFeedbacks.get(0), matchesAccount("757706428", "757706428", 20, FilterResultState.OK));
    }

    @Test
    public void removeScoreOnAdIdMatchWhenHigherMatchIsPresent() {
        prepareSimpleSingleMailWithTexts("m757706428", null, "", "pay me via 123456, the ad is: 757706428");

        MessageProcessingContext messageProcessingContext = prepareMessageProcessingContext(conversation, message, mail);
        List<FilterFeedback> filterFeedbacks = this.filter.filter(messageProcessingContext);

        assertNotNull(filterFeedbacks);
        assertThat(filterFeedbacks.size(), is(2));
        assertThat(filterFeedbacks.get(0), matchesAccount("123456", "123456", 100, FilterResultState.OK));
        assertThat(filterFeedbacks.get(1), matchesAccount("757706428", "757706428", 0, FilterResultState.OK));
    }

    private Mail createMockMail(int index, String replyTo, String plain) {
        Mail mail = mock(Mail.class, "mail_" + index);
        when(mail.getSubject()).thenReturn("mail " + index);
        when(mail.getReplyTo()).thenReturn(replyTo);
        when(mail.getCustomHeaders()).thenReturn(Collections.<String, String>emptyMap());

        // plain part
        TypedContent<String> mutablePlainContent = mock(TypedContent.class);
        when(mutablePlainContent.getContent()).thenReturn(plain);
        when(mutablePlainContent.getMediaType()).thenReturn(MediaType.create("text", "plain"));
        when(mutablePlainContent.isMutable()).thenReturn(true);

        // html part
        TypedContent<String> mutableHtmlContent = mock(TypedContent.class);
        when(mutableHtmlContent.getContent()).thenReturn("<html>" + plain + "</html>");
        when(mutableHtmlContent.getMediaType()).thenReturn(MediaType.create("text", "html"));
        when(mutableHtmlContent.isMutable()).thenReturn(true);

        when(mail.getTextParts(false)).thenReturn(asTypedContentList(mutablePlainContent, mutableHtmlContent));

        return mail;
    }

    private Message crateMockMessage(int index, MessageDirection direction) {
        Message message = mock(Message.class, "message_" + index);
        when(message.getMessageDirection()).thenReturn(direction);
        when(message.getId()).thenReturn("/messages/" + index);
        return message;
    }

    private void mockDfsAndParser(final List<Mail> mails) throws ParsingException {
        when(mailRepository.readInboundMail(anyString())).thenAnswer(invocation ->
                        ((String) invocation.getArguments()[0]).getBytes()
        );

        when(mailsParser.readMail(any(byte[].class))).thenAnswer(invocation -> {
            String name = new String((byte[]) invocation.getArguments()[0]);
            int mailIndex = Integer.parseInt(name.substring("/messages/".length()));
            return mails.get(mailIndex);
        });
    }

    // Circumvents problems with generics in arrays
    @SuppressWarnings({"unchecked"})
    private List<TypedContent<String>> asTypedContentList(TypedContent<String> c1, TypedContent<String> c2) {
        return Arrays.asList(c1, c2);
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