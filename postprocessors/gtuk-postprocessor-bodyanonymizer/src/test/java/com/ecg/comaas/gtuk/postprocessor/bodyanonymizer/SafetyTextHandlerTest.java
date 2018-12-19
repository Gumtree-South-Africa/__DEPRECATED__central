package com.ecg.comaas.gtuk.postprocessor.bodyanonymizer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.net.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
public class SafetyTextHandlerTest {

    private static final String DUMMY_FROM_ADDRESS = "b.anon@mail.gumtree.com";
    private static final String STANDARD_SAFETY_TEXT = "For your safety, email addresses are now masked by Gumtree. Replying to this email (%s) will send your reply via Gumtree for your protection. Find out more: http://gumtree.force.com/Help/articles/General_Information/Anonymised-emails." +
            "\n\n" +
            "Report this reply if you suspect it is spam or fraud: http://gumtree.force.com/Help/knowledgeContact" +
            "\n\n" +
            "______________";
    private static final String SELLERGOOD_SAFETY_TEXT = "We have shared your address with the seller." +
            "\n\n" +
            "Dont like it? Tough bollocks." +
            "\n\n" +
            "________________";

    private SafetyTextHandler processor;

    @Mock private Conversation conversation;
    @Mock private Message sellerSentMessage;
    @Mock private Message sellerBlockedMessage;
    @Mock private Message sellerDelayedMessage;
    @Mock private Message buyerSentMessage;
    @Mock private Mail mail;
    @Mock private TypedContent<String> mutableContent;
    @Mock private MessageBodyAnonymizerConfig config;


