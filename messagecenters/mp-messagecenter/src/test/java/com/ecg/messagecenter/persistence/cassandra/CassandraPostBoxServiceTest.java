package com.ecg.messagecenter.persistence.cassandra;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.MessageType;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPostBoxServiceTest {

    private static final String USER_ID = "123";

    private DefaultCassandraPostBoxRepository postBoxRepository = mock(DefaultCassandraPostBoxRepository.class);
    private ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private UserIdentifierService userIdentifierService = mock(UserIdentifierService.class);

    private CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

    @Test
    public void testGetUnreadCounters() throws Exception {
        PostBoxUnreadCounts postBoxUnreadCounts = new PostBoxUnreadCounts(USER_ID, 2, 3);
        when(postBoxRepository.getUnreadCounts(USER_ID)).thenReturn(postBoxUnreadCounts);

        PostBoxUnreadCounts actualUnreadCounters = service.getUnreadCounts(USER_ID);
        assertSame(postBoxUnreadCounts, actualUnreadCounters);
    }

    @Test
    public void shouldInsertResponseDataForFirstBuyerMessage() {
        ImmutableMessage.Builder messageBuilder = defaultMessage("msgid", MessageDirection.BUYER_TO_SELLER);

        DateTime creationDateTime = new DateTime();

        Conversation conversation = aConversation().withId("id").withAdId("adId")
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessage(messageBuilder).build();

        service.processNewMessage(USER_ID, conversation, messageBuilder.build(), ConversationRole.Seller, false);

        ResponseData responseData = new ResponseData(USER_ID, "id", creationDateTime, MessageType.ASQ, -1);
        verify(postBoxRepository).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldUpdateResponseDataForFirstSellerResponseToBuyer() {
        ImmutableMessage.Builder firstMessageBuilder = defaultMessage("msgid1", MessageDirection.BUYER_TO_SELLER);
        ImmutableMessage.Builder secondMessageBuilder = defaultMessage("msgid2", MessageDirection.SELLER_TO_BUYER);

        DateTime creationDateTime = new DateTime(2016, 1, 30, 20, 1, 42, DateTimeZone.forID("Europe/Amsterdam"));

        Conversation conversation = aConversation().withId("id").withAdId("adId").withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(Arrays.asList(firstMessageBuilder.build(), secondMessageBuilder.build())).build();

        service.processNewMessage(USER_ID, conversation, secondMessageBuilder.build(), ConversationRole.Seller, false);

        ResponseData responseData = new ResponseData(USER_ID, "id", creationDateTime, MessageType.ASQ, 10);
        verify(postBoxRepository).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotUpdateResponseDataForSecondSellerResponseToBuyer() {
        ImmutableMessage.Builder firstMessageBuilder = defaultMessage("msgid1", MessageDirection.BUYER_TO_SELLER);
        ImmutableMessage.Builder secondMessageBuilder = defaultMessage("msgid2", MessageDirection.SELLER_TO_BUYER);
        ImmutableMessage.Builder thirdMessageBuilder = defaultMessage("msgid3", MessageDirection.SELLER_TO_BUYER);

        DateTime creationDateTime = new DateTime(2016, 1, 30, 20, 1, 42, DateTimeZone.forID("Europe/Amsterdam"));

        Conversation conversation = aConversation().withId("id").withAdId("adId")
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withMessages(Arrays.asList(firstMessageBuilder.build(), secondMessageBuilder.build(), thirdMessageBuilder.build())).build();

        service.processNewMessage(USER_ID, conversation, thirdMessageBuilder.build(), ConversationRole.Seller, false);

        ResponseData responseData = new ResponseData(USER_ID, "id", creationDateTime, MessageType.ASQ, 10);
        verify(postBoxRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotInsertResponseDataForFirstSellerMessage() {
        ImmutableMessage.Builder messageBuilder = defaultMessage("msgid", MessageDirection.SELLER_TO_BUYER);

        DateTime creationDateTime = new DateTime();

        Conversation conversation = aConversation().withId("id").withAdId("adId")
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessage(messageBuilder).build();

        service.processNewMessage(USER_ID, conversation, messageBuilder.build(), ConversationRole.Seller, false);

        ResponseData responseData = new ResponseData(USER_ID, "id", creationDateTime, MessageType.ASQ, -1);
        verify(postBoxRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotInsertResponseDataForBuyerRole() {
        ImmutableMessage.Builder messageBuilder = defaultMessage("msgid", MessageDirection.BUYER_TO_SELLER);

        DateTime creationDateTime = new DateTime();

        Conversation conversation = aConversation().withId("id").withAdId("adId")
                .withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessage(messageBuilder).build();

        service.processNewMessage(USER_ID, conversation, messageBuilder.build(), ConversationRole.Buyer, false);

        ResponseData responseData = new ResponseData(USER_ID, "id", creationDateTime, MessageType.ASQ, -1);
        verify(postBoxRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    private static ImmutableMessage.Builder defaultMessage(String id, MessageDirection messageDirection) {
        return aMessage()
                .withId(id)
                .withMessageDirection(messageDirection)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withTextParts(Collections.singletonList(""));
    }
}
