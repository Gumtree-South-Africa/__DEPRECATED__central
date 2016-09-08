package com.ecg.messagebox.service;

import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.persistence.cassandra.DefaultCassandraPostBoxRepository;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CassandraPostBoxServiceTest {

    private static final String USER_ID = "1";

    private final CassandraPostBoxRepository conversationsRepoMock = mock(DefaultCassandraPostBoxRepository.class);
    private final UserIdentifierService userIdentifierServiceMock = mock(UserIdentifierService.class);

    private CassandraPostBoxService service = new CassandraPostBoxService(conversationsRepoMock, userIdentifierServiceMock);

    @Test
    public void processNewMessage() throws Exception {
        Message rtsMessage = newMessage("msgid", MessageDirection.BUYER_TO_SELLER, MessageState.HELD)
                .build();
        Conversation rtsConversation = aConversation()
                .withId("c1")
                .withAdId("m123")
                .withCreatedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withMessages(singletonList(rtsMessage))
                .build();

        service.processNewMessage(USER_ID, rtsConversation, rtsMessage, true);

        verifyZeroInteractions(conversationsRepoMock, userIdentifierServiceMock);
    }

    private static ImmutableMessage.Builder newMessage(String id, MessageDirection direction, MessageState state) {
        return aMessage()
                .withId(id)
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withTextParts(singletonList("text 123"));
    }
}