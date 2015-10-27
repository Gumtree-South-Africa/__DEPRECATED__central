package nl.marktplaats.postprocessor.anonymizebody.safetymessage;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import com.google.common.net.MediaType;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.SafetyTextInsertion;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.HtmlMailPartInsertion;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.PlainTextMailPartInsertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by reweber on 19/10/15
 */
public class SafetyMessagePostProcessorTest {
    private static final String DUMMY_FROM_ADDRESS = "b.anon@mail.gumtree.com";

    private static final String PART_CONTENT = "This is some text to comprise the message";

    private static final List<String> SELLER_SAFETY_TEXTS = Arrays.asList("", "Be careful seller! 2");
    private static final List<String> SELLER_MESSAGES_PLAIN = toExpectedPlainContent(SELLER_SAFETY_TEXTS);
    private static final List<String> SELLER_MESSAGES_HTML = toExpectedHtmlContent(SELLER_SAFETY_TEXTS);
    private static final List<String> BUYER_SAFETY_TEXTS = Arrays.asList(
            "Be careful buyer! 1", "Be careful buyer! 2", "Be careful buyer! 3");
    private static final List<String> BUYER_MESSAGES_HTML = toExpectedHtmlContent(BUYER_SAFETY_TEXTS);
    private static final List<String> BUYER_MESSAGES_PLAIN = toExpectedPlainContent(BUYER_SAFETY_TEXTS);

    private SafetyMessagePostProcessor processor;

    @Mock private Conversation conversation;
    @Mock private Message sellerSentMessage;
    @Mock private Message sellerBlockedMessage;
    @Mock private Message sellerDelayedMessage;
    @Mock private Message buyerSentMessage;
    @Mock private Message buyerBlockedMessage;
    @Mock private Message buyerDelayedMessage;
    @Mock private Mail mail;
    @Mock private MessageProcessingContext messageProcessingContext;
    @Mock private TypedContent<String> mutablePlainContent;
    @Mock private TypedContent<String> mutableHtmlContent;

