package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.*;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Optional;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static com.ecg.messagecenter.core.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ConversationResponseConverterTest {

    private static final String CONVERSATION_ID = "c1";
    private static final String AD_ID = "m1234";

    private static final int UNREAD_MSGS_COUNT = 5;

    private static final String EMAIL_SUBJECT = "email subject";
    private static final String CONVERSATION_TITLE = "conversation title";

    private static final Participant PARTICIPANT_1 = new Participant("buyerId", "buyerName", "buyer@email.com", ParticipantRole.BUYER);
    private static final Participant PARTICIPANT_2 = new Participant("sellerId", "sellerName", "seller@email.com", ParticipantRole.SELLER);

    private static final ParticipantResponse PARTICIPANT_RESP_1 = new ParticipantResponse(
            PARTICIPANT_1.getUserId(), PARTICIPANT_1.getName(), PARTICIPANT_1.getEmail(), PARTICIPANT_1.getRole().getValue());
    private static final ParticipantResponse PARTICIPANT_RESP_2 = new ParticipantResponse(
            PARTICIPANT_2.getUserId(), PARTICIPANT_2.getName(), PARTICIPANT_2.getEmail(), PARTICIPANT_2.getRole().getValue());

    private static final Message MSG_BUYER = new Message(timeBased(), MessageType.ASQ, new MessageMetadata("text buyer", "buyerId"));
    private static final Message MSG_SELLER = new Message(timeBased(), MessageType.CHAT, new MessageMetadata("text seller", "sellerId"));

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void toConversationWithMessagesResponse() {
        MessageResponse expectedMsgResp1 = new MessageResponse(MSG_BUYER.getId().toString(), MSG_BUYER.getType().getValue(),
                MSG_BUYER.getText(), MSG_BUYER.getSenderUserId(), MSG_BUYER.getReceivedDate().toString(), MSG_BUYER.getCustomData());
        MessageResponse expectedMsgResp2 = new MessageResponse(MSG_SELLER.getId().toString(), MSG_SELLER.getType().getValue(),
                MSG_SELLER.getText(), MSG_SELLER.getSenderUserId(), MSG_SELLER.getReceivedDate().toString(), MSG_SELLER.getCustomData());
        expectedMsgResp2.setIsRead(false);

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, CONVERSATION_TITLE, null))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 1);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(PARTICIPANT_RESP_1, PARTICIPANT_RESP_2),
                expectedMsgResp2,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                CONVERSATION_TITLE,
                null,
                UNREAD_MSGS_COUNT,
                Optional.of(asList(expectedMsgResp1, expectedMsgResp2)));

        ConversationResponse actual = ConversationResponseConverter.toConversationResponseWithMessages(conversation);
        assertEquals(expected, actual);
    }

    @Test
    public void toConversationResponse() {
        MessageResponse expectedMsgResp = new MessageResponse(MSG_SELLER.getId().toString(), MSG_SELLER.getType().getValue(), MSG_SELLER.getText(),
                MSG_SELLER.getSenderUserId(), MSG_SELLER.getReceivedDate().toString(), MSG_SELLER.getCustomData())
                .withIsRead(true);

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, CONVERSATION_TITLE, null))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 0);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(PARTICIPANT_RESP_1, PARTICIPANT_RESP_2),
                expectedMsgResp,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                CONVERSATION_TITLE,
                null,
                UNREAD_MSGS_COUNT,
                Optional.empty());

        ConversationResponse actual = ConversationResponseConverter.toConversationResponse(conversation);
        assertEquals(expected, actual);
    }

    @Test
    public void toConversationResponseWithoutMessages() {
        MessageResponse expectedMsgResp = new MessageResponse(MSG_SELLER.getId().toString(), MSG_SELLER.getType().getValue(), MSG_SELLER.getText(),
                MSG_SELLER.getSenderUserId(), MSG_SELLER.getReceivedDate().toString(), MSG_SELLER.getCustomData());
        expectedMsgResp.setIsRead(false);
        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, CONVERSATION_TITLE, null))
                .addMessages(new ArrayList<>())
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), UNREAD_MSGS_COUNT);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(PARTICIPANT_RESP_1, PARTICIPANT_RESP_2),
                expectedMsgResp,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                CONVERSATION_TITLE,
                null,
                UNREAD_MSGS_COUNT,
                Optional.of(new ArrayList<>()));

        ConversationResponse actual = ConversationResponseConverter.toConversationResponseWithMessages(conversation);
        assertEquals(expected, actual);
    }

    @Test
    public void toConversationResponseWithoutTitle() {
        MessageResponse expectedMsgResp = new MessageResponse(MSG_SELLER.getId().toString(), MSG_SELLER.getType().getValue(), MSG_SELLER.getText(),
                MSG_SELLER.getSenderUserId(), MSG_SELLER.getReceivedDate().toString(), MSG_SELLER.getCustomData())
                .withIsRead(true);

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, null, null))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 0);


        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(PARTICIPANT_RESP_1, PARTICIPANT_RESP_2),
                expectedMsgResp,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                null,
                null,
                UNREAD_MSGS_COUNT,
                Optional.empty());
        ConversationResponse actual = ConversationResponseConverter.toConversationResponse(conversation);
        assertEquals(expected, actual);
    }

    @Test
    public void toConversationResponseWithImageUrl() {
        MessageResponse msgResp = new MessageResponse(MSG_SELLER.getId().toString(), MSG_SELLER.getType().getValue(), MSG_SELLER.getText(),
                MSG_SELLER.getSenderUserId(), MSG_SELLER.getReceivedDate().toString(), MSG_SELLER.getCustomData());
        String imageUrl = "some-image-url";

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, null, imageUrl))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 0);


        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(PARTICIPANT_RESP_1, PARTICIPANT_RESP_2),
                msgResp,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                null,
                imageUrl,
                UNREAD_MSGS_COUNT,
                Optional.empty());

        ConversationResponse actual = ConversationResponseConverter.toConversationResponse(conversation);
        assertEquals(expected, actual);
    }
}