    @Before
    public void setup() throws IOException {
        initMocks(this);

        when(config.getSafetyTextFormat()).thenReturn(STANDARD_SAFETY_TEXT);
        when(config.getKnownGoodSellerSafetyTextFormat()).thenReturn(SELLERGOOD_SAFETY_TEXT);
        when(config.getRevealEmailHeader()).thenReturn("X-Cust-Emailaddresscloak");
        when(config.getRevealEmailValue()).thenReturn("REVEAL");
        when(config.getSellerKnownGoodHeader()).thenReturn("X-Cust-Sellergood");
        when(config.getSellerKnownGoodValue()).thenReturn("ACCOUNT_HOLDER");

        processor = new SafetyTextHandler(config);

        when(sellerSentMessage.getState()).thenReturn(MessageState.SENT);
        when(sellerSentMessage.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(sellerBlockedMessage.getState()).thenReturn(MessageState.BLOCKED);
        when(sellerBlockedMessage.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(sellerDelayedMessage.getState()).thenReturn(MessageState.HELD);
        when(sellerDelayedMessage.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(buyerSentMessage.getState()).thenReturn(MessageState.SENT);
        when(buyerSentMessage.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(mail.getFrom()).thenReturn(DUMMY_FROM_ADDRESS);
        when(mail.getTextParts(false)).thenReturn(Arrays.asList(mutableContent));
        when(mutableContent.isMutable()).thenReturn(true);
        when(mutableContent.getContent()).thenReturn("This is some text to comprise the message");
        when(mutableContent.getMediaType()).thenReturn(MediaType.PLAIN_TEXT_UTF_8);
    }

    @Test
    public void testNullMessages() {

        // mock conversation to return null messages
        when(conversation.getMessages()).thenReturn(null);

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());

    }

    @Test
    public void testEmptyMessages() {

        // mock conversation to return empty messages
        when(conversation.getMessages()).thenReturn(Collections.<Message>emptyList());

        // call method under test
        processor.process(conversation, buyerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());

    }

    @Test
    public void testFirstBuyerMessage() {

        // mock conversation to return a single sendable message
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage));

        // call method under test
        processor.process(conversation, buyerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());
    }

    @Test
    public void testFirstSellerMessage() {

        // mock conversation to return the second message as sendable
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage));

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify text parts were inspected and changed correctly
        verify(mail, times(1)).getTextParts(false);
        String insertedString = String.format(config.getSafetyTextFormat(), DUMMY_FROM_ADDRESS);
        verify(mutableContent, times(1)).overrideContent(Matchers.startsWith(insertedString));
    }

    @Test
    public void testSecondSellerMessageFirstSellerMessageDelayed() {

        // mock conversation to return one sent, one delayed and one sendable message
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerDelayedMessage, sellerSentMessage));

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify text parts were inspected and changed correctly
        verify(mail, times(1)).getTextParts(false);
        String insertedString = String.format(config.getSafetyTextFormat(), DUMMY_FROM_ADDRESS);
        verify(mutableContent, times(1)).overrideContent(Matchers.startsWith(insertedString));
    }

    @Test
    public void testSecondSellerMessageFirstSellerMessageBlocked() {

        // mock conversation to return null messages
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerBlockedMessage, sellerSentMessage));

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify text parts were inspected and changed correctly
        verify(mail, times(1)).getTextParts(false);
        String insertedString = String.format(config.getSafetyTextFormat(), DUMMY_FROM_ADDRESS);
        verify(mutableContent, times(1)).overrideContent(Matchers.startsWith(insertedString));

    }

    @Test
    public void testSecondSellerMessageFirstSellerMessageSent() {

        // mock conversation to return null messages
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage, sellerSentMessage));

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());

    }

    @Test
    public void testFirstSellerMessageFirstTextPartImmutable() {

        // mock mail message to return two text parts - one immutable and one mutable
        TypedContent<String> immutableContent = mock(TypedContent.class);
        when(immutableContent.isMutable()).thenReturn(false);
        when(mail.getTextParts(false)).thenReturn(Arrays.asList(immutableContent, mutableContent));

        // mock conversation to return null messages
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage));

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify text parts were inspected
        verify(mail, times(1)).getTextParts(false);

        // verify immutable content was not changed
        verify(immutableContent, never()).overrideContent(Matchers.<String>any());

        // verify mutable content was changed correctly
        String insertedString = String.format(config.getSafetyTextFormat(), DUMMY_FROM_ADDRESS);
        verify(mutableContent, times(1)).overrideContent(Matchers.startsWith(insertedString));

    }

    @Test
    public void testSecondMessageFromBuyerBeforeSellerResponds() {

        // mock conversation to return two buyer messages
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, buyerSentMessage));

        // call method under test
        processor.process(conversation, buyerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());

    }

    @Test
    public void testSecondMessageFromBuyerAfterSellerResponds() {

        // mock conversation to return two buyer messages
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage, buyerSentMessage));

        // call method under test
        processor.process(conversation, buyerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());

    }

    @Test
    public void testSecondMessageFromSeller() {

        // mock conversation to return two buyer messages
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage, sellerSentMessage));

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify that text parts were neither inspected nor modified
        verify(mail, never()).getTextParts(false);
        verify(mutableContent, never()).overrideContent(Matchers.<String>any());

    }


    @Test
    public void testFirstSellerMessageWithHtml() {

        // mock conversation to return the second message as sendable
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage));
        when(mutableContent.getMediaType()).thenReturn(MediaType.HTML_UTF_8);
        when(mutableContent.getContent()).thenReturn("<html><body>This is some text to comprise the email</body></html>");

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify text parts were inspected and changed correctly
        verify(mail, times(1)).getTextParts(false);
        String insertedString = String.format(config.getSafetyTextFormat(), DUMMY_FROM_ADDRESS);
        verify(mutableContent, times(1)).overrideContent(Matchers.contains(insertedString.replaceAll("\n", "<br>")));
    }

    @Test
    public void testFirstSellerMessageWhenProSeller() {
        Map<String,String> headers = new HashMap();
        headers.put("X-Cust-Sellergood", "ACCOUNT_HOLDER");
        headers.put("X-Cust-Emailaddresscloak", "REVEAL");
        when(sellerSentMessage.getCaseInsensitiveHeaders()).thenReturn(headers);
        // mock conversation to return the second message as sendable
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage));
        when(mutableContent.getMediaType()).thenReturn(MediaType.HTML_UTF_8);
        when(mutableContent.getContent()).thenReturn("<html><body>This is some text to comprise the email</body></html>");

        // call method under test
        processor.process(conversation, sellerSentMessage, mail);

        // verify text parts were inspected and changed correctly
        verify(mail, times(1)).getTextParts(false);
        String insertedString = String.format(config.getKnownGoodSellerSafetyTextFormat(), DUMMY_FROM_ADDRESS);
        verify(mutableContent, times(1)).overrideContent(Matchers.contains(insertedString.replaceAll("\n", "<br>")));

    }

}
