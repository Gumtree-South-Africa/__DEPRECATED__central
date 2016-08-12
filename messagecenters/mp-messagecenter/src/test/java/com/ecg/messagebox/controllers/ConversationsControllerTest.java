package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.converters.ConversationsResponseConverter;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.UserUnreadCounts;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class ConversationsControllerTest {

    private PostBoxService postBoxService = mock(PostBoxService.class);
    private ConversationsResponseConverter conversationsResponseConverter = mock(ConversationsResponseConverter.class);

    private ConversationsController conversationsController = new ConversationsController(postBoxService, conversationsResponseConverter);

    private String userId = "userId";
    private String blockedUserId = "blockedUserId";
    private String blockUserAction = "block-user";
    private String unblockUserAction = "unblock-user";
    private String visibility = "archived";
    private int offset = 0;
    private int limit = 10;
    private PostBox postBox = new PostBox(userId, Collections.emptyList(), new UserUnreadCounts(userId, 0, 0), 34);
    private String[] conversationIds = {};

    @Test
    public void blockUser() throws Exception {
        when(postBoxService.blockUser(userId, blockedUserId, Visibility.ARCHIVED, offset, limit)).thenReturn(postBox);

        conversationsController.executeActions(userId, blockUserAction, visibility, blockedUserId, conversationIds, offset, limit);

        verify(postBoxService).blockUser(userId, blockedUserId, Visibility.ARCHIVED, offset, limit);
        verify(conversationsResponseConverter).toConversationsResponse(postBox, offset, limit);
    }

    @Test
    public void unblockUser() throws Exception {
        when(postBoxService.unblockUser(userId, blockedUserId, Visibility.ARCHIVED, offset, limit)).thenReturn(postBox);

        conversationsController.executeActions(userId, unblockUserAction, visibility, blockedUserId, conversationIds, offset, limit);

        verify(postBoxService).unblockUser(userId, blockedUserId, Visibility.ARCHIVED, offset, limit);
        verify(conversationsResponseConverter).toConversationsResponse(postBox, offset, limit);
    }
}