package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.*;
import org.junit.Test;

import java.util.Optional;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class ConversationResponseConverterTest {

    private static final String CONVERSATION_ID = "c1";
    private static final String AD_ID = "m1234";

    private static final int UNREAD_MSGS_COUNT = 5;

    private static final String EMAIL_SUBJECT = "email subject";

    private static final Participant PARTICIPANT_1 = new Participant("buyerId", "buyerName", "buyer@email.com", ParticipantRole.BUYER);
    private static final Participant PARTICIPANT_2 = new Participant("sellerId", "sellerName", "seller@email.com", ParticipantRole.SELLER);

    private static final Message MSG_BUYER = new Message(timeBased(), MessageType.ASQ, new MessageMetadata("text buyer", "buyerId"));
    private static final Message MSG_SELLER = new Message(timeBased(), MessageType.CHAT, new MessageMetadata("text seller", "sellerId"));

    private final MessageResponse messageRespMock = mock(MessageResponse.class);
    private final ParticipantResponse participantRespMock = mock(ParticipantResponse.class);
    private final ParticipantResponseConverter participantRespConverterMock = mock(ParticipantResponseConverter.class);
    private final MessageResponseConverter msgRespConverterMock = mock(MessageResponseConverter.class);

    private final ConversationResponseConverter convRespConverter = new ConversationResponseConverter(participantRespConverterMock, msgRespConverterMock);

    @Test
    public void toConversationWithMessagesResponse() {
        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(EMAIL_SUBJECT))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(UNREAD_MSGS_COUNT);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(participantRespMock, participantRespMock),
                messageRespMock,
                EMAIL_SUBJECT,
                UNREAD_MSGS_COUNT,
                Optional.of(asList(messageRespMock, messageRespMock)));

        when(msgRespConverterMock.toMessageResponse(MSG_BUYER)).thenReturn(messageRespMock);
        when(msgRespConverterMock.toMessageResponse(MSG_SELLER)).thenReturn(messageRespMock);
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
        ConversationThread conversation = new ConversationThread(
                CONVERSATION_ID,
                AD_ID,
                ACTIVE,
                RECEIVE,
                asList(PARTICIPANT_1, PARTICIPANT_2),
                MSG_SELLER,
                new ConversationMetadata(EMAIL_SUBJECT))
                .addMessages(asList(MSG_BUYER, MSG_SELLER))
                .addNumUnreadMessages(UNREAD_MSGS_COUNT);

        ConversationResponse expected = new ConversationResponse(
                CONVERSATION_ID,
                AD_ID,
                "active",
                "receive",
                asList(participantRespMock, participantRespMock),
                messageRespMock,
                EMAIL_SUBJECT,
                UNREAD_MSGS_COUNT,
                Optional.empty());

        when(msgRespConverterMock.toMessageResponse(MSG_SELLER)).thenReturn(messageRespMock);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_1)).thenReturn(participantRespMock);
        when(participantRespConverterMock.toParticipantResponse(PARTICIPANT_2)).thenReturn(participantRespMock);

        ConversationResponse actual = convRespConverter.toConversationResponse(conversation);

        verify(participantRespConverterMock).toParticipantResponse(PARTICIPANT_1);
        verify(participantRespConverterMock).toParticipantResponse(PARTICIPANT_2);
        verify(msgRespConverterMock).toMessageResponse(MSG_SELLER);
        assertThat(actual, is(expected));
    }
}