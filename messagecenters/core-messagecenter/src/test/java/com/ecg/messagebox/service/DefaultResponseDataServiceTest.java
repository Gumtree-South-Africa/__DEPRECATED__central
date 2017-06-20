package com.ecg.messagebox.service;


import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultResponseDataServiceTest {

    private static final String USER_ID_1 = "1";
    private static final String USER_ID_2 = "2";
    private static final String DEFAULT_SUBJECT = "Default subject";
    private static final String AD_ID = "m123";
    private static final String CONVERSATION_ID = "c1";

    @Mock private ResponseDataRepository responseDataRepository;
    @Mock private UserIdentifierService userIdentifierService;

    private ResponseDataService service;

    @Before
    public void setup() {
        service = new DefaultResponseDataService(responseDataRepository, userIdentifierService);
    }

    @Test
    public void shouldInsertResponseDataForFirstBuyerMessage() {
        Message rtsMsg = newMessage("msgid", MessageDirection.BUYER_TO_SELLER, MessageState.SENT, DEFAULT_SUBJECT);

        DateTime creationDateTime = new DateTime();

        Conversation rtsConversation = aConversation().withId(CONVERSATION_ID).withAdId(AD_ID)
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(singletonList(rtsMsg)).withSeller(USER_ID_1, USER_ID_1).build();
        when(userIdentifierService.getSellerUserId(any())).thenReturn(of(USER_ID_1));

        service.calculateResponseData(USER_ID_1, rtsConversation, rtsMsg);

        ResponseData responseData = new ResponseData(USER_ID_1, CONVERSATION_ID, creationDateTime, MessageType.ASQ, -1);
        verify(responseDataRepository).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldUpdateResponseDataForFirstSellerResponseToBuyer() {
        Message firstRtsMessage = newMessage("msgid1", MessageDirection.BUYER_TO_SELLER, MessageState.SENT, DEFAULT_SUBJECT);
        Message secondRtsMessage = newMessage("msgid2", MessageDirection.SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);

        DateTime creationDateTime = new DateTime(2016, 1, 30, 20, 1, 42, DateTimeZone.forID("Europe/Amsterdam"));

        Conversation rtsConversation = aConversation().withId(CONVERSATION_ID).withAdId(AD_ID).withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(Arrays.asList(firstRtsMessage, secondRtsMessage)).withSeller(USER_ID_1, USER_ID_1).build();
        when(userIdentifierService.getSellerUserId(any())).thenReturn(of(USER_ID_1));

        service.calculateResponseData(USER_ID_1, rtsConversation, secondRtsMessage);

        ResponseData responseData = new ResponseData(USER_ID_1, CONVERSATION_ID, creationDateTime, MessageType.ASQ, 10);
        verify(responseDataRepository).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotUpdateResponseDataForSecondSellerResponseToBuyer() {
        Message firstRtsMessage = newMessage("msgid1", MessageDirection.BUYER_TO_SELLER, MessageState.SENT, DEFAULT_SUBJECT);
        Message secondRtsMessage = newMessage("msgid2", MessageDirection.SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);
        Message thirdRtsMessage = newMessage("msgid3", MessageDirection.SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);

        DateTime creationDateTime = new DateTime(2016, 1, 30, 20, 1, 42, DateTimeZone.forID("Europe/Amsterdam"));

        Conversation rtsConversation = aConversation().withId(CONVERSATION_ID).withAdId(AD_ID)
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withSeller(USER_ID_1, USER_ID_1)
                .withMessages(Arrays.asList(firstRtsMessage, secondRtsMessage, thirdRtsMessage)).build();
        when(userIdentifierService.getSellerUserId(any())).thenReturn(of(USER_ID_1));

        service.calculateResponseData(USER_ID_1, rtsConversation, thirdRtsMessage);

        ResponseData responseData = new ResponseData(USER_ID_1, CONVERSATION_ID, creationDateTime, MessageType.ASQ, 10);
        verify(responseDataRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotInsertResponseDataForFirstSellerMessage() {
        Message firstRtsMessage = newMessage("msgid", MessageDirection.SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);

        DateTime creationDateTime = new DateTime();

        Conversation rtsConversation = aConversation().withId(CONVERSATION_ID).withAdId(AD_ID)
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(singletonList(firstRtsMessage)).withSeller(USER_ID_1, USER_ID_1).build();
        when(userIdentifierService.getSellerUserId(any())).thenReturn(of(USER_ID_1));

        service.calculateResponseData(USER_ID_1, rtsConversation, firstRtsMessage);

        ResponseData responseData = new ResponseData(USER_ID_1, CONVERSATION_ID, creationDateTime, MessageType.ASQ, -1);
        verify(responseDataRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotInsertResponseDataForBuyerRole() {
        Message firstRtsMessage = newMessage("msgid", MessageDirection.BUYER_TO_SELLER, MessageState.SENT, DEFAULT_SUBJECT);
        DateTime creationDateTime = new DateTime();

        Conversation rtsConversation = aConversation().withId(CONVERSATION_ID).withAdId(AD_ID)
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(singletonList(firstRtsMessage))
                .withSeller(USER_ID_2, USER_ID_2).withBuyer(USER_ID_1, USER_ID_1).build();
        when(userIdentifierService.getSellerUserId(any())).thenReturn(of(USER_ID_2));

        service.calculateResponseData(USER_ID_1, rtsConversation, firstRtsMessage);

        ResponseData responseData = new ResponseData(USER_ID_1, CONVERSATION_ID, creationDateTime, MessageType.ASQ, -1);
        verify(responseDataRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldCallRepositoryToGetResponseData() {
        service.getResponseData(USER_ID_1);

        verify(responseDataRepository).getResponseData(USER_ID_1);
    }

    @Test
    public void aggregatedResponseDataShouldGetListFromRepoAndCalculateAggregatedValue() {
        Optional<AggregatedResponseData> responseData = service.getAggregatedResponseData(USER_ID_1);
        verify(responseDataRepository).getResponseData(USER_ID_1);

        assertEquals(Optional.empty(), responseData);
    }

    private Message newMessage(String id, MessageDirection direction, MessageState state, String subject) {
        return aMessage()
                .withId(id)
                .withEventTimeUUID(UUID.randomUUID())
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withTextParts(singletonList("text 123"))
                .withHeader("Subject", subject)
                .build();
    }
}
