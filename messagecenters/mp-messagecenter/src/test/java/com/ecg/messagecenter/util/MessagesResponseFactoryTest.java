package com.ecg.messagecenter.util;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.identifier.UserIdentifierServiceByUserIdHeaders;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessagesResponseFactoryTest {

    private static final String PHONE_NUMBER = "123-123";
    private static final String BUYER = "id123";
    private static final String SELLER = "id456";

    private static final String BUYER_MAIL_ADDRESS = "buyer@buyer.de";
    private static final String SELLER_MAIL_ADDRESS = "seller@seller.de";

    private UserIdentifierService userIdentifierService = new UserIdentifierServiceByUserIdHeaders();

    private MessagesResponseFactory messageStripper;
    private Conversation conv;

    private List<Message> messages;
    private Map<String, String> customValues;
    private List<String> attachments = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        messages = new ArrayList<>();

        conv = mock(Conversation.class);
        customValues = new HashMap<>();
        when(conv.getCustomValues()).thenReturn(customValues);
        when(conv.getBuyerId()).thenReturn(BUYER_MAIL_ADDRESS);
        when(conv.getSellerId()).thenReturn(SELLER_MAIL_ADDRESS);
        when(conv.getMessages()).thenReturn(messages);

        messageStripper = new MessagesResponseFactory(userIdentifierService);

        customValues.put(userIdentifierService.getSellerUserIdName(), SELLER);
        customValues.put(userIdentifierService.getBuyerUserIdName(), BUYER);
    }

    @Test
    public void onlyAppendPhoneNumberIfAvailableOnFirstMessage() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        customValues.put(MessagesResponseFactory.BUYER_PHONE_FIELD, PHONE_NUMBER);

        List<MessageResponse> transformedMessages = createMessagesList();

        assertTrue(transformedMessages.get(0).getTextShort().contains(PHONE_NUMBER));
        assertFalse(transformedMessages.get(1).getTextShort().contains(PHONE_NUMBER));
    }

    @Test
    public void skipPhoneNumberIfNotAvailable() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        customValues.put(MessagesResponseFactory.BUYER_PHONE_FIELD, null);

        List<MessageResponse> transformedMessages = createMessagesList();

        assertFalse(transformedMessages.get(0).getTextShort().contains(PHONE_NUMBER));
        assertFalse(transformedMessages.get(1).getTextShort().contains(PHONE_NUMBER));
    }

    @Test
    public void messagesVisibleIfSENT() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(2, response.size());
    }

    @Test
    public void doNotIncludeUNSENTMessagesIfNotOwnMessage() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList(SELLER);

        assertEquals(1, response.size());
    }

    @Test
    public void doNotIncludeUNSENTMessagesIfNotOwnMessage2() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER, "desktop");

        List<MessageResponse> response = createMessagesList(SELLER);

        assertEquals(2, response.size());
    }

    @Test
    public void doIncludeAllOwnMessages() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER, "desktop");
        addMessage("forthMessage", MessageState.DISCARDED, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(3, response.size());
    }

    @Test
    public void alwaysIncludeAllMessagesStatesIfOwnMessageSellerSide() {
        addMessage("firstMessage", SELLER_TO_BUYER);
        addMessage("secondMessage", MessageState.HELD, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList(SELLER);

        assertEquals(2, response.size());
    }

    @Test
    public void alwaysIncludeAllMessagesStatesIfOwnMessageBuyerSide() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.HELD, BUYER_TO_SELLER, "api_12");
        addMessage("thirdMessage", MessageState.DISCARDED, BUYER_TO_SELLER, "api_12");

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(3, response.size());
    }

    @Test
    public void skipIGNOREDMessagesEvenIfOwn() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", MessageState.IGNORED, BUYER_TO_SELLER, "desktop");

        List<MessageResponse> response = createMessagesList(BUYER);

        assertEquals(1, response.size());
    }

    @Test
    public void absentValueIfNoMessagesProvided() {
        addMessage("firstMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertFalse(messageStripper.create(SELLER, conv, messages).isPresent());
    }

    @Test
    public void lastItemForInitialContactPosterWhenNoRepliesAndNoUserMessageHeader() {
        addMessage("firstMessage", BUYER_TO_SELLER);

        assertEquals("firstMessage", messageStripper.latestMessage(BUYER, conv).get().getTextShort());
    }

    @Test
    public void lastItemForInitialContactPosterWhenNoRepliesAndUserMessageHeaderPresent() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Contact-Poster-User-Message", "user's firstMessage");
        addMessage("user's firstMessage + template", MessageState.SENT, BUYER_TO_SELLER, headers);

        assertEquals("user's firstMessage", messageStripper.latestMessage(BUYER, conv).get().getTextShort());
    }

    @Test
    public void lastItemForFurtherReplies() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);

        assertEquals("secondMessage", messageStripper.latestMessage(BUYER, conv).get().getTextShort());
    }

    @Test
    public void lastItemSkipHandleUnsentMessages() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(messageStripper.latestMessage(SELLER, conv).isPresent());
    }

    @Test
    public void lastItemSkipHandleUnsentMessages2() {
        addMessage("firstMessage", BUYER_TO_SELLER);
        addMessage("secondMessage", SELLER_TO_BUYER);
        addMessage("thirdMessage", MessageState.HELD, BUYER_TO_SELLER);

        assertTrue(messageStripper.latestMessage(BUYER, conv).isPresent());
    }

    @Test
    public void includeSenderMailToMessage() {
        addMessage("firstMessage", MessageState.SENT, BUYER_TO_SELLER, "desktop");
        addMessage("secondMessage", MessageState.SENT, SELLER_TO_BUYER, "desktop");

        List<MessageResponse> response = createMessagesList();

        assertEquals("buyer@buyer.de", response.get(response.size() - 2).getSenderEmail());
        assertEquals("seller@seller.de", response.get(response.size() - 1).getSenderEmail());
    }

    @Test
    public void contactPosterUserMessages() {
        Map<String, String> headersFirstMessage = new LinkedHashMap<>();
        headersFirstMessage.put("X-Contact-Poster-User-Message", "user's \\n firstMessage");
        addMessage("user's firstMessage + template", MessageState.SENT, BUYER_TO_SELLER, headersFirstMessage);

        Map<String, String> headersSecondMessage = new LinkedHashMap<>();
        headersSecondMessage.put("X-Reply-Channel", "cp_desktop");
        headersSecondMessage.put("X-Contact-Poster-User-Message", "user's secondMessage");
        addMessage("user's secondMessage + template", MessageState.SENT, BUYER_TO_SELLER, headersSecondMessage);

        Map<String, String> headersThirdMessage = new LinkedHashMap<>();
        headersThirdMessage.put("X-Reply-Channel", "cp_desktop");
        addMessage("user's thirdMessage + template", MessageState.SENT, BUYER_TO_SELLER, headersThirdMessage);

        List<MessageResponse> messages = createMessagesList();

        assertEquals(messages.get(0).getTextShort(), "user's \n firstMessage");
        assertEquals(messages.get(1).getTextShort(), "user's secondMessage");
        assertEquals(messages.get(2).getTextShort(), "user's thirdMessage + template");
    }

    @Test
    public void messageBoxClientUserMessages() {
        Map<String, String> headersFirstMessage = new LinkedHashMap<>();
        headersFirstMessage.put("X-User-Message", "user's \\n firstMessage");
        addMessage("user's firstMessage + template", MessageState.SENT, BUYER_TO_SELLER, headersFirstMessage);

        Map<String, String> headersSecondMessage = new LinkedHashMap<>();
        headersSecondMessage.put("X-Reply-Channel", "cp_desktop");
        headersSecondMessage.put("X-User-Message", "user's secondMessage");
        addMessage("user's secondMessage + template", MessageState.SENT, BUYER_TO_SELLER, headersSecondMessage);

        Map<String, String> headersThirdMessage = new LinkedHashMap<>();
        headersThirdMessage.put("X-Reply-Channel", "cp_desktop");
        addMessage("user's thirdMessage + template", MessageState.SENT, BUYER_TO_SELLER, headersThirdMessage);

        List<MessageResponse> messages = createMessagesList();

        assertEquals(messages.get(0).getTextShort(), "user's \n firstMessage");
        assertEquals(messages.get(1).getTextShort(), "user's secondMessage");
        assertEquals(messages.get(2).getTextShort(), "user's thirdMessage + template");
    }

    @Test
    public void removeEmailClientWhenAnonymizedEmailFragment() {
        customValues.put("buyer-name", "BuyerName :)");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message from **SellerName** :) to BuyerName.\n" +
                "Van: b-someemail@mail.marktplaats.nl\n" +
                "Datum: Wednesday 20 January 2016 at 16:10\n+" +
                "Message from BuyerName :) to **SellerName**.", SELLER_TO_BUYER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message from **SellerName** :) to BuyerName.", messages.get(1).getTextShort());

    }


    @Test
    public void strippedEmailAddressWhenUserRepliesToOwnEmail() {
        customValues.put("buyer-name", "BuyerName :)");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message2 from BuyerName :) to **SellerName**.\n" +
                        "\n" +
                        "From: \" BuyerName :) <buyerRealEmail@marktplaats.nl>\n" +
                        "Date: Wednesday 17 February 2016 at 14:32\n" +
                        "To: **SellerName** via Marktplaats <abc99321@mail.marktplaats.nl>\n" +
                        "Subject: Re: Ik heb interesse in 'Ad'\n" +
                        "\n" +
                        "Message from BuyerName :) to **SellerName**."
                , BUYER_TO_SELLER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message2 from BuyerName :) to **SellerName**.", messages.get(1).getTextShortTrimmed());
    }

    @Test
    public void strippedEmailAddressWhenUserRepliesToOwnEmailWithGmailAddress() {
        customValues.put("buyer-name", "BuyerName :)");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message2 from BuyerName :) to **SellerName**.\n" +
                        "\n" +
                        "On Thu, Feb 15, 2016 at 14:31 AM, BuyerName <buyerRealEmail@gmail.com> wrote:\n" +

                        "Message from BuyerName :) to **SellerName**."
                , BUYER_TO_SELLER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message2 from BuyerName :) to **SellerName**.", messages.get(1).getTextShortTrimmed());
    }

    @Test
    public void removeEmailClientWhenActualEmailFragment() {
        customValues.put("buyer-name", "BuyerName :)");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message from **SellerName** :) to BuyerName.\n" +
                "From: =?ANSI_X3.4-1968?Q?M=3Fur=3F_=26_Gra=3Fias=3F=3F_=40ho?= =?ANSI_X3.4-1968?Q?tmail_via_Marktplaats?\n" +
                "To: realEmail@gmail.com\n" +
                "Datum: Wednesday 20 January 2016 at 16:10\n+" +
                "Message from BuyerName :) to **SellerName**.", SELLER_TO_BUYER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message from **SellerName** :) to BuyerName.\n" +
                        "From: =?ANSI_X3.4-1968?Q?M=3Fur=3F_=26_Gra=3Fias=3F=3F_=40ho?= =?ANSI_X3.4-1968?Q?tmail_via_Marktplaats?",
                messages.get(1).getTextShort());
    }

    @Test
    public void removeEmailClientWhenDifferentEmailFragmentPattersArePresent() {
        customValues.put("buyer-name", "BuyerName");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message 5\n" +
                "From: BuyerName\n" +
                "To: \"Seller\" <**SellerName**@marktplaats.nl<mailto:**SellerName**@marktplaats.nl>>\n" +
                "Subject: Re: Ik heb interesse in 'Ad 7' - BuyerName\n" +
                "Message 4\n" +
                "From: **SellerName** via Marktplaats <b-k0f27hzhhp10@mail.qa-mp.so<mailto:b-k0f27hzhhp10@mail.qa-mp.so>>\n" +
                "Date: Wednesday 27 January 2016 12:01\n" +
                "To: Buyer <a-3c3atu8y17m2y@mail.qa-mp.so<mailto:a-3c3atu8y17m2y@mail.qa-mp.so>>\n" +
                "Subject: Re: Ik heb interesse in 'Ad 7' - BuyerName\n" +
                "message2\n" +
                "Van: BuyerName via Marktplaats <a-3c3atu8y17m2y@mail.qa-mp.so<mailto:a-3c3atu8y17m2y@mail.qa-mp.so>>\n" +
                "Date: Wednesday 27 January 2016 at 12:00\n" +
                "To: \"Seller\" <b-k0f27hzhhp10@mail.qa-mp.so<mailto:b-k0f27hzhhp10@mail.qa-mp.so>>" +
                "\nSubject: Ik heb interesse in 'Ad 7' - BuyerName\n" +
                "nmessage 1", SELLER_TO_BUYER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message 5\n" +
                "From: BuyerName", messages.get(1).getTextShort());
    }

    @Test
    public void removeEmailClientFragmentWhenItStartsWithSubject() {
        customValues.put("buyer-name", "BuyerName");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message 5\n" +
                "Subject: Re: Ik heb interesse in 'Ad 7' - BuyerName\n" +
                "From: BuyerName\n" +
                "To: \"Seller\" <**SellerName**@marktplaats.nl<mailto:**SellerName**@marktplaats.nl>>\n" +
                "Date: Wednesday 27 January 2016 12:01\n" +
                "Message 4\n", SELLER_TO_BUYER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message 5", messages.get(1).getTextShort());
    }

    @Test
    public void removeEmailClientFragmentWhenItStartsWithDate() {
        customValues.put("buyer-name", "BuyerName");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Message 5\n" +
                "Date: Wednesday 27 January 2016 12:01\n" +
                "Subject: Re: Ik heb interesse in 'Ad 7' - BuyerName\n" +
                "From: BuyerName\n" +
                "To: \"Seller\" <**SellerName**@marktplaats.nl<mailto:**SellerName**@marktplaats.nl>>\n" +
                "Message 4\n", SELLER_TO_BUYER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Message 5", messages.get(1).getTextShort());
    }

    @Test
    public void removeEmailClientFragmentWhenItStartsWithDefault() {
        customValues.put("buyer-name", "BuyerName :)");
        customValues.put("seller-name", "**SellerName**");

        addMessage("Message from BuyerName :) to **SellerName**.", BUYER_TO_SELLER);

        addMessage("Hi BuyerName :) via Marktplaats,\n" +
                "Message from **SellerName** :) to BuyerName.\n" +
                "BuyerName :) via Marktplaats <b-someemail@mail.marktplaats.nl>\n" +
                "Datum: Wednesday 20 January 2016 at 16:10\n+" +
                "Message from BuyerName :) to **SellerName**.", SELLER_TO_BUYER);

        List<MessageResponse> messages = createMessagesList();

        assertEquals("Hi BuyerName :) via Marktplaats,\n" +
                "Message from **SellerName** :) to BuyerName.", messages.get(1).getTextShort());
    }

    @Test
    public void regexpsShouldNotChokeOnVeryLargeMessages() throws IOException {
        customValues.put("buyer-name", "buyerName");
        customValues.put("seller-name", "sellerName");

        addMessage("First message", BUYER_TO_SELLER);

        BufferedReader buffer = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("message-with-embedded-image")));
        String largeMessage = buffer.lines().collect(Collectors.joining("\n"));
        addMessage(largeMessage, SELLER_TO_BUYER);

        createMessagesList();

        // just verify it does not hang for 30 mins ;-)
    }

    private void addMessage(String text, MessageDirection messageDirection) {
        addMessage(text, MessageState.SENT, messageDirection, "mail");
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection) {
        addMessage(text, state, messageDirection, "mail");
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection, String replyChannelInfo) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Reply-Channel", replyChannelInfo);
        addMessage(text, state, messageDirection, headers);
    }

    private void addMessage(String text, MessageState state, MessageDirection messageDirection, Map<String, String> headers) {
        Message message = mock(Message.class);
        when(message.getReceivedAt()).thenReturn(new DateTime());
        when(message.getMessageDirection()).thenReturn(messageDirection);
        when(message.getPlainTextBody()).thenReturn(text);
        when(message.getState()).thenReturn(state);

        when(message.getHeaders()).thenReturn(headers);

        when(message.getAttachmentFilenames()).thenReturn(attachments);

        addMessage(message);
    }

    private List<MessageResponse> createMessagesList() {
        return messageStripper.create(BUYER, conv, messages).get();
    }

    private List<MessageResponse> createMessagesList(String email) {
        return messageStripper.create(email, conv, messages).get();
    }

    private void addMessage(Message firstMessage) {
        messages.add(firstMessage);
    }
}