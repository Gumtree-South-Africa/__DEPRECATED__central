package com.ecg.messagebox.service;

import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.persistence.cassandra.DefaultCassandraPostBoxRepository;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

public class CassandraPostBoxServiceTest {

    private static final String USER_ID_1 = "1";
    private static final String USER_ID_2 = "2";

    private static final String BUYER_USER_ID_NAME = "user-id-buyer";
    private static final String SELLER_USER_ID_NAME = "user-id-seller";

    private final CassandraPostBoxRepository conversationsRepoMock = mock(DefaultCassandraPostBoxRepository.class);
    private final UserIdentifierService userIdentifierServiceMock = mock(UserIdentifierService.class);

    private CassandraPostBoxService service = new CassandraPostBoxService(conversationsRepoMock, userIdentifierServiceMock);

    @Test
    public void processNewMessageWithStateIgnored() {
        when(userIdentifierServiceMock.getBuyerUserIdName()).thenReturn(BUYER_USER_ID_NAME);

        Message rtsMsg = newMessage("1", MessageDirection.BUYER_TO_SELLER, MessageState.IGNORED);
        service.processNewMessage(USER_ID_1, newConversation("c1"), rtsMsg, true);

        verify(userIdentifierServiceMock).getBuyerUserIdName();
        verifyZeroInteractions(conversationsRepoMock);
    }

    @Test
    public void processNewMessageWithStateHeld() {
        when(userIdentifierServiceMock.getSellerUserIdName()).thenReturn(SELLER_USER_ID_NAME);

        Message rtsMsg = newMessage("1", MessageDirection.SELLER_TO_BUYER, MessageState.HELD);
        service.processNewMessage(USER_ID_1, newConversation("c1"), rtsMsg, true);

        verify(userIdentifierServiceMock).getSellerUserIdName();
        verifyZeroInteractions(conversationsRepoMock);
    }

    private Conversation newConversation(String id) {
        return aConversation()
                .withId(id)
                .withAdId("m123")
                .withCreatedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withCustomValues(ImmutableMap.of(BUYER_USER_ID_NAME, USER_ID_1, SELLER_USER_ID_NAME, USER_ID_2))
                .build();
    }

    private Message newMessage(String id, MessageDirection direction, MessageState state) {
        return aMessage()
                .withId(id)
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withTextParts(singletonList("text 123"))
                .build();
    }
}