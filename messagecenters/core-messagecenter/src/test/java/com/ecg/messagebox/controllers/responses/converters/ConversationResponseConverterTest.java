package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.*;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConversationResponseConverterTest {

    private static final String CONVERSATION_ID = "c1";
    private static final String AD_ID = "m1234";

    private static final int UNREAD_MSGS_COUNT = 5;

    private static final String EMAIL_SUBJECT = "email subject";
    private static final String CONVERSATION_TITLE = "conversation title";

    private static final Participant PARTICIPANT_1 = new Participant("buyerId", "buyerName", "buyer@email.com", ParticipantRole.BUYER);
    private static final Participant PARTICIPANT_2 = new Participant("sellerId", "sellerName", "seller@email.com", ParticipantRole.SELLER);

    private static final Message MSG_BUYER = new Message(timeBased(), MessageType.ASQ, new MessageMetadata("text buyer", "buyerId"));
    private static final Message MSG_SELLER = new Message(timeBased(), MessageType.CHAT, new MessageMetadata("text seller", "sellerId"));

    @Mock
    private MessageResponse messageRespMock;
    @Mock
    private ParticipantResponse participantRespMock;
    @Mock
    private ParticipantResponseConverter participantRespConverterMock;
    @Mock
    private MessageResponseConverter msgRespConverterMock;

    private ConversationResponseConverter convRespConverter;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        convRespConverter = new ConversationResponseConverter(participantRespConverterMock, msgRespConverterMock);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void toConversationWithMessagesResponse() {
        MessageResponse msgResp1 = new MessageResponse("m1", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data");
        MessageResponse msgResp2 = new MessageResponse("m2", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data");

        MessageResponse expectedMsgResp1 = new MessageResponse("m1", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data");
        MessageResponse expectedMsgResp2 = new MessageResponse("m2", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data");
        expectedMsgResp2.setIsRead(false);

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, CONVERSATION_TITLE))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 1);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(participantRespMock, participantRespMock),
                expectedMsgResp2,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                CONVERSATION_TITLE,
                UNREAD_MSGS_COUNT,
                Optional.of(asList(expectedMsgResp1, expectedMsgResp2)));


        when(msgRespConverterMock.toMessageResponse(MSG_BUYER)).thenReturn(msgResp1);
        when(msgRespConverterMock.toMessageResponse(MSG_SELLER)).thenReturn(msgResp2);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_1)).thenReturn(participantRespMock);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_2)).thenReturn(participantRespMock);

        ConversationResponse actual = convRespConverter.toConversationResponseWithMessages(conversation);

        verify(msgRespConverterMock).toMessageResponse(MSG_BUYER);
        verify(msgRespConverterMock, times(2)).toMessageResponse(MSG_SELLER);
        verify(participantRespConverterMock).toParticipantResponse(PARTICIPANT_1);
        verify(participantRespConverterMock).toParticipantResponse(PARTICIPANT_2);
        assertThat(actual, is(expected));
    }

    @Test
    public void toConversationResponse() {
        MessageResponse msgResp = new MessageResponse("m1", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data");
        MessageResponse expectedMsgResp = new MessageResponse("m1", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data").withIsRead(true);

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, CONVERSATION_TITLE))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 0);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(participantRespMock, participantRespMock),
                expectedMsgResp,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                CONVERSATION_TITLE,
                UNREAD_MSGS_COUNT,
                Optional.empty());

        when(msgRespConverterMock.toMessageResponse(MSG_SELLER)).thenReturn(msgResp);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_1)).thenReturn(participantRespMock);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_2)).thenReturn(participantRespMock);

        ConversationResponse actual = convRespConverter.toConversationResponse(conversation);

        verify(participantRespConverterMock).toParticipantResponse(PARTICIPANT_1);
        verify(participantRespConverterMock).toParticipantResponse(PARTICIPANT_2);
        verify(msgRespConverterMock).toMessageResponse(MSG_SELLER);
        assertThat(actual, is(expected));
    }

    @Test
    public void toConversationResponseWithoutTitle() {

        MessageResponse msgResp = new MessageResponse("m1", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data");

        MessageResponse expectedMsgResp = new MessageResponse("m1", "message", "text", PARTICIPANT_1.getUserId(), "1/1/1", "data").withIsRead(true);

        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                PARTICIPANT_1.getUserId(),
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(now(), EMAIL_SUBJECT, null))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(PARTICIPANT_1.getUserId(), UNREAD_MSGS_COUNT)
                .addNumUnreadMessages(PARTICIPANT_2.getUserId(), 0);


        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(participantRespMock, participantRespMock),
                expectedMsgResp,
                toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT,
                null,
                UNREAD_MSGS_COUNT,
                Optional.empty());

        when(msgRespConverterMock.toMessageResponse(MSG_SELLER)).thenReturn(msgResp);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_1)).thenReturn(participantRespMock);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_2)).thenReturn(participantRespMock);

        ConversationResponse actual = convRespConverter.toConversationResponse(conversation);

        assertThat(actual, is(expected));
    }
}