    @Before
    public void setup() throws IOException, PersistenceException {
        SafetyMessagePostProcessorConfig config = new SafetyMessagePostProcessorConfig(SELLER_SAFETY_TEXTS, BUYER_SAFETY_TEXTS);

        initMocks(this);

        processor = new SafetyMessagePostProcessor(config);

        when(sellerSentMessage.getState()).thenReturn(MessageState.SENT);
        when(sellerSentMessage.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(sellerBlockedMessage.getState()).thenReturn(MessageState.BLOCKED);
        when(sellerBlockedMessage.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(sellerDelayedMessage.getState()).thenReturn(MessageState.HELD);
        when(sellerDelayedMessage.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(buyerSentMessage.getState()).thenReturn(MessageState.SENT);
        when(buyerSentMessage.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(buyerBlockedMessage.getState()).thenReturn(MessageState.BLOCKED);
        when(buyerBlockedMessage.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(buyerDelayedMessage.getState()).thenReturn(MessageState.HELD);
        when(buyerDelayedMessage.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(mail.getFrom()).thenReturn(DUMMY_FROM_ADDRESS);
        when(mail.getTextParts(false)).thenReturn(asTypedContentList(mutablePlainContent, mutableHtmlContent));
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(messageProcessingContext.getMail()).thenReturn(mail);
        when(mutablePlainContent.getMediaType()).thenReturn(MediaType.create("text", "plain"));
        when(mutablePlainContent.isMutable()).thenReturn(true);
        when(mutablePlainContent.getContent()).thenReturn(PART_CONTENT);
        when(mutableHtmlContent.getMediaType()).thenReturn(MediaType.create("text", "html"));
        when(mutableHtmlContent.isMutable()).thenReturn(true);
        when(mutableHtmlContent.getContent()).thenReturn(PART_CONTENT);
    }

    // Circumvents problems with generics in arrays
    @SuppressWarnings({"unchecked"})
    private List<TypedContent<String>> asTypedContentList(TypedContent<String> c1, TypedContent<String> c2) {
        return Arrays.asList(c1, c2);
    }

    @Test
    public void testFirstSkippingSellerSafetyTest_FirstMessageToSeller() {
        assertTrue("expected empty string for first SELLER_SAFETY_TEXTS", SELLER_SAFETY_TEXTS.get(0).isEmpty());

        // no preceding messages in conversation
        when(conversation.getMessages()).thenReturn(null);
        when(messageProcessingContext.getMessage()).thenReturn(buyerSentMessage);

        // call method under test
        processor.postProcess(messageProcessingContext);

        // verify that both parts were not modified
        verify(mutablePlainContent, never()).overrideContent(anyString());
        verify(mutableHtmlContent, never()).overrideContent(anyString());
    }

    @Test
    public void testFirstSellerSafetyText_FirstMessageToBuyer() {
        assertTrue("expected non-empty string for first BUYER_SAFETY_TEXTS", !BUYER_SAFETY_TEXTS.get(0).isEmpty());

        // Preceding messages in conversation:
        //  -- 1: initial message from buyer to seller
        when(messageProcessingContext.getMessage()).thenReturn(buyerSentMessage);

        // call method under test
        processor.postProcess(messageProcessingContext);

        // verify that both parts were modified
        verify(mutablePlainContent).overrideContent(BUYER_MESSAGES_PLAIN.get(0));
        verify(mutableHtmlContent).overrideContent(BUYER_MESSAGES_HTML.get(0));
    }

    @Test
    public void testLastSellerSafetyText_SecondMessageToSeller() {
        assertTrue("expected non-empty string for second SELLER_SAFETY_TEXTS", !SELLER_SAFETY_TEXTS.get(1).isEmpty());

        // Preceding messages in conversation:
        //  -- 1: initial message from buyer to seller
        //  -- 2: seller's reply to the buyer
        when(conversation.getMessages()).thenReturn(Arrays.asList(buyerSentMessage, sellerSentMessage));
        when(messageProcessingContext.getMessage()).thenReturn(buyerSentMessage);

        // call method under test
        processor.postProcess(messageProcessingContext);

        // verify that both parts were modified
        verify(mutablePlainContent).overrideContent(SELLER_MESSAGES_PLAIN.get(1));
        verify(mutableHtmlContent).overrideContent(SELLER_MESSAGES_HTML.get(1));
    }

    @Test
    public void testExhaustedSellerSafetyTexts_ThirdMessageToSeller() {
        assertTrue("expected SELLER_SAFETY_TEXTS to contain 2 messages", SELLER_SAFETY_TEXTS.size() == 2);

        // Preceding messages in conversation:
        //  -- 1: initial message from buyer to seller
        //  -- 2: seller's reply to the buyer
        //  -- 3: another message from buyer to seller
        //  -- 4: another message from seller to buyer
        when(conversation.getMessages()).thenReturn(Arrays.asList(
                buyerSentMessage, sellerSentMessage, buyerSentMessage, sellerSentMessage));
        when(messageProcessingContext.getMessage()).thenReturn(buyerSentMessage);

        // call method under test
        processor.postProcess(messageProcessingContext);

        // verify that both parts were not modified
        verify(mutablePlainContent, never()).overrideContent(anyString());
        verify(mutableHtmlContent, never()).overrideContent(anyString());
    }


    @Test
    public void testExhaustedBuyerSafetyTexts_FourthMessageToBuyer() {
        assertTrue("expected BUYER_SAFETY_TEXTS to contain 3 messages", BUYER_SAFETY_TEXTS.size() == 3);

        // Preceding messages in conversation:
        //  -- 1: initial message from buyer to seller
        //  -- 2: seller's reply to the buyer (1)
        //  -- 3: another message from buyer to seller
        //  -- 4: another message from seller to buyer (2)
        //  -- 5: yet another message from buyer to seller
        //  -- 6: yet another message from seller to buyer (3)
        //  -- 7: and yet another message from buyer to seller
        when(conversation.getMessages()).thenReturn(Arrays.asList(
                buyerSentMessage, sellerSentMessage, buyerSentMessage, sellerSentMessage,
                buyerSentMessage, sellerSentMessage, buyerSentMessage));
        when(messageProcessingContext.getMessage()).thenReturn(sellerSentMessage);

        // call method under test
        processor.postProcess(messageProcessingContext);

        // verify that both parts were not modified
        verify(mutablePlainContent, never()).overrideContent(anyString());
        verify(mutableHtmlContent, never()).overrideContent(anyString());
    }

    @Test
    public void testSecondSellerSafetyText_NotCountingNonSentMessages() {
        assertTrue("expected non-empty string for second SELLER_SAFETY_TEXTS", !SELLER_SAFETY_TEXTS.get(1).isEmpty());

        // Preceding messages in conversation:
        //  -- 1: initial message from buyer to seller
        //  -- 2: seller's reply to the buyer
        //  -- 3: another message from buyer to seller, ON HOLD
        //  -- 4: another message from seller to buyer, ON HOLD
        //  -- 5: yet another message from buyer to seller, BLOCKED
        when(conversation.getMessages()).thenReturn(Arrays.asList(
                buyerSentMessage, sellerSentMessage, buyerDelayedMessage, sellerDelayedMessage, buyerBlockedMessage));
        when(messageProcessingContext.getMessage()).thenReturn(buyerSentMessage);

        // call method under test
        processor.postProcess(messageProcessingContext);

        // verify that both parts were modified
        verify(mutablePlainContent).overrideContent(SELLER_MESSAGES_PLAIN.get(1));
        verify(mutableHtmlContent).overrideContent(SELLER_MESSAGES_HTML.get(1));
    }

    private static List<String> toExpectedHtmlContent(List<String> messages) {
        return insertSafetyTexts(new HtmlMailPartInsertion(), messages);
    }

    private static List<String> toExpectedPlainContent(List<String> messages) {
        return insertSafetyTexts(new PlainTextMailPartInsertion(), messages);
    }

    private static List<String> insertSafetyTexts(SafetyTextInsertion insertion, List<String> messages) {
        List<String> expectedContent = new ArrayList<String>(messages.size());
        for (String message : messages) {
            if (message == null || message.isEmpty()) {
                // Content not modified
                expectedContent.add(PART_CONTENT);
            } else {
                expectedContent.add(insertion.insertSafetyText(PART_CONTENT, message));
            }
        }
        return expectedContent;
    }
}