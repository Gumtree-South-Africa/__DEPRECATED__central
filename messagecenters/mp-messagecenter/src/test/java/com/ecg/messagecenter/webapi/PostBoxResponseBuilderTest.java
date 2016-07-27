package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxResponseBuilderTest {

    private static final long BUYER_ID = 111L;
    private static final long SELLER_ID = 222L;
    private static final String USER_ID = "test@email.com";
    private static final DateTime RECEIVED_DATE = new DateTime(2015, 3, 26, 12, 0, 0, 0);
    private static final String LAST_MESSAGE = "Last message";

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private UserIdentifierService userIdentifierService;
    @Mock
    private PostBox postBox;

    @InjectMocks
    private PostBoxResponseBuilder testObject = new PostBoxResponseBuilder(conversationRepository, userIdentifierService);

    @Before
    public void before() {
        when(postBox.getNumUnreadMessages()).thenReturn(10);
    }

    @Test
    public void buildsPostBoxResponse() {
        ConversationThread conversationThread = mock(ConversationThread.class);
        when(conversationThread.containsNewListAggregateData()).thenReturn(true);
        when(conversationThread.getBuyerName()).thenReturn(Optional.empty());
        when(conversationThread.getSellerName()).thenReturn(Optional.empty());
        when(conversationThread.getUserIdBuyer()).thenReturn(Optional.of(BUYER_ID));
        when(conversationThread.getUserIdSeller()).thenReturn(Optional.of(SELLER_ID));
        when(conversationThread.getMessageDirection()).thenReturn(Optional.of(MessageDirection.BUYER_TO_SELLER.name()));
        when(conversationThread.getReceivedAt()).thenReturn(RECEIVED_DATE);
        when(conversationThread.getLastMessageCreatedAt()).thenReturn(Optional.empty());
        when(conversationThread.getPreviewLastMessage()).thenReturn(Optional.of(LAST_MESSAGE));
        when(userIdentifierService.getRoleFromConversation(USER_ID, conversationThread)).thenReturn(ConversationRole.Buyer);

        ConversationThread conversationThreadWithoutData = mock(ConversationThread.class);
        when(conversationThreadWithoutData.containsNewListAggregateData()).thenReturn(false);
        when(conversationThreadWithoutData.getBuyerName()).thenReturn(Optional.empty());
        when(conversationThreadWithoutData.getSellerName()).thenReturn(Optional.empty());
        when(conversationThreadWithoutData.getUserIdBuyer()).thenReturn(Optional.of(BUYER_ID));
        when(conversationThreadWithoutData.getUserIdSeller()).thenReturn((Optional.of(SELLER_ID)));
        when(conversationThreadWithoutData.getMessageDirection()).thenReturn(Optional.of(MessageDirection.BUYER_TO_SELLER.name()));
        when(conversationThreadWithoutData.getReceivedAt()).thenReturn(RECEIVED_DATE);
        when(conversationThreadWithoutData.getLastMessageCreatedAt()).thenReturn(Optional.empty());
        when(conversationThreadWithoutData.getPreviewLastMessage()).thenReturn(Optional.of(LAST_MESSAGE));
        when(userIdentifierService.getRoleFromConversation(USER_ID, conversationThreadWithoutData)).thenReturn(ConversationRole.Seller);
        List<ConversationThread> conversationThreadList = Arrays.asList(conversationThread, conversationThreadWithoutData);
        when(postBox.getConversationThreadsCapTo(3, 5)).thenReturn(conversationThreadList);

        PostBoxResponse postBoxResponse = testObject.buildPostBoxResponse(USER_ID, 5, 3, postBox);
        assertEquals(1, postBoxResponse.get_meta().getNumFound());
        assertEquals(3, postBoxResponse.get_meta().getPageNum());
        assertEquals(5, postBoxResponse.get_meta().getPageSize());
        assertEquals(1, postBoxResponse.getConversations().size());
        assertEquals(USER_ID, postBoxResponse.getConversations().get(0).getUserId());
        assertEquals(RECEIVED_DATE.toString(), postBoxResponse.getConversations().get(0).getReceivedDate());
    }
}
