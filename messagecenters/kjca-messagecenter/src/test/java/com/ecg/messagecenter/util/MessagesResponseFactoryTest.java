package com.ecg.messagecenter.util;

import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final String BUYER = "buyer@buyer.de";
    private static final String SELLER = "seller@seller.de";


    private MessagesResponseFactory messagesResponseFactory;
    private Conversation conv;

    private List<Message> messages;
    private MessagesDiffer differ;
    private Map<String, String> customValues;


    @Before
    public void setUp() throws Exception {
        messages = new ArrayList<>();

        differ = mock(MessagesDiffer.class);
        when(differ.cleanupFirstMessage(anyString())).thenReturn("cleanedUpFirstMessage");
        when(differ.diff(any(MessagesDiffer.DiffInput.class), any(MessagesDiffer.DiffInput.class))).thenReturn(new TextDiffer.TextCleanerResult("diffed"));

        conv = mock(Conversation.class);
        customValues = new HashMap<>();
        when(conv.getCustomValues()).thenReturn(customValues);
        when(conv.getBuyerId()).thenReturn(BUYER);
        when(conv.getSellerId()).thenReturn(SELLER);
        when(conv.getMessages()).thenReturn(messages);

        messagesResponseFactory = new MessagesResponseFactory(differ);
    }

    @Test
    public void cleanOnlyOnFirstMessage() {
        addMessage("firstMessage", BUYER_TO_SELLER);

        List<MessageResponse> transformedMessages = createMessagesList().collect(Collectors.toList());

        assertEquals(1, transformedMessages.size());
        verify(differ, times(1)).cleanupFirstMessage("firstMessage");
        verify(differ, never()).diff(any(MessagesDiffer.DiffInput.class), any(MessagesDiffer.DiffInput.class));
    }

    @Test
    public void messagesVisibleIfSENT() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList(BUYER).collect(Collectors.toList());

        assertEquals(2, response.size());
    }

    @Test
    public void doNotIncludeUNSENTMessagesIfNotOwnMessage() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList(SELLER).collect(Collectors.toList());

        assertEquals(1, response.size());
    }

    @Test
    public void doNotIncludeUNSENTMessagesIfNotOwnMessage2() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        List<MessageResponse> response = createMessagesList(SELLER).collect(Collectors.toList());

        assertEquals(2, response.size());
    }

    @Test
    public void doIncludeAllOwnMessages() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);
        addMessage("forthMessage", MessageState.DISCARDED, SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList(BUYER).collect(Collectors.toList());

        assertEquals(3, response.size());
    }

    @Test
    public void alwaysIncludeAllMessagesStatesIfOwnMessageSellerSide() {
        addMessage("firstMessage", SELLER_TO_BUYER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList(SELLER).collect(Collectors.toList());

        assertEquals(2, response.size());
    }

    @Test
    public void alwaysIncludeAllMessagesStatesIfOwnMessageBuyerSide() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, BUYER_TO_SELLER);
        addMessage("thirdMessage", MessageState.DISCARDED, BUYER_TO_SELLER);

        List<MessageResponse> response = createMessagesList(BUYER).collect(Collectors.toList());

        assertEquals(3, response.size());
    }

    @Test
    public void skipIGNOREDMessagesEvenIfOwn() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.IGNORED, BUYER_TO_SELLER);

        List<MessageResponse> response = createMessagesList(BUYER).collect(Collectors.toList());

        assertEquals(1, response.size());
    }

    @Test
    public void absentValueIfNoMessagesProvided() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertFalse(messagesResponseFactory.create(SELLER, conv).findAny().isPresent());
    }

    @Test
    public void lastItemForIntialContactPosterWhenNoReplies() {
        addMessage("firstMessage", BUYER_TO_SELLER);

        assertEquals("cleanedUpFirstMessage", messagesResponseFactory.latestMessage(BUYER, conv).get().getTextShort());
    }

    @Test
    public void lastItemSkipHandleUnsentMessagesFromSeller() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(messagesResponseFactory.latestMessage(SELLER, conv).isPresent());
    }

    @Test
    public void lastItemSkipHandleUnsentMessagesFromBuyer() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        when(differ.cleanupFirstMessage("thirdMessage")).thenReturn("thirdMessage");

        Optional<MessageResponse> buyersLatestMsg = messagesResponseFactory.latestMessage(BUYER, conv);
        assertTrue(buyersLatestMsg.isPresent());
        assertEquals("thirdMessage", buyersLatestMsg.get().getTextShort());
    }

    @Test
    public void doNotDiffReinitializedContactPoster() {
        addMessage("firstMessage", MessageState.SENT, SELLER_TO_BUYER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER);

        createMessagesList().collect(Collectors.toList());

        verify(differ, times(2)).cleanupFirstMessage(anyString());
        verify(differ, never()).diff(any(MessagesDiffer.DiffInput.class), any(MessagesDiffer.DiffInput.class));
    }

    @Test
    public void includeSenderMailToMessage() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList().collect(Collectors.toList());

        assertEquals("buyer@buyer.de", response.get(response.size() - 2).getSenderEmail());
        assertEquals("seller@seller.de", response.get(response.size() - 1).getSenderEmail());
    }

    @Test
    public void emptyResponseWhenConversationHasOnlyBlankMessages() throws Exception {
        when(differ.cleanupFirstMessage("> quoted. will be removed")).thenReturn("");
        addMessage("> quoted. will be removed", MessageState.SENT, BUYER_TO_SELLER);

        Stream<MessageResponse> messagesList = createMessagesList();

        assertFalse(messagesList.findAny().isPresent());
    }

    @Test
    public void excludedEmptyMessagesFromResponse() throws Exception {
        when(differ.cleanupFirstMessage("")).thenReturn("");

        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("", MessageState.SENT, SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("fourthMessage", MessageState.SENT, SELLER_TO_BUYER);
        addMessage("", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("sixthMessage", MessageState.SENT, BUYER_TO_SELLER);

        List<MessageResponse> buyerVisibleMessages = createMessagesList(BUYER).collect(Collectors.toList());
        assertEquals(4, buyerVisibleMessages.size());

        List<MessageResponse> sellerVisibleMessages = createMessagesList(SELLER).collect(Collectors.toList());
        assertEquals(4, sellerVisibleMessages.size());
    }

    private void addMessage(String text, MessageDirection messageDirection) {
        addMessage(text, MessageState.SENT, messageDirection);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection) {
        Message message = mock(Message.class);
        when(message.getReceivedAt()).thenReturn(new DateTime());
        when(message.getMessageDirection()).thenReturn(messageDirection);
        when(message.getPlainTextBody()).thenReturn(text);
        when(message.getState()).thenReturn(state);

        Map<String, String> map = new LinkedHashMap<>();
        when(message.getHeaders()).thenReturn(map);

        addMessage(message);
    }

    private Stream<MessageResponse> createMessagesList() {
        return messagesResponseFactory.create(BUYER, conv);
    }

    private Stream<MessageResponse> createMessagesList(String email) {
        return messagesResponseFactory.create(email, conv);
    }

    private void addMessage(Message firstMessage) {
        messages.add(firstMessage);
    }

}
