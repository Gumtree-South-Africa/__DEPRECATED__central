package com.ecg.messagecenter.persistence.cassandra;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.MessageType;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class CassandraPostBoxServiceTest {

    private DefaultCassandraPostBoxRepository postBoxRepository = mock(DefaultCassandraPostBoxRepository.class);
    private ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private UserIdentifierService userIdentifierService = mock(UserIdentifierService.class);

    @Test
    public void testGetUnreadCounters() throws Exception {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        PostBoxUnreadCounts postBoxUnreadCounts = new PostBoxUnreadCounts(2, 3);
        when(postBoxRepository.getUnreadCounts("p123")).thenReturn(postBoxUnreadCounts);

        PostBoxUnreadCounts actualUnreadCounters = service.getUnreadCounts("p123");
        assertSame(postBoxUnreadCounts, actualUnreadCounters);
    }

    @Test
    public void shouldInsertResponseDataForFirstBuyerMessage() {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        ImmutableMessage.Builder messageBuilder = defaultMessage("msgid", MessageDirection.BUYER_TO_SELLER);

        DateTime creationDateTime = new DateTime();

        Conversation conversation = aConversation().withId("id").withAdId("adId").withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime()).withState(ConversationState.ACTIVE).withMessage(messageBuilder).build();

        service.processNewMessage("userId", conversation, messageBuilder.build(), ConversationRole.Seller, false, Optional.absent());

        ResponseData responseData = new ResponseData("userId", "id", creationDateTime, MessageType.ASQ, -1);
        verify(postBoxRepository).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldUpdateResponseDataForFirstSellerResponseToBuyer() {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        ImmutableMessage.Builder firstMessageBuilder = defaultMessage("msgid1", MessageDirection.BUYER_TO_SELLER);
        ImmutableMessage.Builder secondMessageBuilder = defaultMessage("msgid2", MessageDirection.SELLER_TO_BUYER);

        DateTime creationDateTime = new DateTime(2016, 1, 30, 20, 1, 42, DateTimeZone.forID("Europe/Amsterdam"));

        Conversation conversation = aConversation().withId("id").withAdId("adId").withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(Arrays.asList(firstMessageBuilder.build(), secondMessageBuilder.build())).build();

        service.processNewMessage("userId", conversation, secondMessageBuilder.build(), ConversationRole.Seller, false, Optional.absent());

        ResponseData responseData = new ResponseData("userId", "id", creationDateTime, MessageType.ASQ, 10);
        verify(postBoxRepository).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotUpdateResponseDataForSecondSellerResponseToBuyer() {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        ImmutableMessage.Builder firstMessageBuilder = defaultMessage("msgid1", MessageDirection.BUYER_TO_SELLER);
        ImmutableMessage.Builder secondMessageBuilder = defaultMessage("msgid2", MessageDirection.SELLER_TO_BUYER);
        ImmutableMessage.Builder thirdMessageBuilder = defaultMessage("msgid3", MessageDirection.SELLER_TO_BUYER);

        DateTime creationDateTime = new DateTime(2016, 1, 30, 20, 1, 42, DateTimeZone.forID("Europe/Amsterdam"));

        Conversation conversation = aConversation().withId("id").withAdId("adId").withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE).withMessages(Arrays.asList(firstMessageBuilder.build(), secondMessageBuilder.build(), thirdMessageBuilder.build())).build();

        service.processNewMessage("userId", conversation, thirdMessageBuilder.build(), ConversationRole.Seller, false, Optional.absent());

        ResponseData responseData = new ResponseData("userId", "id", creationDateTime, MessageType.ASQ, 10);
        verify(postBoxRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotInsertResponseDataForFirstSellerMessage() {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        ImmutableMessage.Builder messageBuilder = defaultMessage("msgid", MessageDirection.SELLER_TO_BUYER);

        DateTime creationDateTime = new DateTime();

        Conversation conversation = aConversation().withId("id").withAdId("adId").withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime()).withState(ConversationState.ACTIVE).withMessage(messageBuilder).build();

        service.processNewMessage("userId", conversation, messageBuilder.build(), ConversationRole.Seller, false, Optional.absent());

        ResponseData responseData = new ResponseData("userId", "id", creationDateTime, MessageType.ASQ, -1);
        verify(postBoxRepository, never()).addOrUpdateResponseDataAsync(responseData);
    }

    @Test
    public void shouldNotInsertResponseDataForBuyerRole() {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        ImmutableMessage.Builder messageBuilder = defaultMessage("msgid", MessageDirection.BUYER_TO_SELLER);

        DateTime creationDateTime = new DateTime();

        Conversation conversation = aConversation().withId("id").withAdId("adId").withCreatedAt(creationDateTime).withLastModifiedAt(new DateTime()).withState(ConversationState.ACTIVE).withMessage(messageBuilder).build();

        service.processNewMessage("userId", conversation, messageBuilder.build(), ConversationRole.Buyer, false, Optional.absent());

        ResponseData responseData = new ResponseData("userId", "id", creationDateTime, MessageType.ASQ, -1);
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