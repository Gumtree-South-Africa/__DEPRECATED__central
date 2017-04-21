package com.ecg.de.ebayk.messagecenter.util;

import com.ecg.de.ebayk.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class MessagesResponseFactoryTest {

    private static final String PHONE_NUMBER = "123-123";
    private static final String BUYER = "buyer@buyer.de";
    private static final String SELLER = "seller@seller.de";


    private MessagesResponseFactory messageStripper;
    private Conversation conv;

    private List<Message> messages;
    private MessagesDiffer differ;
    private Map<String, String> customValues;


    @Before public void setUp() throws Exception {
        messages = new ArrayList<Message>();

        differ = mock(MessagesDiffer.class);
        when(differ.cleanupFirstMessage(anyString())).thenReturn("cleanedUpFirstMessage");
        when(differ.diff(any(MessagesDiffer.DiffInput.class), any(MessagesDiffer.DiffInput.class)))
                        .thenReturn(new TextDiffer.TextCleanerResult("diffed"));

        conv = mock(Conversation.class);
        customValues = new HashMap<String, String>();
        when(conv.getCustomValues()).thenReturn(customValues);
        when(conv.getBuyerId()).thenReturn(BUYER);
        when(conv.getSellerId()).thenReturn(SELLER);
        when(conv.getMessages()).thenReturn(messages);

        messageStripper = new MessagesResponseFactory(differ);
    }

    @Ignore @Test public void diffSubsequentMessages() throws Exception {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", BUYER_TO_SELLER);

        List<MessageResponse> transformedMessages = createMessagesList();

        assertEquals(3, transformedMessages.size());
        verify(differ, times(1)).cleanupFirstMessage("firstMessage");
        verify(differ, times(1)).diff(new MessagesDiffer.DiffInput("firstMessage", null, null),
                        new MessagesDiffer.DiffInput("secondMessage", null, null));
        verify(differ, times(1)).diff(new MessagesDiffer.DiffInput("secondMessage", null, null),
                        new MessagesDiffer.DiffInput("thirdMessage", null, null));
    }

    @Test public void cleanOnlyOnFirstMessage() {
        addMessage("firstMessage", BUYER_TO_SELLER);

        List<MessageResponse> transformedMessages = createMessagesList();

        assertEquals(1, transformedMessages.size());
        verify(differ, times(1)).cleanupFirstMessage("firstMessage");
        verify(differ, never()).diff(any(MessagesDiffer.DiffInput.class),
                        any(MessagesDiffer.DiffInput.class));
    }

    @Test public void doNotStripMessageSentViaMessageCenterClientViaApi() throws Exception {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", BUYER_TO_SELLER, "api_16");

        List<MessageResponse> transformedMessages = createMessagesList();

        assertEquals(2, transformedMessages.size());
        assertEquals("secondMessage", transformedMessages.get(1).getTextShort());
        verify(differ, times(1)).cleanupFirstMessage("firstMessage");
        verify(differ, never()).diff(any(MessagesDiffer.DiffInput.class),
                        any(MessagesDiffer.DiffInput.class));

    }

    @Test public void doNotStripMessageSentViaMessageCenterClientViaDesktop() throws Exception {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", BUYER_TO_SELLER, "desktop");

        List<MessageResponse> transformedMessages = createMessagesList();

        assertEquals(2, transformedMessages.size());
        assertEquals("secondMessage", transformedMessages.get(1).getTextShort());
        verify(differ, times(1)).cleanupFirstMessage("firstMessage");
        verify(differ, never()).diff(any(MessagesDiffer.DiffInput.class),
                        any(MessagesDiffer.DiffInput.class));

    }


    @Test public void onlyAppendPhoneNumberIfAvailableOnFirstMessage() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        customValues.put(MessagesResponseFactory.BUYER_PHONE_FIELD, PHONE_NUMBER);

        List<MessageResponse> transformedMessages = createMessagesList();

        assertTrue(transformedMessages.get(0).getTextShort().contains(PHONE_NUMBER));
        assertFalse(transformedMessages.get(1).getTextShort().contains(PHONE_NUMBER));
    }

    @Test public void skipPhoneNumberIfNotAvailable() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        customValues.put(MessagesResponseFactory.BUYER_PHONE_FIELD, null);

        List<MessageResponse> transformedMessages = createMessagesList();

        assertFalse(transformedMessages.get(0).getTextShort().contains(PHONE_NUMBER));
        assertFalse(transformedMessages.get(1).getTextShort().contains(PHONE_NUMBER));
    }

    @Test public void messagesVisibleIfSENT() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(2, response.size());
    }

    @Test public void doNotIncludeUNSENTMessagesIfNotOwnMessage() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList(SELLER);

        assertEquals(1, response.size());
    }

    @Test public void doNotIncludeUNSENTMessagesIfNotOwnMessage2() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER, "desktop");

        List<MessageResponse> response = createMessagesList(SELLER);

        assertEquals(2, response.size());
    }

    @Test public void doIncludeAllOwnMessages() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER, "desktop");
        addMessage("forthMessage", MessageState.DISCARDED, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(3, response.size());
    }

    @Test public void alwaysIncludeAllMessagesStatesIfOwnMessageSellerSide() {
        addMessage("firstMessage", SELLER_TO_BUYER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList(SELLER);

        assertEquals(2, response.size());
    }

    @Test public void alwaysIncludeAllMessagesStatesIfOwnMessageBuyerSide() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, BUYER_TO_SELLER, "api_12");
        addMessage("thirdMessage", MessageState.DISCARDED, BUYER_TO_SELLER, "api_12");

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(3, response.size());
    }

    @Test public void skipIGNOREDMessagesEvenIfOwn() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.IGNORED, BUYER_TO_SELLER, "desktop");

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(1, response.size());
    }

    @Test public void absentValueIfNoMessagesProvided() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertFalse(messageStripper.create(SELLER, conv, messages).isPresent());
    }

    @Test public void lastItemForIntialContactPosterWhenNoReplies() {
        addMessage("firstMessage", BUYER_TO_SELLER);

        assertEquals("cleanedUpFirstMessage",
                        messageStripper.latestMessage(BUYER, conv).get().getTextShort());
    }

    @Ignore @Test public void lastItemForFurtherReplies() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);

        assertEquals("diffed", messageStripper.latestMessage(BUYER, conv).get().getTextShort());
    }

    @Test public void lastItemSkipHandleUnsentMessages() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(messageStripper.latestMessage(SELLER, conv).isPresent());
    }

    @Test public void lastItemSkipHandleUnsentMessages2() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(messageStripper.latestMessage(BUYER, conv).isPresent());
    }

    @Ignore @Test public void diffAlwaysOnLastOtherRole() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", SELLER_TO_BUYER);

        createMessagesList();

        verify(differ).diff(argThat(new MessageTextMatcher("firstMessage")),
                        argThat(new MessageTextMatcher("secondMessage")));
        verify(differ).diff(argThat(new MessageTextMatcher("firstMessage")),
                        argThat(new MessageTextMatcher("thirdMessage")));
    }

    @Test public void doNotDiffReinitializedContactPoster() {
        addMessage("firstMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "cp_desktop");

        createMessagesList();

        verify(differ, times(2)).cleanupFirstMessage(anyString());
        verify(differ, never()).diff(any(MessagesDiffer.DiffInput.class),
                        any(MessagesDiffer.DiffInput.class));
    }

    @Test public void includeSenderMailToMessage() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList();

        assertEquals("buyer@buyer.de", response.get(response.size() - 2).getSenderEmail());
        assertEquals("seller@seller.de", response.get(response.size() - 1).getSenderEmail());
    }

    @Test public void doNotIncludeRobotMessageIfRoleIsSellerAndDirectionIsSellerToBuyer() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("robotMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop", true);

        List<MessageResponse> response = createMessagesList(SELLER);
        assertEquals(1, response.size());
        assertEquals("cleanedUpFirstMessage", response.get(0).getTextShortTrimmed());
    }

    @Test public void doNotIncludeRobotMessageIfRoleIsBuyerAndDirectionIsBuyerToSeller() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("robotMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop", true);

        List<MessageResponse> response = createMessagesList(BUYER);
        assertEquals(2, response.size());
        assertEquals("cleanedUpFirstMessage", response.get(0).getTextShortTrimmed());
    }

    @Test public void doNotIncludeRobotMessageIfRobotDisabled() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("robotMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop", true);

        List<MessageResponse> response = createMessagesList(BUYER, false);
        assertEquals(1, response.size());
        assertEquals("cleanedUpFirstMessage", response.get(0).getTextShortTrimmed());
    }

    @Test public void includeRobotMessageIfRobotEnabled() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("robotMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop", true);

        List<MessageResponse> response = createMessagesList(BUYER, true);
        assertEquals(2, response.size());
        assertEquals("cleanedUpFirstMessage", response.get(0).getTextShortTrimmed());
        assertEquals("robotMessage", response.get(1).getTextShortTrimmed());
    }

    private void addMessage(String text, MessageDirection messageDirection) {
        addMessage(text, MessageState.SENT, messageDirection, "mail", false);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection) {
        addMessage(text, state, messageDirection, "mail", false);
    }

    private void addMessage(String text, MessageDirection messageDirection,
                    String replyChannelInfo) {
        addMessage(text, MessageState.SENT, messageDirection, replyChannelInfo, false);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection,
                    String replyChannelInfo) {
        addMessage(text, state, messageDirection, replyChannelInfo, false);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection,
                    boolean isRobotMessage) {
        addMessage(text, state, messageDirection, "mail", isRobotMessage);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection,
                    String replyChannelInfo, boolean isRobotMessage) {
        Message message = mock(Message.class);
        when(message.getReceivedAt()).thenReturn(new DateTime());
        when(message.getMessageDirection()).thenReturn(messageDirection);
        when(message.getPlainTextBody()).thenReturn(text);
        when(message.getState()).thenReturn(state);

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("X-Reply-Channel", replyChannelInfo);

        Map<String, String> mapWithRobotHeader = new LinkedHashMap<String, String>();
        mapWithRobotHeader.put("X-Reply-Channel", replyChannelInfo);
        mapWithRobotHeader.put("X-Robot", "GTAU");

        if (isRobotMessage) {
            when(message.getHeaders()).thenReturn(mapWithRobotHeader);
        } else {
            when(message.getHeaders()).thenReturn(map);
        }

        addMessage(message);
    }


    private List<MessageResponse> createMessagesList() {
        return messageStripper.create(BUYER, conv, messages).get();
    }

    private List<MessageResponse> createMessagesList(String email) {
        return messageStripper.create(email, conv, messages).get();
    }

    private List<MessageResponse> createMessagesList(String email, boolean robotEnabled) {
        return messageStripper.create(email, conv, messages, robotEnabled).get();
    }

    private void addMessage(Message firstMessage) {
        messages.add(firstMessage);
    }


}


class MessageTextMatcher extends BaseMatcher<MessagesDiffer.DiffInput> {
    private String firstMessage;

    public MessageTextMatcher(String firstMessage) {
        this.firstMessage = firstMessage;
    }

    @Override public boolean matches(Object o) {
        MessagesDiffer.DiffInput diffInput = (MessagesDiffer.DiffInput) o;
        return diffInput.getText().equals(firstMessage);
    }


    @Override public void describeTo(Description description) {
    }
}
