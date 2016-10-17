package com.ecg.messagecenter.listeners;

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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxCleanupListenerTest {

    @Mock
    private PostBoxService postBoxServiceMock;
    @Mock
    private UserIdentifierService userIdentifierServiceMock;

    private ImmutableConversation.Builder convBuilder;

    private PostBoxCleanupListener listener;

    private static final String BUYER_USER_ID = "1";
    private static final String SELLER_USER_ID = "2";

    @Before
    public void setup() {
        listener = new PostBoxCleanupListener(postBoxServiceMock, userIdentifierServiceMock);

        convBuilder = ImmutableConversation.Builder
                .aConversation()
                .withId("cid")
                .withState(CLOSED);

        when(userIdentifierServiceMock.getBuyerUserId(any())).thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(any())).thenReturn(of(SELLER_USER_ID));
    }

    @Test
    public void conversationDeletedEventIsProcessed() {
        Conversation conversation = convBuilder.build();

        listener.eventsTriggered(conversation, Arrays.asList(new ConversationDeletedEvent(new DateTime())));

        verify(postBoxServiceMock).deleteConversation(BUYER_USER_ID, conversation.getId(), conversation.getAdId());
        verify(postBoxServiceMock).deleteConversation(SELLER_USER_ID, conversation.getId(), conversation.getAdId());
    }

    @Test
    public void conversationClosedEventIsIgnored() {
        Conversation conversation = convBuilder.build();

        listener.eventsTriggered(conversation, Arrays.asList(new ConversationClosedEvent(new ConversationClosedCommand(conversation.getId(), ConversationRole.Buyer, new DateTime()))));

        verify(postBoxServiceMock, never()).deleteConversation(BUYER_USER_ID, conversation.getId(), conversation.getAdId());
        verify(postBoxServiceMock, never()).deleteConversation(SELLER_USER_ID, conversation.getId(), conversation.getAdId());
    }

}
