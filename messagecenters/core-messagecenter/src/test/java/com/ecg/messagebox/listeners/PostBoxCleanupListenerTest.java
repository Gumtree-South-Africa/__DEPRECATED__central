package com.ecg.messagebox.listeners;

import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationClosedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static com.ecg.replyts.core.api.model.conversation.ConversationState.CLOSED;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxCleanupListenerTest {
    private static final String BUYER_USER_ID = "1";
    private static final String SELLER_USER_ID = "2";

    @Mock
    private PostBoxService postBoxServiceMock;

    @Mock
    private UserIdentifierService userIdentifierServiceMock;

    private PostBoxCleanupListener listener;

    private Conversation conversation;

    @Before
    public void setup() {
        listener = new PostBoxCleanupListener(postBoxServiceMock, userIdentifierServiceMock);

        conversation = ImmutableConversation.Builder
          .aConversation()
          .withId("cid")
          .withState(CLOSED)
          .build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation)).thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation)).thenReturn(of(SELLER_USER_ID));
    }

    @Test
    public void conversationDeletedEventIsProcessed() {
        listener.eventsTriggered(conversation, Arrays.asList(new ConversationDeletedEvent(new DateTime())));

        verify(postBoxServiceMock).deleteConversation(BUYER_USER_ID, conversation.getId(), conversation.getAdId());
        verify(postBoxServiceMock).deleteConversation(SELLER_USER_ID, conversation.getId(), conversation.getAdId());
    }

    @Test
    public void conversationClosedEventIsIgnored() {
        listener.eventsTriggered(conversation, Arrays.asList(new ConversationClosedEvent(new ConversationClosedCommand(conversation.getId(), ConversationRole.Buyer, new DateTime()))));

        verify(postBoxServiceMock, never()).deleteConversation(BUYER_USER_ID, conversation.getId(), conversation.getAdId());
        verify(postBoxServiceMock, never()).deleteConversation(SELLER_USER_ID, conversation.getId(), conversation.getAdId());
    }
}
