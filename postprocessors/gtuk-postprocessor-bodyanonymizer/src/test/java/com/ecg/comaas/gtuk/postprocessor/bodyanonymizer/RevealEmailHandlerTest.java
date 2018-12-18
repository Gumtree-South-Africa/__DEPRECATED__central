package com.ecg.comaas.gtuk.postprocessor.bodyanonymizer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.net.MediaType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by reweber on 08/10/15
 */
@RunWith(MockitoJUnitRunner.class)
public class RevealEmailHandlerTest {

    private static final String INSERT_TEXT = "Hello there you are known good here's the email address '%s'";
    private static final String BUYER_EMAIL = "buyer@gumtree.com";
    private static final String SELLERGOOD_HEADER = "X-Cust-Sellergood";
    private static final String ACCOUNT_HOLDER_VALUE = "ACCOUNT_HOLDER";
    private static final String CLOAKEMAIL_HEADER = "X-Cust-Emailaddresscloak";
    private static final String CLOAKEMAIL_VALUE = "REVEAL";
    private RevealEmailHandler processor;

    @Mock
    private Conversation conversation;
    @Mock
    private Message message;
    @Mock
    private Mail mail;
    @Mock
    private TypedContent<String> mutableContent;
    @Mock
    private MessageBodyAnonymizerConfig config;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(config.getRevealEmailHeader()).thenReturn(CLOAKEMAIL_HEADER);
        when(config.getRevealEmailValue()).thenReturn(CLOAKEMAIL_VALUE);
        when(config.getSellerKnownGoodHeader()).thenReturn(SELLERGOOD_HEADER);
        when(config.getSellerKnownGoodValue()).thenReturn(ACCOUNT_HOLDER_VALUE);
        when(config.getKnownGoodInsertFooterFormat()).thenReturn(INSERT_TEXT);

        when(conversation.getBuyerId()).thenReturn(BUYER_EMAIL);

        when(mail.getTextParts(anyBoolean())).thenReturn(Collections.singletonList(mutableContent));

        processor = new RevealEmailHandler(config);
    }

    @Test
    public void testSellerAccountHolderAndInitialB2S() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        String newText = String.format(INSERT_TEXT, BUYER_EMAIL);
        verify(mutableContent, times(1)).overrideContent("Some message text\n\n" + newText);
    }

    @Test
    public void testSellerAccountHolderAndNotInitialB2S() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(1);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testSellerAccountHolderAndInitialS2B() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testSellerAccountHolderButNoCloakHeader() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, false);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testBuyerAccountHolderAndInitialB2S() {
        setupMailHeaders("buyergood", ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testSellerKnownGoodButNotAccountHolder() {
        setupMailHeaders(SELLERGOOD_HEADER, "ANOTHER_VALUE", true);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testSellerNotKnownGood() {
        Map<String, String> headers = mock(Map.class);
        when(message.getCaseInsensitiveHeaders()).thenReturn(headers);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.PLAIN_TEXT_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testKnownGoodAndB2SButNoMutableContent() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(0);
        when(mutableContent.getContent()).thenReturn("Some message text");
        when(mutableContent.getMediaType()).thenReturn(MediaType.PLAIN_TEXT_UTF_8);
        when(mutableContent.isMutable()).thenReturn(false);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        verify(mutableContent, never()).overrideContent(anyString());
    }

    @Test
    public void testKnownGoodAndB2SWith2MutableContents() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(0);
        when(mutableContent.getContent()).thenReturn("Some message text");
        when(mutableContent.getMediaType()).thenReturn(MediaType.PLAIN_TEXT_UTF_8);
        when(mutableContent.isMutable()).thenReturn(true);

        TypedContent<String> mutableContent2 = mock(TypedContent.class);
        when(mutableContent2.getContent()).thenReturn("Some message text 2");
        when(mutableContent2.getMediaType()).thenReturn(MediaType.PLAIN_TEXT_UTF_8);
        when(mutableContent2.isMutable()).thenReturn(true);
        when(mail.getTextParts(anyBoolean())).thenReturn(Arrays.asList(mutableContent, mutableContent2));

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        String newText = String.format(INSERT_TEXT, BUYER_EMAIL);
        verify(mutableContent, times(1)).overrideContent("Some message text\n\n" + newText);
        verify(mutableContent2, times(1)).overrideContent("Some message text 2\n\n" + newText);
    }

    @Test
    public void testKnownGoodAndB2SWithHtmlContent() {
        setupMailHeaders(SELLERGOOD_HEADER, ACCOUNT_HOLDER_VALUE, true);
        setupMessageDate(0);
        setupMutableTextContent(MediaType.HTML_UTF_8);

        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        processor.process(conversation, message, mail);

        String newText = String.format(INSERT_TEXT, BUYER_EMAIL);
        verify(mutableContent, times(1)).overrideContent("Some message text<html><br><p>" + newText + "</p></html>");
    }


    private void setupMailHeaders(String knownGoodHeader, String knownGoodValue, boolean revealEmail) {
        Map<String, String> headers = mock(Map.class);
        when(headers.get(knownGoodHeader)).thenReturn(knownGoodValue);

        if (revealEmail) {
            when(headers.get(CLOAKEMAIL_HEADER)).thenReturn(CLOAKEMAIL_VALUE);
            when(headers.containsKey(CLOAKEMAIL_HEADER)).thenReturn(true);
        }

        when(message.getCaseInsensitiveHeaders()).thenReturn(headers);
        when(headers.containsKey(knownGoodHeader)).thenReturn(true);
    }

    private void setupMessageDate(int msToAdd) {
        long time = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        Date convCreateDate = cal.getTime();
        cal.add(Calendar.MILLISECOND, msToAdd);
        Date msgReceiveDate = cal.getTime();

        when(conversation.getCreatedAt()).thenReturn(new DateTime(convCreateDate));
        when(message.getReceivedAt()).thenReturn(new DateTime(msgReceiveDate));
    }

    private void setupMutableTextContent(MediaType mediaType) {
        when(mutableContent.getContent()).thenReturn("Some message text");
        when(mutableContent.getMediaType()).thenReturn(mediaType);
        when(mutableContent.isMutable()).thenReturn(true);
    }
}
