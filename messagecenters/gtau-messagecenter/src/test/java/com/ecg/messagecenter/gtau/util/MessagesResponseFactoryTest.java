package com.ecg.messagecenter.gtau.util;

import com.ecg.messagecenter.gtau.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessagesResponseFactoryTest {

    private static final String PHONE_NUMBER = "123-123";
    private static final String BUYER = "buyer@buyer.de";
    private static final String SELLER = "seller@seller.de";
    private static final String BUYER_PHONE_FIELD = "buyer-phonenumber";

    private Conversation conv;
    private List<Message> messages;
    private Map<String, String> customValues;

    @Before
    public void setUp() throws Exception {
        messages = new ArrayList<>();

        conv = mock(Conversation.class);
        customValues = new HashMap<>();
        when(conv.getCustomValues()).thenReturn(customValues);
        when(conv.getBuyerId()).thenReturn(BUYER);
        when(conv.getSellerId()).thenReturn(SELLER);
        when(conv.getMessages()).thenReturn(messages);
    }

    @Test
    public void cleanOnlyOnFirstMessage() {
        addMessage("firstMessage", BUYER_TO_SELLER);

        List<MessageResponse> transformedMessages = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(1, transformedMessages.size());
    }

    @Test
    public void doNotStripMessageSentViaMessageCenterClientViaApi() throws Exception {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", BUYER_TO_SELLER, "api_16");

        List<MessageResponse> transformedMessages = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(2, transformedMessages.size());
        assertEquals("secondMessage", transformedMessages.get(1).getTextShort());
    }

    @Test
    public void doNotStripMessageSentViaMessageCenterClientViaDesktop() throws Exception {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", BUYER_TO_SELLER, "desktop");

        List<MessageResponse> transformedMessages = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(2, transformedMessages.size());
        assertEquals("secondMessage", transformedMessages.get(1).getTextShort());
    }

    @Test
    public void onlyAppendPhoneNumberIfAvailableOnFirstMessage() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        customValues.put(BUYER_PHONE_FIELD, PHONE_NUMBER);

        List<MessageResponse> transformedMessages = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertTrue(transformedMessages.get(0).getTextShort().contains(PHONE_NUMBER));
        assertFalse(transformedMessages.get(1).getTextShort().contains(PHONE_NUMBER));
    }

    @Test
    public void skipPhoneNumberIfNotAvailable() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        customValues.put(BUYER_PHONE_FIELD, null);

        List<MessageResponse> transformedMessages = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertFalse(transformedMessages.get(0).getTextShort().contains(PHONE_NUMBER));
        assertFalse(transformedMessages.get(1).getTextShort().contains(PHONE_NUMBER));
    }

    @Test
    public void messagesVisibleIfSENT() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);

        List<MessageResponse> response = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(2, response.size());
    }

    @Test
    public void doNotIncludeUNSENTMessagesIfNotOwnMessage() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = MessagesResponseFactory.create(SELLER, conv, messages).get();

        assertEquals(1, response.size());
    }

    @Test
    public void doNotIncludeUNSENTMessagesIfNotOwnMessage2() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER, "desktop");

        List<MessageResponse> response = MessagesResponseFactory.create(SELLER, conv, messages).get();

        assertEquals(2, response.size());
    }

    @Test
    public void doIncludeAllOwnMessages() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER, "desktop");
        addMessage("forthMessage", MessageState.DISCARDED, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(3, response.size());
    }

    @Test
    public void alwaysIncludeAllMessagesStatesIfOwnMessageSellerSide() {
        addMessage("firstMessage", SELLER_TO_BUYER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = MessagesResponseFactory.create(SELLER, conv, messages).get();

        assertEquals(2, response.size());
    }

    @Test
    public void alwaysIncludeAllMessagesStatesIfOwnMessageBuyerSide() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, BUYER_TO_SELLER, "api_12");
        addMessage("thirdMessage", MessageState.DISCARDED, BUYER_TO_SELLER, "api_12");

        List<MessageResponse> response = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(3, response.size());
    }

    @Test
    public void skipIGNOREDMessagesEvenIfOwn() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.IGNORED, BUYER_TO_SELLER, "desktop");

        List<MessageResponse> response = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals(1, response.size());
    }

    @Test
    public void lastItemSkipHandleUnsentMessages() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(MessagesResponseFactory.latestMessage(SELLER, conv).isPresent());
    }

    @Test
    public void lastItemSkipHandleUnsentMessages2() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(MessagesResponseFactory.latestMessage(BUYER, conv).isPresent());
    }

    @Test
    public void includeSenderMailToMessage() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = MessagesResponseFactory.create(BUYER, conv, messages).get();

        assertEquals("buyer@buyer.de", response.get(response.size() - 2).getSenderEmail());
        assertEquals("seller@seller.de", response.get(response.size() - 1).getSenderEmail());
    }

    private void addMessage(String text, MessageDirection messageDirection) {
        addMessage(text, MessageState.SENT, messageDirection, "mail", false);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection) {
        addMessage(text, state, messageDirection, "mail", false);
    }

    private void addMessage(String text, MessageDirection messageDirection, String replyChannelInfo) {
        addMessage(text, MessageState.SENT, messageDirection, replyChannelInfo, false);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection, String replyChannelInfo) {
        addMessage(text, state, messageDirection, replyChannelInfo, false);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection, String replyChannelInfo, boolean isRobotMessage) {
        Message message = mock(Message.class);
        when(message.getReceivedAt()).thenReturn(new DateTime());
        when(message.getMessageDirection()).thenReturn(messageDirection);
        when(message.getPlainTextBody()).thenReturn(text);
        when(message.getState()).thenReturn(state);

        Map<String, String> map = new LinkedHashMap<>();
        map.put("X-Reply-Channel", replyChannelInfo);

        Map<String, String> mapWithRobotHeader = new LinkedHashMap<>();
        mapWithRobotHeader.put("X-Reply-Channel", replyChannelInfo);
        mapWithRobotHeader.put("X-Robot", "GTAU");

        if (isRobotMessage) {
            when(message.getCaseInsensitiveHeaders()).thenReturn(mapWithRobotHeader);
        } else {
            when(message.getCaseInsensitiveHeaders()).thenReturn(map);
        }

        messages.add(message);
    }
}
