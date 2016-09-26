package com.ecg.messagebox.diff;

import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiffToolTest {

    private static final String USER_ID = "123";

    @Mock
    private PostBoxResponseDiff postBoxResponseDiff;
    @Mock
    private ConversationResponseDiff conversationResponseDiff;
    @Mock
    private UnreadCountsDiff unreadCountsDiff;

    @Mock
    private CompletableFuture<PostBoxResponse> oldPostBoxResponseFuture;
    @Mock
    private CompletableFuture<PostBoxResponse> newPostBoxResponseFuture;
    @Mock
    private CompletableFuture<Optional<ConversationResponse>> oldConversationResponseFuture;
    @Mock
    private CompletableFuture<Optional<ConversationResponse>> newConversationResponseFuture;
    @Mock
    private CompletableFuture<PostBoxUnreadCounts> oldUnreadCountsFuture;
    @Mock
    private CompletableFuture<PostBoxUnreadCounts> newUnreadCountsFuture;

    private DiffTool diffTool;

    @Before
    public void setup() {
        diffTool = new DiffTool(postBoxResponseDiff, conversationResponseDiff, unreadCountsDiff);
    }

    @Test
    public void postBoxResponseDiff() {
        PostBoxResponse oldResponse = new PostBoxResponse();
        PostBoxResponse newResponse = new PostBoxResponse();

        when(newPostBoxResponseFuture.join()).thenReturn(newResponse);
        when(oldPostBoxResponseFuture.join()).thenReturn(oldResponse);

        diffTool.postBoxResponseDiff(USER_ID, newPostBoxResponseFuture, oldPostBoxResponseFuture);

        verify(postBoxResponseDiff).diff(USER_ID, newResponse, oldResponse);
    }

    @Test
    public void conversationResponseDiff() {
        String conversationId = "cid";
        ConversationResponse oldResponse = new ConversationResponse(conversationId, ConversationRole.Buyer, "buyerEmail", "sellerEmail",
                "buyerName", "sellerName", 1L, 2L, "m123", "creationDate", "emailSubject", emptyList(), 1);
        ConversationResponse newResponse = new ConversationResponse(conversationId, ConversationRole.Buyer, "buyerEmail", "sellerEmail",
                "buyerName", "sellerName", 1L, 2L, "m123", "creationDate2", "emailSubject", emptyList(), 1);

        when(newConversationResponseFuture.join()).thenReturn(Optional.of(newResponse));
        when(oldConversationResponseFuture.join()).thenReturn(Optional.of(oldResponse));

        diffTool.conversationResponseDiff(USER_ID, conversationId, newConversationResponseFuture, oldConversationResponseFuture);

        verify(conversationResponseDiff).diff(USER_ID, conversationId, Optional.of(newResponse), Optional.of(oldResponse));
    }

    @Test
    public void postBoxUnreadCountsDiff() {
        PostBoxUnreadCounts oldResponse = new PostBoxUnreadCounts(USER_ID, 1, 3);
        PostBoxUnreadCounts newResponse = new PostBoxUnreadCounts(USER_ID, 1, 5);

        when(newUnreadCountsFuture.join()).thenReturn(newResponse);
        when(oldUnreadCountsFuture.join()).thenReturn(oldResponse);

        diffTool.postBoxUnreadCountsDiff(USER_ID, newUnreadCountsFuture, oldUnreadCountsFuture);

        verify(unreadCountsDiff).diff(USER_ID, newResponse, oldResponse);
    }
}